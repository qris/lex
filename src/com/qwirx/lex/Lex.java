/*
 * Created on 18-Jul-2004
 * Heavily based on the TestEmdros class distributed with
 * Emdros 1.2.0-pre73
 */
package com.qwirx.lex;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.crosswire.jsword.book.sword.SwordBookPath;
import org.xml.sax.SAXException;

import com.mysql.jdbc.Driver;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.DbColumn;
import com.qwirx.db.sql.DbTable;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.jemdros.Preloader;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.lexicon.Lexicon;
import com.qwirx.lex.ontology.OntologyDb;
import com.qwirx.lex.parser.Parser;
import com.qwirx.lex.translit.DatabaseTransliterator;

/**
 * @author chris
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Lex 
{
    private static final Logger m_LOG = Logger.getLogger(Lex.class);

    public static final SqlDatabase getSqlDatabase(String user) 
    throws Exception 
    {
        return getSqlDatabase(user, "lex");
    }

	public static final SqlDatabase getSqlDatabase(String user, String database) 
	throws Exception 
    {
		loadLibrary();
		return new SqlDatabase(getSqlConnection(), user, database);
	}

	public static final OntologyDb getOntologyDb() 
	throws SAXException, IOException 
    {
		return OntologyDb.getInstance();
	}

    static class EmdrosDatabasePool
    {
        private Stack<EmdrosDatabase> m_Pool = new Stack<EmdrosDatabase>();
        private String m_User, m_Host; 
        
        public EmdrosDatabasePool(String user, String host)
        {
            m_User = user;
            m_Host = host;
        }
        
        public synchronized EmdrosDatabase get()
        throws Exception
        {
            if (m_Pool.size() == 0)
            {
                return create();
            }
            
            EmdrosDatabase db = m_Pool.pop();
            
            if (!db.isAlive())
            {
                db.delete();
                return create();
            }
            
            return db;
        }
        
        public synchronized void put(EmdrosDatabase db)
        {
            if (m_Pool.size() > 0 || !db.isAlive())
            {
                db.delete();
            }
            else
            {
                m_Pool.push(db);
            }
        }
        
        public EmdrosDatabase create()
        throws Exception
        {
            loadLibrary();

            EmdrosDatabase emdrosDb = new EmdrosDatabase("localhost", 
                "wihebrew", "emdf", "changeme", m_User, m_Host,
                getLogDatabaseHandle());
            
            emdrosDb.createObjectTypeIfMissing("note");
            emdrosDb.createFeatureIfMissing("note",  "text",             "string");
            emdrosDb.createFeatureIfMissing("verse", "bart_gloss",       "string");
            emdrosDb.createFeatureIfMissing("clause","logical_struct_id","integer");
            emdrosDb.createFeatureIfMissing("clause","logical_structure","string");
            emdrosDb.createFeatureIfMissing("clause","published",        "integer default 0");
            emdrosDb.createFeatureIfMissing("clause","predicate",        "string");
            emdrosDb.createFeatureIfMissing("phrase","argument_name",    "string");
            emdrosDb.createFeatureIfMissing("phrase","type_id",          "integer");
            emdrosDb.createFeatureIfMissing("phrase","macrorole_number", "integer default -1");
            emdrosDb.createFeatureIfMissing("word",  "wordnet_gloss",    "string");
            emdrosDb.createFeatureIfMissing("word",  "wordnet_synset",   "integer");
            
            return emdrosDb;
        }
        
        public void clear()
        {
            for (Iterator<EmdrosDatabase> i = m_Pool.iterator(); i.hasNext();)
            {
                EmdrosDatabase db = i.next();
                db.delete();
                i.remove();
            }
        }
    }
    
    private static Map<String, EmdrosDatabasePool> m_EmdrosPools = 
        new Hashtable<String, EmdrosDatabasePool>();
    private static Map<EmdrosDatabase, EmdrosDatabasePool> m_Ledger = 
        new Hashtable<EmdrosDatabase, EmdrosDatabasePool>();

	public static final EmdrosDatabase getEmdrosDatabase(String user, 
        String host, SqlDatabase logDatabase) 
	throws Exception 
    {
        String key = user + "@" + host;
        
        EmdrosDatabasePool pool = m_EmdrosPools.get(key);
        
        if (pool == null)
        {
            pool = new EmdrosDatabasePool(user, host);
            m_EmdrosPools.put(key, pool);
        }
        
        EmdrosDatabase db = pool.get();
        m_Ledger.put(db, pool);
        db.setLogConnection(logDatabase.getConnection());
        return db;
	}
    
    public static final void putEmdrosDatabase(EmdrosDatabase db)
    {
        EmdrosDatabasePool pool = m_Ledger.get(db);
        if (pool == null)
        {
            db.delete();
            return;
        }
        
        pool.put(db);
        m_Ledger.remove(db);
    }
    
    public static final void emptyPools()
    {
        for (Iterator<String> i = m_EmdrosPools.keySet().iterator();
            i.hasNext();)
        {
            String key = i.next();
            EmdrosDatabasePool pool = m_EmdrosPools.get(key);
            pool.clear();
            i.remove();
        }
    }
	
	private static final Connection getLogDatabaseHandle()
	throws DatabaseException {
		return getSqlConnection();
	}

	private static boolean m_IsLibraryLoaded = false;

    public static void loadLibrary() throws Exception 
    {
    	if (m_IsLibraryLoaded)
    		return;
    		
		Preloader.load();
    	
        URL url = Lex.class.getResource("/com/qwirx/crosswire/kjv");
        assert(url != null);
        File[] files = new File[1];
        files[0] = new File(url.getPath());
        SwordBookPath.setAugmentPath(files);

        m_IsLibraryLoaded = true;		
    }
    
    private Lex()
    {
    	// do not call
    }
    
    private static Connection getSqlConnection()
    throws DatabaseException
    {
		// System.out.println("Connection established.");

    	String dsn = "jdbc:mysql://localhost:3306/lex?user=emdf" +
            "&password=changeme&useServerPrepStmts=false" +
            "&jdbcCompliantTruncation=false" +
            "&characterEncoding=utf8";
    	Connection dbconn;
    	
		try
        {
			dbconn = new Driver().connect(dsn, new Properties());
            dbconn.prepareStatement("SET NAMES utf8").executeUpdate();

            /*
			new DbTable("object_types",
				new DbColumn[]{
					new DbColumn("ID",           "INT(11)",     false, 
							true, true),
					new DbColumn("Name",         "VARCHAR(40)", false),
					new DbColumn("Supertype_ID", "INT(11)",     false),
				}
			).check(dbconn, true);
            */
			
            Lexicon.checkDatabase(dbconn);
            Parser.checkDatabase(dbconn);
            DatabaseTransliterator.checkDatabase(dbconn);
            
            new DbTable
            (
                "user_text_access", "utf8",
                new DbColumn[]
                {
                    new DbColumn("ID",           "INT(11)", false, 
                        true, true),
                    new DbColumn("User_Name",    "CHAR(20)", false, 
                        false, false),
                    new DbColumn("Monad_First",  "INT(11)", false),
                    new DbColumn("Monad_Last",   "INT(11)", false),
                    new DbColumn("Write_Access", "ENUM('0','1')", false),
                }
            ).check(dbconn, true);
		}
		catch (SQLException ex)
		{
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: "     + ex.getSQLState());
			System.out.println("VendorError: "  + ex.getErrorCode());
			throw new DatabaseException("Failed to connect to database: " + 
			    dsn, ex);
    	}
		catch (IllegalStateException ex)
		{
    		System.err.println(ex);
    		throw new DatabaseException("Failed to connect to database: " + 
    		    dsn, ex);
    	}
        catch (Exception ex)
        {
            System.err.println(ex);
            throw new DatabaseException("Database check failed: " + 
                dsn, ex);
        }

		return dbconn;
    }
    
	public static void main (String [] args)
	{
			junit.textui.TestRunner.run(LexTest.class);
    }
}

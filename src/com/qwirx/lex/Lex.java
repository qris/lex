/*
 * Created on 18-Jul-2004
 * Heavily based on the TestEmdros class distributed with
 * Emdros 1.2.0-pre73
 */
package com.qwirx.lex;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import jemdros.EmdrosEnv;
import jemdros.eCharsets;
import jemdros.eOutputKind;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.DbColumn;
import com.qwirx.db.sql.DbTable;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.ontology.OntologyDb;

/**
 * @author chris
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Lex 
{
    private static final Logger m_LOG = Logger.getLogger(Lex.class);
    
	private static void showDatabaseError(EmdrosEnv env) 
    {
		m_LOG.error("DB Error: " + env.getDBError());
	}
	
	public static final SqlDatabase getSqlDatabase(String user) 
	throws DatabaseException 
    {
		loadLibrary();
		return new SqlDatabase(getSqlConnection(), user, "lex");
	}

	public static final OntologyDb getOntologyDb() 
	throws SAXException, IOException 
    {
		return OntologyDb.getInstance();
	}

	private static ThreadLocal s_EmdrosDatabaseMap = new ThreadLocal()
	{
        protected synchronized Object initialValue()
        {
        	return new Hashtable();
        }
	};

	public static final EmdrosDatabase getEmdrosDatabase(String user, 
        String host) 
	throws DatabaseException 
    {
    	loadLibrary();
        
    	String key = Thread.currentThread().getId() + "@" + user +
    		"@" + host; 
    	
    	Map tlsMap = (Map)s_EmdrosDatabaseMap.get();
    	
    	EmdrosDatabase db = (EmdrosDatabase)tlsMap.get(key);
    	if (db != null)
    	{
    		db.setLogDatabaseHandle(getLogDatabaseHandle());
    		return db;
    	}
    	
		EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
			eCharsets.kCSISO_8859_1, "localhost", "emdf", "changeme", 
			"wihebrew");

		if (!env.connectionOk()) 
        {
			showDatabaseError(env);
			throw new IllegalStateException("Not connected to database "+
                    "("+env.getDBError()+")");
		}

		EmdrosDatabase emdrosDb = new EmdrosDatabase(
				env, user, host, "wihebrew", getLogDatabaseHandle());
		
        emdrosDb.createObjectTypeIfMissing("note");
        emdrosDb.createFeatureIfMissing("note",  "text",             "string");
		emdrosDb.createFeatureIfMissing("clause","logical_struct_id","integer");
		emdrosDb.createFeatureIfMissing("phrase","argument_name",    "string");
		emdrosDb.createFeatureIfMissing("phrase","type_id",          "integer");
        emdrosDb.createFeatureIfMissing("phrase","macrorole_number", "integer default -1");
		emdrosDb.createFeatureIfMissing("clause","logical_structure","string");
		emdrosDb.createFeatureIfMissing("verse", "bart_gloss",       "string");
        emdrosDb.createFeatureIfMissing("word",  "wordnet_gloss",    "string");
        emdrosDb.createFeatureIfMissing("word",  "wordnet_synset",   "integer");
		
        tlsMap.put(key, emdrosDb);
        
		return emdrosDb;
	}
	
	private static final Connection getLogDatabaseHandle()
	throws DatabaseException {
		return getSqlConnection();
	}

	private static boolean isLibraryLoaded = false;

    public static void loadLibrary() {
    	if (isLibraryLoaded)
    		return;
    		
    	try 
    	{
    		Class.forName("com.qwirx.lex.Preloader").newInstance();
		}
    	catch (Throwable t) 
    	{
			t.printStackTrace(System.err);
			throw(new RuntimeException(t));
		}
    	
		// Get OS name
//		String osName = System.getProperty("os.name");
		// System.out.println(osName);

//		if (osName.matches(".*[Ww]in.*")) {
//			// Substitute your path
//			System.load("c:\\programmer\\emdros\\lib\\jemdros.dll");
//		} else {
//			// Substitute your path
//			System.load("/home/chris/project/emdros/root/lib/emdros/libjemdros.so");
//		}
//
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		}
		catch (Exception e)
		{
			System.err.println(e);
			return;
		}
		
		isLibraryLoaded = true;		
    }
    
    private Lex()
    {
    	// do not call
    }
    
    private static Connection getSqlConnection()
    throws DatabaseException
    {
		// System.out.println("Connection established.");

    	String dsn = "jdbc:mysql://localhost:3306/lex?user=emdf&password=changeme";
    	Connection dbconn;
    	
		try {
			dbconn = DriverManager.getConnection(dsn);

			new DbTable("object_types",
				new DbColumn[]{
					new DbColumn("ID",           "INT(11)",     false, 
							true, true),
					new DbColumn("Name",         "VARCHAR(40)", false),
					new DbColumn("Supertype_ID", "INT(11)",     false),
				}
			).check(dbconn);
			
			new DbTable("lexicon_entries",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)",     false, 
							true, true),
					new DbColumn("Lexeme",    "VARCHAR(40)", true),
					new DbColumn("Structure", "VARCHAR(160)", true),
					new DbColumn("Domain_Parent_ID", "INT(11)",
							true),
					new DbColumn("Domain_Label", "VARCHAR(40)", 
							true),
					new DbColumn("Domain_Desc", "VARCHAR(160)", 
							true),
					new DbColumn("Symbol",      "VARCHAR(40)", true),
					new DbColumn("Gloss",       "VARCHAR(40)", true),
                    new DbColumn("Syntactic_Args", "INT(11)",  false),
                    new DbColumn("Aktionsart",  
                            "ENUM('NONE','INGR','SEML','BECOME')", false),
                    new DbColumn("Active",      "ENUM('0','1')", false),
                    new DbColumn("Pred_Enable", "ENUM('0','1')", false),
                    new DbColumn("Predicate",   "VARCHAR(40)", true),
                    new DbColumn("Arguments",   "ENUM('','X','XY')", false),
                    new DbColumn("Become",      "ENUM('0','1')", false),
                    new DbColumn("Become_Pred", "VARCHAR(40)", false),
                    new DbColumn("Become_Args", "ENUM('','Y','ZX')", false),
                    new DbColumn("Caused",      "ENUM('0','1')", false),
                    new DbColumn("Caused_Aktionsart",  
                            "ENUM('NONE','INGR','SEML','BECOME')", false),
                    new DbColumn("Caused_Active",      "ENUM('0','1')", false),
                    new DbColumn("Caused_Pred_Enable", "ENUM('0','1')", false),
                    new DbColumn("Caused_Predicate",   "VARCHAR(40)", false),
                    new DbColumn("Caused_Arguments",   "ENUM('','X','XY')", false),
                    new DbColumn("Caused_Become",      "ENUM('0','1')", false),
                    new DbColumn("Caused_Become_Pred", "VARCHAR(40)", false),
                    new DbColumn("Caused_Become_Args", "ENUM('','Y','ZX')", false),
                    new DbColumn("Punctual",           "ENUM('0','1')", false),
                    new DbColumn("Has_Result_State",   "ENUM('0','1')", false),
                    new DbColumn("Telic",              "ENUM('0','1')", false),
                    new DbColumn("Thematic_Relation",  "VARCHAR(11)", true),
                    new DbColumn("Dynamic",            "ENUM('0','1')", false),
                    new DbColumn("Has_Endpoint",       "ENUM('0','1')", false),
                    new DbColumn("Result_Predicate",   "VARCHAR(40)", true),
                    new DbColumn("Result_Predicate_Arg",   "VARCHAR(1)", true),
				}
			).check(dbconn);

			new DbTable("lexical_rules",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)",     false, 
							true, true),
					new DbColumn("Symbol",    "VARCHAR(40)", true),
					new DbColumn("Structure", "VARCHAR(80)", true),
				}
			).check(dbconn);

			new DbTable("lexicon_variables",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)", false, 
							true, true),
					new DbColumn("Lexeme_ID", "INT(11)", false),
					new DbColumn("Name",  "VARCHAR(20)", false),
					new DbColumn("Value", "VARCHAR(40)", false),
				}
			).check(dbconn);

            new DbTable
            (
                "user_text_access",
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
            ).check(dbconn);
		}
		catch (SQLException ex)
		{
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: "     + ex.getSQLState());
			System.out.println("VendorError: "  + ex.getErrorCode());
			throw new DatabaseException(ex, "Connecting to "+dsn);
    	}
		catch (IllegalStateException ex)
		{
    		System.err.println(ex);
    		throw new DatabaseException(ex, "Connecting to "+dsn);
    	}

		return dbconn;
    }
    
	public static void main (String [] args)
	{
			junit.textui.TestRunner.run(LexTest.class);
    }
}

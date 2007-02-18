/*
 * Created on 18-Jul-2004
 * Heavily based on the TestEmdros class distributed with
 * Emdros 1.2.0-pre73
 */
package com.qwirx.lex;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;

import jemdros.EmdrosEnv;
import jemdros.eCharsets;
import jemdros.eOutputKind;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.ontology.OntologyDb;
import com.qwirx.lex.sql.SqlDatabase;

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

	public static final EmdrosDatabase getEmdrosDatabase(String user, 
        String host) 
	throws DatabaseException 
    {
    	loadLibrary();
        
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
				env, user, host, "wihebrew1202", getLogDatabaseHandle());
		
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
    		
    	try {
    		Class.forName("com.qwirx.lex.Preloader").newInstance();
		} catch (Throwable t) {
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
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			System.err.println(e);
			return;
		}
		
		isLibraryLoaded = true;		
    }
    
    static class DbColumn 
    {
    	String name, type, defaultValue;
    	boolean isPrimaryKey, isNullAllowed, isAutoNumbered;
    	
    	public DbColumn
        (
            String name, 
            String type, 
            boolean isNullAllowed,
            boolean isPrimaryKey, 
            boolean isAutoNumbered
        ) 
        {
    		this(name, type, isNullAllowed);
    		this.isPrimaryKey   = isPrimaryKey;
    		this.isAutoNumbered = isAutoNumbered;
    	}

    	public DbColumn(String name, String type, boolean isNullAllowed) 
        {
    		this.name           = name;
    		this.type           = type;
    		this.isNullAllowed  = isNullAllowed;
    	}

    }
    
    static class DbTable 
    {
    	String name;
    	DbColumn [] columns;
    	
    	public DbTable(String name, DbColumn [] columns) 
        {
    		this.name    = name;
    		this.columns = columns;
    	}
    }

    public static void cleanUpSql(Statement stmt, ResultSet rs) {
		try { rs.close();   } catch (Exception e) { /* ignore */ }
		try { stmt.close(); } catch (Exception e) { /* ignore */ }
    }

    private static String getColumnSpec(DbColumn col)
    {
    	String sql = col.name + " " + col.type;
    	
		if (!col.isNullAllowed)
			sql += " NOT NULL";
		if (col.defaultValue != null)
			sql += " DEFAULT "+col.defaultValue;
		if (col.isAutoNumbered)
			sql += " AUTO_INCREMENT";
		if (col.isPrimaryKey)
			sql += " PRIMARY KEY";

		return sql;
    }
    
    public static void checkTable(Connection conn, DbTable table) 
    throws SQLException, IllegalStateException 
    {
		Statement stmt = null;
		ResultSet rs = null;
		String sql = null;
        
		try 
		{
			stmt = conn.prepareStatement
				("SHOW TABLES");
			rs = ((PreparedStatement)stmt).executeQuery();
			
			while (rs.next()) 
			{
				String tableName = rs.getString(1);
				if (tableName.equals(table.name)) 
				{	
					cleanUpSql(stmt, rs);
                    sql = "DESCRIBE " + table.name;
					stmt = conn.prepareStatement(sql);
					rs = ((PreparedStatement)stmt).executeQuery();
					Iterator tci = Arrays.asList(table.columns)
						.iterator();
					while (rs.next()) {
						String currentColumnName = rs.getString(1);
						if (!tci.hasNext()) {
							throw new IllegalStateException
								("Table " + table.name +
								" contains unknown column " + 
								currentColumnName);
						}
						
						DbColumn expectedCol = (DbColumn)(tci.next());
						String expectedColName = expectedCol.name;
						if (!expectedColName.equals(currentColumnName))
							throw new IllegalStateException
								("Table "+table.name+
								" column "+currentColumnName+
								" should be called "+expectedColName);

						String expectedType = expectedCol.type.toLowerCase();
                        if (expectedType.startsWith("varchar"))
                        {
                            expectedType = "char" + expectedType.substring(7);
                        }

                        String actualType   = rs.getString(2).toLowerCase();
                        if (actualType.startsWith("varchar"))
                        {
                            actualType = "char" + actualType.substring(7);
                        }
                        
						if (! actualType.equals(expectedType))
							throw new IllegalStateException
								("Table "+table.name+
								" column "+currentColumnName+
								" should have type "+expectedType+
								" instead of "+actualType);
						
						boolean expectedNull = expectedCol.isNullAllowed;
                        boolean passed = false; 
                        
                        String actualNull = rs.getString(3);
                        if (expectedNull && ! actualNull.equals("YES"))
                        {
                            passed = false;
                        }
                        else if (!expectedNull && actualNull.equals("YES"))
                        {
                            passed = false;
                        }
                        else
                        {
                            passed = true;
                        }
                        
						if (! passed)
							throw new IllegalStateException
								("Table "+table.name+
								" column "+currentColumnName+
								" should"+(expectedNull ? "" : " not")+
								" allow NULL ("+actualNull+")");
					}
					
					while (tci.hasNext()) 
					{
						DbColumn missing = (DbColumn)(tci.next());
                        sql = "ALTER TABLE "+table.name+" ADD COLUMN "+
                        getColumnSpec(missing);
						stmt = conn.prepareStatement(sql);
						((PreparedStatement)stmt).executeUpdate();
					}
					
					return;
				}
					
			}
			
			sql = "CREATE TABLE "+table.name+ " (";
			
			for (int i = 0; i < table.columns.length; i++) {
				DbColumn col = table.columns[i];
				sql += getColumnSpec(col);
				if (i < table.columns.length - 1)
					sql += ", ";
			}
			
			sql += ")";
			
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
        } 
        catch (SQLException e)
        {
            m_LOG.error("Error executing SQL statement "+sql+": "+e);
            throw e;
		} 
        finally 
        {
			cleanUpSql(stmt, rs);
		}
    }
    
    /*
    private FeatureInfo [] newFeatures = new FeatureInfo[]{
    		new FeatureInfo("clause","logical_struct_id","integer"),
    		new FeatureInfo("phrase","argument_name",    "string"),
    		new FeatureInfo("phrase","type_id",          "integer")
    };
    
    public List getNewFeatures() {
    	return Arrays.asList(newFeatures);
    }
    */
    
    private Lex() {
    	// do not call
    }
    
    private static Connection getSqlConnection() throws DatabaseException {
		// System.out.println("Connection established.");

    	String dsn = "jdbc:mysql://localhost:3306/lex?user=emdf&password=changeme";
    	Connection dbconn;
    	
		try {
			dbconn = DriverManager.getConnection(dsn);

			checkTable(dbconn,
				new DbTable("object_types",
					new DbColumn[]{
						new DbColumn("ID",           "INT(11)",     false, 
								true, true),
						new DbColumn("Name",         "VARCHAR(40)", false),
						new DbColumn("Supertype_ID", "INT(11)",     false),
					}
				)
			);
			
			checkTable(dbconn,
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
					)
				);

			checkTable(dbconn,
					new DbTable("lexical_rules",
						new DbColumn[]{
							new DbColumn("ID",        "INT(11)",     false, 
									true, true),
							new DbColumn("Symbol",    "VARCHAR(40)", true),
							new DbColumn("Structure", "VARCHAR(80)", true),
						}
					)
				);

			checkTable(dbconn,
					new DbTable("lexicon_variables",
						new DbColumn[]{
							new DbColumn("ID",        "INT(11)", false, 
									true, true),
							new DbColumn("Lexeme_ID", "INT(11)", false),
							new DbColumn("Name",  "VARCHAR(20)", false),
							new DbColumn("Value", "VARCHAR(40)", false),
						}
					)
				);

			checkTable(dbconn,
					new DbTable("change_log",
						new DbColumn[]{
							new DbColumn("ID",         "INT(11)", false, 
									true, true),
							new DbColumn("User",       "VARCHAR(20)", false),
							new DbColumn("Date_Time",  "DATETIME",    false),
							new DbColumn("DB_Type",    "ENUM('Emdros','SQL')", 
									false),
							new DbColumn("DB_Name",    "VARCHAR(40)", false),
							new DbColumn("Table_Name", "VARCHAR(40)", false),
							new DbColumn("Cmd_Type",  
									"ENUM('INSERT','UPDATE','DELETE')",	false),
						}
					)
				);

			checkTable(dbconn,
					new DbTable("changed_rows",
						new DbColumn[]{
							new DbColumn("ID",        "INT(11)", false, 
									true, true),
							new DbColumn("Log_ID",    "INT(11)", false),
							new DbColumn("Unique_ID", "INT(11)", false),
						}
					)
				);

			checkTable(dbconn,
					new DbTable("changed_values",
						new DbColumn[]{
							new DbColumn("ID",        "INT(11)", false, 
									true, true),
							new DbColumn("Row_ID",    "INT(11)", false),
							new DbColumn("Col_Name",  "VARCHAR(40)", false),
							new DbColumn("Old_Value", "MEDIUMTEXT", true),
							new DbColumn("New_Value", "MEDIUMTEXT", true),
						}
					)
				);

            checkTable
            (
                dbconn,
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
                )
            );
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: "     + ex.getSQLState());
			System.out.println("VendorError: "  + ex.getErrorCode());
			throw new DatabaseException(ex, "Connecting to "+dsn);
    	} catch (IllegalStateException ex) {
    		System.err.println(ex);
    		throw new DatabaseException(ex, "Connecting to "+dsn);
    	}

		DataType.setConnection(dbconn);
		
		return dbconn;

		/*
		sqlDb    = new SqlDatabase   (m_sql, "chris", "lex");
    	emdrosDb = new EmdrosDatabase(m_env, "chris", "lex", m_sql);
    	*/
    }
    
	public static void main (String [] args)
	{
			junit.textui.TestRunner.run(LexTest.class);
    }
}

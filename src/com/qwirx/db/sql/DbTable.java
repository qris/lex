/**
 * 
 */
package com.qwirx.db.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;

public class DbTable 
{
	private String m_Name, m_Charset;
	private DbColumn [] m_Columns;
    private static final Logger m_LOG = Logger.getLogger(DbTable.class);

	public DbTable(String name, String charset, DbColumn [] columns) 
    {
		this.m_Name    = name;
        this.m_Charset = charset;
		this.m_Columns = columns;
	}

    private void cleanUpSql(Statement stmt, ResultSet rs)
    {
		try { rs.close();   } catch (Exception e) { /* ignore */ }
		try { stmt.close(); } catch (Exception e) { /* ignore */ }
    }

    private static boolean DATABASE_CHECKS_ENABLED = true;
    private static File DATABASE_CONFIG_FILE;
	public static final String DATABASE_CHECK_PROPERTY = 
		"database.check.enabled";
    
    static
    {
    	URL propFileBase = DbTable.class.getResource("/");
    	String propFileName = "com.qwirx.db.properties";
    	DATABASE_CONFIG_FILE = new File(propFileBase.getPath(), propFileName);
    	
    	if (!DATABASE_CONFIG_FILE.exists())
    	{
    		m_LOG.warn("Failed to load database properties file: " + 
    				DATABASE_CONFIG_FILE);
    	}
    	else
    	{
    		Properties props = new Properties();
    		try
    		{
    			props.load(new FileInputStream(DATABASE_CONFIG_FILE));
    		}
    		catch (IOException e)
    		{
        		m_LOG.warn("Failed to load database properties file: " + 
        				DATABASE_CONFIG_FILE, e);		
    		}
    		
    		m_LOG.info("Loading database configuration from " +
    				DATABASE_CONFIG_FILE);

    		String enabled = props.getProperty(DATABASE_CHECK_PROPERTY);
    		
    		if (enabled != null)
    		{
    			if (enabled.equalsIgnoreCase("no") ||
    				enabled.equalsIgnoreCase("false"))
    			{
    				DATABASE_CHECKS_ENABLED = false;
    	    		m_LOG.info("Database checks disabled: enable by setting " +
    	    				DATABASE_CHECK_PROPERTY + "to 'yes'");
    			}
    		}

    		if (DATABASE_CHECKS_ENABLED)
    		{
	    		m_LOG.info("Database checks enabled: disable by setting " +
	    				DATABASE_CHECK_PROPERTY + "to 'no'");
    		}
    	}    	
    }

    public void check(Connection conn, boolean addMissingColumns) 
    throws SQLException, IllegalStateException 
    {
    	if (DATABASE_CHECKS_ENABLED)
    	{
			m_LOG.info("Checking table " + m_Name);
    	}
    	else
    	{
    		m_LOG.info("Not checking table " + m_Name);
    		return;    		
    	}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		String sql = null;
        
		try 
		{
			stmt = conn.prepareStatement("SHOW TABLES");
			rs = stmt.executeQuery();
			
			while (rs.next()) 
			{
				String tableName = rs.getString(1);
				
				if (tableName.equals(m_Name)) 
				{	
					cleanUpSql(stmt, rs);
                    sql = "DESCRIBE " + m_Name;
					stmt = conn.prepareStatement(sql);
					rs = stmt.executeQuery();
					Iterator tci = Arrays.asList(m_Columns)
						.iterator();
					
					while (rs.next())
					{
						String currentColumnName = rs.getString(1);
						if (!tci.hasNext())
						{
							throw new IllegalStateException
								("Table " + m_Name +
								" contains unknown column " + 
								currentColumnName);
						}
						
						DbColumn expectedCol = (DbColumn)(tci.next());
						String expectedColName = expectedCol.name;
						if (!expectedColName.equalsIgnoreCase(currentColumnName))
						{
							throw new IllegalStateException
								("Table "+m_Name+
								" column "+currentColumnName+
								" should be called "+expectedColName);
						}
						
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
						{
							throw new IllegalStateException
								("Table "+m_Name+
								" column "+currentColumnName+
								" should have type "+expectedType+
								" instead of "+actualType);
						}
						
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
                        {
							throw new IllegalStateException
								("Table "+m_Name+
								" column "+currentColumnName+
								" should"+(expectedNull ? "" : " not")+
								" allow NULL ("+actualNull+")");
                        }
					}
                    
                    rs.close();
                    stmt.close();
					
                    if (m_Charset != null)
                    {
	                    stmt = conn.prepareStatement("SHOW CREATE TABLE " +
	                        this.m_Name);
	                    rs = stmt.executeQuery();
	                    rs.next();
	                    String charset = rs.getString(2);
	                    rs.close();
	                    stmt.close();
	                    
	                    charset = charset.replaceFirst("(?s).* DEFAULT " +
	                    		"CHARSET=", "");
	                    charset = charset.replaceFirst(" .*", "");
	                    
	                    if (! charset.equals(m_Charset))
	                    {
	                    	throw new IllegalStateException("Table " + m_Name + 
	                    			" has wrong character set " + charset + 
	                    			" instead of " + m_Charset);
	                    }
                    }
                    
                    if (!tci.hasNext())
                    {
                        return;
                    }
                    
                    if (addMissingColumns)
                    {
                        while (tci.hasNext()) 
                        {
                            DbColumn missing = (DbColumn)(tci.next());
                            sql = "ALTER TABLE "+m_Name+" ADD COLUMN "+
                            missing.getSpec();
                            stmt = conn.prepareStatement(sql);
                            stmt.executeUpdate();
                        }
                    }
                    else
                    {
                        DbColumn missing = (DbColumn)(tci.next());
                        throw new IllegalStateException
                            ("Table "+m_Name+" missing column "+missing.name+
                            " " + missing.getSpec());
                    }
                    
					return;
				}
			}
			
			sql = "CREATE TABLE "+m_Name+ " (";
			
			for (int i = 0; i < m_Columns.length; i++)
			{
				DbColumn col = m_Columns[i];
				sql += col.getSpec();
				if (i < m_Columns.length - 1)
					sql += ", ";
			}
			
			sql += ") CHARACTER SET " + m_Charset;
			
			stmt = conn.prepareStatement(sql);
			stmt.executeUpdate();
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
}
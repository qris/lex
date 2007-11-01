/**
 * 
 */
package com.qwirx.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class DbTable 
{
	String name;
	DbColumn [] columns;
    private static final Logger m_LOG = Logger.getLogger(DbTable.class);

	public DbTable(String name, DbColumn [] columns) 
    {
		this.name    = name;
		this.columns = columns;
	}

    private void cleanUpSql(Statement stmt, ResultSet rs)
    {
		try { rs.close();   } catch (Exception e) { /* ignore */ }
		try { stmt.close(); } catch (Exception e) { /* ignore */ }
    }


    public void check(Connection conn) 
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
				if (tableName.equals(name)) 
				{	
					cleanUpSql(stmt, rs);
                    sql = "DESCRIBE " + name;
					stmt = conn.prepareStatement(sql);
					rs = ((PreparedStatement)stmt).executeQuery();
					Iterator tci = Arrays.asList(columns)
						.iterator();
					while (rs.next()) {
						String currentColumnName = rs.getString(1);
						if (!tci.hasNext()) {
							throw new IllegalStateException
								("Table " + name +
								" contains unknown column " + 
								currentColumnName);
						}
						
						DbColumn expectedCol = (DbColumn)(tci.next());
						String expectedColName = expectedCol.name;
						if (!expectedColName.equals(currentColumnName))
							throw new IllegalStateException
								("Table "+name+
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
								("Table "+name+
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
								("Table "+name+
								" column "+currentColumnName+
								" should"+(expectedNull ? "" : " not")+
								" allow NULL ("+actualNull+")");
					}
					
					while (tci.hasNext()) 
					{
						DbColumn missing = (DbColumn)(tci.next());
                        sql = "ALTER TABLE "+name+" ADD COLUMN "+
                        missing.getSpec();
						stmt = conn.prepareStatement(sql);
						((PreparedStatement)stmt).executeUpdate();
					}
					
					return;
				}
					
			}
			
			sql = "CREATE TABLE "+name+ " (";
			
			for (int i = 0; i < columns.length; i++) {
				DbColumn col = columns[i];
				sql += col.getSpec();
				if (i < columns.length - 1)
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
}
/*
 * Created on 26-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.qwirx.db.Change;
import com.qwirx.db.Database;
import com.qwirx.db.DatabaseException;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SqlDatabase implements Database
{
	private Connection conn;
	private PreparedStatement stmt;
	private ResultSet rs;
	private String username, database, query;
	private long startTime;
    private static final Logger m_LOG = Logger.getLogger(SqlDatabase.class);
    
	public SqlDatabase(Connection conn, String username, String database)
	throws DatabaseException
    {
		this.conn = conn;
		this.username = username;
		this.database = database;
		
		try
		{
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
			).check(conn);

			new DbTable("changed_rows",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)", false, 
							true, true),
					new DbColumn("Log_ID",    "INT(11)", false),
					new DbColumn("Unique_ID", "INT(11)", false),
				}
			).check(conn);

			new DbTable("changed_values",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)", false, 
							true, true),
					new DbColumn("Row_ID",    "INT(11)", false),
					new DbColumn("Col_Name",  "VARCHAR(40)", false),
					new DbColumn("Old_Value", "MEDIUMTEXT", true),
					new DbColumn("New_Value", "MEDIUMTEXT", true),
				}
			).check(conn);
		}
		catch (SQLException e)
		{
			throw new DatabaseException(e, "Error checking table structures");
		}
	}
	
	public void executeDirect(String sql) throws DatabaseException 
    {
		try 
        {
            long startTime = System.currentTimeMillis();
			PreparedStatement s = conn.prepareStatement(sql);
			s.executeUpdate();
			s.close();
            long totalTime = System.currentTimeMillis() - startTime;
            m_LOG.info(totalTime+" ms to "+sql);
		} 
        catch (SQLException e) 
        {
            m_LOG.error(sql, e);
			throw new DatabaseException(e, sql);
		}
	}

	public PreparedStatement prepareSelect(String sql) 
	throws DatabaseException 
    {
		this.query = sql;
		this.startTime = System.currentTimeMillis();
        
		if (stmt != null || rs != null) 
        {
			throw new IllegalStateException("Previous statement not finished");
		}
		
		try 
        {
			stmt = conn.prepareStatement(sql);
		} 
        catch (SQLException e) 
        {
			throw new DatabaseException(e, sql);
		}
		
		return stmt;
	}
	
	public ResultSet select() throws DatabaseException 
    {
		try 
        {
            long startTime = System.currentTimeMillis();
			rs = stmt.executeQuery();
            long totalTime = System.currentTimeMillis() - startTime;
            m_LOG.info(totalTime+" ms to execute "+query);
            
			return rs;
		} 
        catch (SQLException e) 
        {
			try { stmt.close(); } catch (Exception e2) { /* ignore */ }
            m_LOG.error(query, e);
			throw new DatabaseException(e, query);
		}
	}
	
	public void finish() throws DatabaseException 
    {
		try 
        {
			if (rs != null)
			{
				rs.close();
				rs = null;
			}
			
			if (stmt != null)
			{
				stmt.close();
				stmt = null;

				long totalTime = System.currentTimeMillis() - startTime;
		        m_LOG.info(totalTime+" ms to finish  "+query);
		        query = null;
			}
		} 
        catch (SQLException e) 
        {
			try { stmt.close(); } catch (Exception e2) { /* ignore */ }
            m_LOG.error(query, e);
			throw new DatabaseException(e, query);
		}
        
	}
	
	public Change createChange(Object type, String table, 
			Object conditions)
	{
		return new SqlChange(username, database, 
			(SqlChange.Type)type, table, (String)conditions, conn);
	}
	
	public void close() throws SQLException
	{
		conn.close();
	}
}

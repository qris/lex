/*
 * Created on 26-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.qwirx.lex.Change;
import com.qwirx.lex.Database;
import com.qwirx.lex.DatabaseException;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SqlDatabase implements Database {
	private Connection conn;
	private PreparedStatement stmt;
	private ResultSet rs;
	private String username, database, query;
	private long startTime;
    private static final Logger m_LOG = Logger.getLogger(SqlDatabase.class);
    
	public SqlDatabase(Connection conn, String username, String database) 
    {
		this.conn = conn;
		this.username = username;
		this.database = database;
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
			if (rs != null) {
				rs.close();
				rs = null;
			}
			
			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		} 
        catch (SQLException e) 
        {
			try { stmt.close(); } catch (Exception e2) { /* ignore */ }
            m_LOG.error(query, e);
			throw new DatabaseException(e, query);
		}
        
        long totalTime = System.currentTimeMillis() - startTime;
        m_LOG.info(totalTime+" ms to finish  "+query);
	}
	
	public Change createChange(Object type, String table, 
			Object conditions)
	{
		return new SqlChange(username, database, 
			(SqlChange.Type)type, table, (String)conditions, conn);
	}
}

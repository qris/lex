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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.qwirx.db.Change;
import com.qwirx.db.ChangeType;
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
	private Connection m_Connection;
	private PreparedStatement stmt;
	private ResultSet rs;
	private String username, database, query;
	private long startTime;
    private static final Logger m_LOG = Logger.getLogger(SqlDatabase.class);
    
    /*
    private boolean m_TrackChanges = false;
    public void setTrackChanges(boolean trackChanges)
    {
    	m_TrackChanges = trackChanges;
    }
    */
    
	public SqlDatabase(Connection conn, String username, String database)
	throws DatabaseException
    {
		this.m_Connection = conn;
		this.username = username;
		this.database = database;
		
		try
		{
			new DbTable("change_log", "utf8",
				new DbColumn[]{
					new DbColumn("ID",         "INT(11)", false, 
							true, true),
					new DbColumn("User",       "VARCHAR(60)", false),
					new DbColumn("Date_Time",  "DATETIME",    false),
					new DbColumn("DB_Type",    "ENUM('Emdros','SQL')", 
							false),
					new DbColumn("DB_Name",    "VARCHAR(40)", false),
					new DbColumn("Table_Name", "VARCHAR(40)", false),
					new DbColumn("Cmd_Type",  
							"ENUM('INSERT','UPDATE','DELETE')",	false),
				}
			).check(conn, true);

			new DbTable("changed_rows", "utf8",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)", false, 
							true, true),
					new DbColumn("Log_ID",    "INT(11)", false),
					new DbColumn("Unique_ID", "INT(11)", false),
				}
			).check(conn, true);

			new DbTable("changed_values", "utf8",
				new DbColumn[]{
					new DbColumn("ID",        "INT(11)", false, 
							true, true),
					new DbColumn("Row_ID",    "INT(11)", false),
					new DbColumn("Col_Name",  "VARCHAR(40)", false),
					new DbColumn("Old_Value", "MEDIUMTEXT", true),
					new DbColumn("New_Value", "MEDIUMTEXT", true),
				}
			).check(conn, true);
		}
		catch (SQLException e)
		{
			throw new DatabaseException("Error checking table structures", e);
		}
	}
    
    public Connection getConnection()
    {
        return m_Connection;
    }
    
	public void executeDirect(String sql) throws DatabaseException 
    {
		try 
        {
            long startTime = System.currentTimeMillis();
			PreparedStatement s = m_Connection.prepareStatement(sql);
			s.executeUpdate();
			s.close();
            long totalTime = System.currentTimeMillis() - startTime;
            m_LOG.info(totalTime+" ms to "+sql);
		} 
        catch (SQLException e) 
        {
            m_LOG.error(sql, e);
			throw new DatabaseException("Failed to execute direct query", 
			    e, sql);
		}
	}

	public PreparedStatement prepareSelectIndependent(String sql) 
	throws DatabaseException 
    {
		PreparedStatement istmt;
		
		try 
        {
			istmt = m_Connection.prepareStatement(sql);
		} 
        catch (SQLException e) 
        {
			throw new DatabaseException("Failed to prepare query", e, sql);
		}
		
		return istmt;
	}

    private Throwable m_PreviousStatementStarted;
    
	public PreparedStatement prepareSelect(String sql) 
	throws DatabaseException 
    {
		this.query = sql;
		this.startTime = System.currentTimeMillis();
        
		if (stmt != null || rs != null) 
        {
			throw new IllegalStateException("Previous statement not finished",
                m_PreviousStatementStarted);
		}
		
        m_PreviousStatementStarted = new Throwable();
        
		try 
        {
			stmt = m_Connection.prepareStatement(sql);
		} 
        catch (SQLException e) 
        {
			throw new DatabaseException("Failed to prepare query", e, sql);
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
			throw new DatabaseException("Failed to execute SELECT statement",
			    e, query);
		}
	}

	public ResultSet select(String sql) throws DatabaseException
	{
		prepareSelect(sql);
		return select();
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
			throw new DatabaseException("Failed to finish query", e, query);
		}
        
	}
	
	public Change createChange(ChangeType type, String table, 
			Object conditions)
	{
		return new SqlChange(username, database, 
			(SqlChange.Type)type, table, (String)conditions, m_Connection);
	}
	
	public SqlChange loadChange(int id) throws DatabaseException
	{
		try
		{
			return SqlChange.load(id, m_Connection, username);
		}
		catch (SQLException e)
		{
			throw new DatabaseException("Failed to reload SqlChange record", e);
		}
	}
	
	public void close() throws SQLException
	{
		m_Connection.close();
	}
	
    public int getSingleInteger(String query)
    throws DatabaseException
    {
        prepareSelect(query);
        
        ResultSet rs = select();
        
        try
        {
        	if (!rs.next())
        	{
        		throw new DatabaseException("No results", query);
        	}
        }
        catch (SQLException e)
        {
        	throw new DatabaseException("No results", e);
        }
        
        int value;
        
        try
        {
        	value = rs.getInt(1);
        }
        catch (SQLException e)
        {
        	throw new DatabaseException("Failed to get value of " +
        			"column 1 as integer", e);
        }
        
        finish();
        
        return value;
    }

    public List<String[]> getTableAsList(String query)
	throws DatabaseException
	{
    	try
    	{
		    List results = new ArrayList();
		    prepareSelect(query);
		    
		    ResultSet rs = select();
		    int cols = rs.getMetaData().getColumnCount();
		    
		    while (rs.next())
		    {
		        String [] result = new String [cols];
		        for (int i = 0; i < cols; i++)
		        {
		            result[i] = rs.getString(i+1);
		        }
		        results.add(result);
		    }
		    
		    finish();
		    
		    return results;
    	}
    	catch (SQLException e)
    	{
    		throw new DatabaseException("Failed to get database table " +
    				"as List", e, query);
    	}
	}

    public List<Map> getTableAsListOfHashes(String query)
	throws DatabaseException
	{
    	try
    	{
		    List<Map> results = new ArrayList<Map>();
		    prepareSelect(query);
		    
		    ResultSet rs = select();
		    int cols = rs.getMetaData().getColumnCount();
		    
		    while (rs.next())
		    {
		        Map<String, String> result = new HashMap<String, String>();
		        for (int i = 0; i < cols; i++)
		        {
		        	result.put(rs.getMetaData().getColumnLabel(i+1),
		        			getString(i+1));
		        }
		        results.add(result);
		    }
		    
		    finish();
		    
		    return results;
    	}
    	catch (SQLException e)
    	{
    		throw new DatabaseException("Failed to get database table " +
    				"as List", e, query);
    	}
	}
    
    /**
     * Returns the first column of the results of a query as an array of Strings.
     * @param query the SQL query whose results will be returned in the List
     * @return a List of Strings from the first columns of the results.
     * @throws DatabaseException
     * @throws SQLException
     */
    public String [] getColumnAsArray(String query)
    throws DatabaseException, SQLException
    {
        List list = getColumnAsList(query);
        String [] array = new String [list.size()];
        
        int index = 0;
        for (Iterator it = list.iterator(); it.hasNext();)
        {
            array[index++] = (String)it.next();
        }
        
        return array;
    }
    
    /**
     * Returns the first column of the results of a query as a List of Strings.
     * @param query the SQL query whose results will be returned in the List
     * @return a List of Strings from the first column of the results.
     * @throws DatabaseException
     * @throws SQLException
     */
    public List getColumnAsList(String query)
    throws DatabaseException, SQLException
    {
        List results = new ArrayList();
        prepareSelect(query);
        ResultSet rs = select();
        
        while (rs.next())
        {
            results.add(rs.getString(1));
        }
        
        finish();
        return results;
    }

    /**
     * Returns a map from the first to the second columns of the results
     * of a query as a Map of Strings to Strings.
     * @param query the SQL query whose results will be returned in the Map
     * @return a Map of Strings to Strings from the first two columns of the
     * results.
     * @throws DatabaseException
     * @throws SQLException
     */
    public Map fetchMap(String query)
    throws DatabaseException, SQLException
    {
        Map results = new HashMap();
        prepareSelect(query);
        ResultSet rs = select();
        
        while (rs.next())
        {
            results.put(rs.getString(1), rs.getString(2));
        }
        
        finish();
        return results;
    }
    
    /**
     * Returns a map from the first to the second columns of the results
     * of a query, grouped by the first column, as a Map of Strings to String[]s.
     * @param query the SQL query whose results will be returned in the Map
     * @return a Map of Strings to String[]s from the first two columns of the
     * results.
     * @throws DatabaseException
     * @throws SQLException
     */
    public Map fetchGroupMap(String query)
    throws DatabaseException, SQLException
    {
        Map results = new HashMap();
        prepareSelect(query);
        ResultSet rs = select();
        
        while (rs.next())
        {
            String key = rs.getString(1);
            String value = rs.getString(2);
            
            List list = (List)results.get(key);
            if (list == null)
            {
                list = new ArrayList();
                results.put(key, list);
            }
            
            list.add(value);
        }
        
        finish();
        
        for (Iterator i = results.keySet().iterator(); i.hasNext();)
        {
            String key = (String)i.next(); 
            List list = (List)results.get(key);
            String [] array = new String [list.size()];
            array = (String[])list.toArray(array);
            results.put(key, array);
        }
        
        return results;
    }
    /**
     * Ugly hack replacement for ResultSet.getString() because
     * getString() doesn't work for 0000-00-00 values, and 
     * noDatetimeStringSync=true returns empty string instead of
     * 0000-00-00 contrary to the docs. 
     * @param columnName
     * @return
     * @throws SQLException
     */
    public String getString(String columnName) throws SQLException
    {
        // FIXME date 0000-00-00 in a MySQL database causes
        // an exception when we call getString() on it
        
        try
        {
            return rs.getString(columnName);
        }
        catch (SQLException e)
        {
            if (e.getMessage().equals("Value '0000-00-00' " +
                "can not be represented as java.sql.Date"))
            {
                return "0000-00-00";
            }
           
            throw e;
        }
    }

    public String getString(int columnNum) throws SQLException
    {
        // FIXME date 0000-00-00 in a MySQL database causes
        // an exception when we call getString() on it
        
        try
        {
            return rs.getString(columnNum);
        }
        catch (SQLException e)
        {
            if (e.getMessage().equals("Value '0000-00-00' " +
                "can not be represented as java.sql.Date"))
            {
                return "0000-00-00";
            }
           
            throw e;
        }
    }
	
    public String getStringNotNull(String colName, String defaultValue)
    throws SQLException
    {
        String value = getString(colName);
        
        if (value == null)
        {
            value = defaultValue;
        }
        
        return value;
    }
	
    public Object getObject(String columnName) throws SQLException
    {
        // FIXME date 0000-00-00 in a MySQL database causes
        // an exception when we call getString() on it
        
        try
        {
            return rs.getObject(columnName);
        }
        catch (SQLException e)
        {
            if (e.getMessage().equals("Value '0000-00-00' " +
                "can not be represented as java.sql.Date"))
            {
                return "0000-00-00";
            }
           
            throw e;
        }
    }
	
    /**
     * Ugly hack replacement for ResultSet.getString() because
     * getString() doesn't work for 0000-00-00 values, and 
     * noDatetimeStringSync=true returns empty string instead of
     * 0000-00-00 contrary to the docs. 
     * @param columnNum
     * @return
     * @throws SQLException
     */
    public static Object getObject(int columnNum,
    		ResultSet rs)
    throws SQLException
    {
    	// FIXME date 0000-00-00 in a MySQL database causes
    	// an exception when we call getString() on it

    	try
    	{
    		return rs.getObject(columnNum);
    	}
    	catch (SQLException e)
    	{
    		if (e.getMessage().equals("Value '0000-00-00' " +
    		"can not be represented as java.sql.Date"))
    		{
    			return "0000-00-00";
    		}

    		throw e;
    	}
    }

    /**
     * Ugly hack replacement for ResultSet.getString() because
     * getString() doesn't work for 0000-00-00 values, and 
     * noDatetimeStringSync=true returns empty string instead of
     * 0000-00-00 contrary to the docs. 
     * @param columnName
     * @param rs
     * @return
     * @throws SQLException
     */
    public static Object getObject(String columnName,
    		ResultSet rs)
    throws SQLException
    {
    	// FIXME date 0000-00-00 in a MySQL database causes
    	// an exception when we call getString() on it

    	try
    	{
    		return rs.getObject(columnName);
    	}
    	catch (SQLException e)
    	{
    		if (e.getMessage().equals("Value '0000-00-00' " +
    		"can not be represented as java.sql.Date"))
    		{
    			return "0000-00-00";
    		}

    		throw e;
    	}
    }
    
    public void beginTransaction() throws DatabaseException
    {
        try
        {
            m_Connection.setAutoCommit(false);
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to start transaction", e);
        }
    }
    
    public void commitTransaction() throws DatabaseException
    {
        try
        {
            m_Connection.commit();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to commit transaction", e);
        }
    }

    public void cancelTransaction() throws DatabaseException
    {
        try
        {
            m_Connection.rollback();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to cancel transaction", e);
        }
    }
}

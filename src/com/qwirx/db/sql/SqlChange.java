/*
 * Created on 02-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.qwirx.db.Change;
import com.qwirx.db.DatabaseException;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public final class SqlChange implements Change {
	private Type   type;
	private Map    fields;
	private String username, database, table, conditions;
	private Connection conn;
	private int    id;
	private int    insertedRowId = -1;
    private static final Logger m_LOG = Logger.getLogger(SqlChange.class);
	
	public SqlChange(String username, String database, 
			Type type, String table, String conditions,
			Connection conn) 
	{
		this.username   = username;
		this.database   = database;
		this.type       = type;
		this.table      = table;
		this.conditions = conditions;
		this.fields     = new HashMap();
		this.conn       = conn;
	}

	final static class Type {
		private String name;
		private Type(String name) { this.name = name; }
		public String toString() { return this.name; }
	}
    
    class Constant
    {
        private String m_value;
        public Constant(String value) { m_value = value; }
        public String toString() { return m_value; }
    }

	public static final Type
		INSERT = new Type("INSERT"),
		UPDATE = new Type("UPDATE"),
		DELETE = new Type("DELETE");
	
	private int insertRowChangeLog(int originalRowId) 
	throws SQLException
	{
		PreparedStatement stmt = prepareAndLogError(
				"INSERT INTO changed_rows SET Log_ID = ?, " +
				"Unique_ID = ?");
		stmt.setInt(1, this.id);
		stmt.setInt(2, originalRowId);
		stmt.executeUpdate();
		stmt.close();

		stmt = prepareAndLogError(
			"SELECT LAST_INSERT_ID()");
		ResultSet rs = executeQueryAndLogError(stmt);
		rs.next();
		int logRowEntryId = rs.getInt(1);
		rs.close();
		stmt.close();

		return logRowEntryId;
	}			

	private int findRowChangeLog(int originalRowId) 
	throws SQLException
	{
		PreparedStatement stmt = prepareAndLogError(
				"SELECT ID FROM changed_rows WHERE Log_ID = ? AND " +
				"Unique_ID = ?");
		stmt.setInt(1, this.id);
		stmt.setInt(2, originalRowId);
		ResultSet rs = executeQueryAndLogError(stmt);
		rs.next();
		int logRowEntryId = rs.getInt(1);
		rs.close();
		stmt.close();

		return logRowEntryId;
	}			

	private void captureValues(boolean createRowChangeLogs, boolean storeAsNewValue) 
	throws DatabaseException
	{
		/*
		System.out.println("Capturing values for "+type.toString()+" ("+
				createRowChangeLogs+", "+storeAsNewValue+")");
				*/
		
		StringBuffer sb = new StringBuffer("SELECT * FROM ").append(table);
		
		if (conditions != null)
		{
			sb.append(" WHERE ").append(conditions);
		}
		
		PreparedStatement stmt;
		ResultSet rs;
		
		try
		{
		    stmt = prepareAndLogError(sb.toString());
		    rs = executeQueryAndLogError(stmt);
		}
		catch (SQLException e)
		{
		    throw new DatabaseException("Failed to capture old values " +
		    		"of rows. You may have an error in your conditions: " +
		    		conditions, e);
		}

		ResultSetMetaData md = null;
		
		try
		{
		    md = stmt.getMetaData();
		}
		catch (SQLException e)
		{
            throw new DatabaseException("Failed to get metadata for " +
                "capturing old values of rows.", e);
		}
		
		String destColName = storeAsNewValue ? "New_Value" :  "Old_Value";

		try
		{
	        String query;

	        if (createRowChangeLogs)
	        {
	            query = "INSERT INTO changed_values SET " + destColName +
	                " = ?, Row_ID = ?, Col_Name = ?";
	        }
	        else
	        {
	            query = "UPDATE changed_values SET " + destColName + " = ? " +
	                "WHERE Row_ID = ? AND Col_Name = ?";
	        }

	        PreparedStatement cvs = prepareAndLogError(query);
		
  			while (rs.next())
  			{
  			    int uniqueId = rs.getInt("ID");

  			    int logRowEntryId;

  			    if (createRowChangeLogs)
  			    {
  			        logRowEntryId = insertRowChangeLog(uniqueId);
  			    }
  			    else
  			    { 
  			        logRowEntryId = findRowChangeLog(uniqueId);
  			    }

  			    for (int i = 1; i <= md.getColumnCount(); i++)
  			    {
  			        String colName  = md.getColumnName(i);
  			        String curValue = null;

  			        // FIXME date 0000-00-00 in a MySQL database causes
  			        // an exception when we call getString() on it
  			        try
  			        {
  			            curValue = rs.getString(i);
  			        }
  			        catch (SQLException e)
  			        {
  			            if (e.getMessage().equals("Value '0000-00-00' " +
  			                "can not be represented as java.sql.Date"))
  			            {
  			                // do nothing
  			            }
  			            else
  			            {
  			                throw e;
  			            }
  			        }

  			        cvs.setString(1, curValue);
  			        cvs.setInt   (2, logRowEntryId);
  			        cvs.setString(3, colName);
  			        cvs.executeUpdate();
  			    }

  			    stmt = prepareAndLogError("DELETE FROM changed_values " +
  			        "WHERE Row_ID = ? AND ((Old_Value = New_Value) OR " +
  			        "(Old_Value IS NULL AND New_Value IS NULL))");

  			    stmt.setInt(1, logRowEntryId);
  			    stmt.executeUpdate();
  			}

  			cvs.close();
  			rs.close();
  			stmt.close();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to store old values in " +
                "change tracking tables. Perhaps there is a problem " +
                "with the tables?", e);
        }
	}
	
	private PreparedStatement prepareAndLogError(String query) 
	throws SQLException
	{
		try
		{
			return conn.prepareStatement(query);
		}
		catch (SQLException e)
		{
			System.err.println("Error preparing query ("+query+"): "+e);
			throw(e);
		}
	}
	
	private ResultSet executeQueryAndLogError(PreparedStatement stmt) 
	throws SQLException 
	{
		try
		{
			return stmt.executeQuery();
		}
		catch (SQLException e)
		{
			System.err.println("Error executing query ("+stmt.toString()+"): "+e);
			throw(e);
		}
	}
	
	private void captureOldValues() 
	throws DatabaseException
	{
		if (type != UPDATE && type != DELETE)
			throw new AssertionError("this method can only be used "+
					"when the command type is UPDATE or DELETE");
		
		captureValues(true, false);
	}
	
	private void captureNewValues() 
	throws DatabaseException 
	{
		if (type != INSERT && type != UPDATE)
		{
			throw new AssertionError("this method can only be used "+
					"when the command type is INSERT or UPDATE");
		}
		
		if (type == INSERT)
		{
		    try
		    {
    			PreparedStatement stmt = prepareAndLogError(
    					"SELECT LAST_INSERT_ID()");
    			ResultSet rs = executeQueryAndLogError(stmt);
    			rs.next();
    			insertedRowId = rs.getInt(1);
    			rs.close();
    			stmt.close();
		    }
		    catch (SQLException e)
		    {
		        throw new DatabaseException("Failed to get the ID " +
		        		"of the last inserted row", e);
		    }
		    
			conditions = "ID = " + insertedRowId;

			captureValues(true, true);
		}
		else
		{
			captureValues(false, true);
		}
	}

	public void execute() throws DatabaseException 
    {
        long startTime = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer();
        
        if (type == INSERT) 
        {
            sb.append("INSERT INTO ").append(table);
        }
        else if (type == UPDATE)
        {
            sb.append("UPDATE ").append(table);
        }
        else if (type == DELETE)
        {
            sb.append("DELETE FROM ").append(table);
        }
        else
        {
            throw new AssertionError("type != INSERT && type != UPDATE "+
                    "&& type != DELETE");
        }
        
        if (type == INSERT || type == UPDATE) 
        {
            sb.append(" SET ");
            for (Iterator keys = fields.keySet().iterator(); keys.hasNext(); ) 
            {
                String key = (String)( keys.next() );
                Object value = fields.get(key);
                
                if (value instanceof Constant)
                {
                    sb.append(key+" = "+value);
                }
                else
                {
                    sb.append(key+" = ?");
                }
                
                if (keys.hasNext()) sb.append(", ");
            }
        }
        
        if ((type == UPDATE || type == DELETE) && conditions != null) {
            sb.append(" WHERE ").append(conditions);
        }
		
		try
		{
			boolean oldAutoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			
			PreparedStatement stmt = prepareAndLogError(
					"INSERT INTO change_log SET User = ?, Date_Time = NOW(), "+
					"DB_Type = 'SQL', DB_Name = ?, Table_Name = ?, Cmd_Type = ?");
			stmt.setString(1, username);
			stmt.setString(2, database);
			stmt.setString(3, table);
			stmt.setString(4, type.toString());
			stmt.executeUpdate();
			stmt.close();
			
			stmt = prepareAndLogError("SELECT LAST_INSERT_ID()");
			ResultSet rs = executeQueryAndLogError(stmt);
			rs.next();
			this.id = rs.getInt(1);
			rs.close();
			stmt.close();
			
			if (type == UPDATE || type == DELETE)
			{
				captureOldValues();
			}
			
			stmt = prepareAndLogError(sb.toString());
			
			if (type == INSERT || type == UPDATE)
			{
				int i = 0;
				for (Iterator keys = fields.keySet().iterator(); keys.hasNext(); ) 
                {
					String key = (String)( keys.next() );
					Object value = fields.get(key);
                    
                    if (value instanceof String)
                    {
                        stmt.setString(++i, (String)value);
                    }
                    else if (value == null)
                    {
                        stmt.setNull(++i, Types.VARCHAR);
                    }
				}
			}
			
			stmt.executeUpdate();
			// System.out.println(stmt.toString());
			stmt.close();
			
			if (type == INSERT || type == UPDATE) {
				captureNewValues();
			}
			
			conn.commit();
			conn.setAutoCommit(oldAutoCommit);
		} 
        catch (SQLException e) 
        {
            m_LOG.error(sb.toString(), e);
            DatabaseException de = new DatabaseException("Failed to execute " +
                "the requested operation. Please check your field names " +
                "and values", e, sb.toString());
            de.setStackTrace(e.getStackTrace());
			throw de;
		}
        
        long totalTime = System.currentTimeMillis() - startTime;
        m_LOG.info(totalTime+" ms to track "+sb.toString());
	}
	
	public void setInt(String column, long value)
	{
		fields.put(column, Long.toString(value));
	}

	public void setString(String column, String value)
	{
		fields.put(column, value);
	}

	public void setConstant(String column, String value) 
    {
        throw new UnsupportedOperationException("Not supported yet");
        // fields.put(column, new Constant(value));
	}

	public int getId()
	{
		return this.id;
	}
	
	public int getInsertedRowId()
	{
		if (type != INSERT)
		{
			throw new IllegalStateException(
				"Only INSERT changes have an inserted ID");
		}
		return insertedRowId;
	}
}

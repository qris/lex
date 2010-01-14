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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.mysql.jdbc.MysqlDataTruncation;
import com.qwirx.db.Change;
import com.qwirx.db.ChangeType;
import com.qwirx.db.ChangedRow;
import com.qwirx.db.ChangedValue;
import com.qwirx.db.DatabaseException;

/**
 * Represents (models) a change to one or more rows of a single table.
 * The change is created by SqlDatabase.createChange, which fixes the
 * change type (INSERT, UPDATE or DELETE) table name and conditions.
 * The SqlChange object is updated with column values to change,
 * and then execute()d. 
 * @author chris
 */
public final class SqlChange implements Change
{
	private Type   type;
	private Map<String, Object> fields;
	private String username, database, table, conditions;
	private Connection conn;
	private Integer id;
	private int    insertedRowId = -1;
    private static final Logger m_LOG = Logger.getLogger(SqlChange.class);
    private String m_PrimaryKeyField = "ID";
    private String changeQuery = null;
	
	public SqlChange(String username, String database, 
			Type type, String table, String conditions,
			Connection conn) 
	{
		this.username   = username;
		this.database   = database;
		this.type       = type;
		this.table      = table;
		this.conditions = conditions;
		this.fields     = new HashMap<String, Object>();
		this.conn       = conn;
	}

	public static SqlChange load(int id, Connection conn, String username)
	throws SQLException
	{
		PreparedStatement stmt = conn.prepareStatement("SELECT " +
			"DB_Name, Cmd_Type, Table_Name FROM change_log " +
			"WHERE id = " + id);
		ResultSet rs = stmt.executeQuery();
		if (!rs.next())
		{
			throw new IllegalArgumentException("No such changelog entry: " + 
				id);
		}
		String databaseName = rs.getString(1);
		String commandType  = rs.getString(2);
		String tableName    = rs.getString(3);
		SqlChange ch = new SqlChange(username, databaseName,
			lookup(commandType), tableName, null, conn);
		ch.id = id;
		return ch;
	}
	
	final static class Type implements ChangeType
	{
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
	
	public static final Type lookup(String typeName)
	{
		if (typeName.equals("INSERT"))
		{
			return INSERT;
		}
		if (typeName.equals("UPDATE"))
		{
			return UPDATE;
		}
		if (typeName.equals("DELETE"))
		{
			return DELETE;
		}
		
		throw new IllegalArgumentException("Unknown change type " + typeName);
	}
	
	public ChangeType getType() { return type; }
	
	private int insertRowChangeLog(int originalRowId) 
	throws DatabaseException
	{
        int logRowEntryId;
        
        try
        {
    		PreparedStatement stmt = prepareAndLogError(
    				"INSERT INTO changed_rows SET Log_ID = ?, " +
    				"Unique_ID = ?");
    		stmt.setInt(1, this.id.intValue());
    		stmt.setInt(2, originalRowId);
    		stmt.executeUpdate();
    		stmt.close();
    
    		stmt = prepareAndLogError(
    			"SELECT LAST_INSERT_ID()");
    		ResultSet rs = executeQueryAndLogError(stmt);
    		rs.next();
    		logRowEntryId = rs.getInt(1);
    		rs.close();
    		stmt.close();
        }
        catch (Exception e)
        {
            throw new DatabaseException("Failed to create changed_rows",
                e);
        }

		return logRowEntryId;
	}			

	private int findRowChangeLog(int originalRowId) 
	throws DatabaseException
	{
        int logRowEntryId;

        try
        {
            PreparedStatement stmt = prepareAndLogError(
                "SELECT ID FROM changed_rows WHERE Log_ID = ? AND " +
                "Unique_ID = ?");
    		stmt.setInt(1, this.id.intValue());
    		stmt.setInt(2, originalRowId);
    		ResultSet rs = executeQueryAndLogError(stmt);
    		rs.next();
    		logRowEntryId = rs.getInt(1);
    		rs.close();
    		stmt.close();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to find changed_rows entry",
                e);
        }

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
		    if (storeAsNewValue)
		    {
	            throw new DatabaseException("Failed to capture new values " +
                    "of rows. You may not have an ID column in the table.", e,
                    sb.toString());
		    }
		    else
		    {
    		    throw new DatabaseException("Failed to capture old values " +
    		    		"of rows. You may have an error in your table name " +
                        "or conditions (" + conditions + ")", e, sb.toString());
		    }
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
		
		String destColName = storeAsNewValue ? "New_Value" : "Old_Value";

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
	        List<Object> changedRowIds = new ArrayList<Object>();
		
  			while (rs.next())
  			{
  			    int uniqueId = rs.getInt(m_PrimaryKeyField);
  			    changedRowIds.add(new Integer(uniqueId));

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
  			                curValue = "0000-00-00";
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

  			    // It's never safe to delete row logs when storing old values,
  			    // as either it's an UPDATE (where we don't yet know what the
  			    // new values will be, and we need the rows to exist) or a 
  			    // DELETE (where we want to know the original value, even
  			    // if it's NULL).
  			    
  			    if (storeAsNewValue)
  			    {
	  			    stmt = prepareAndLogError("DELETE FROM changed_values " +
	  			        "WHERE Row_ID = ? AND ((Old_Value = New_Value) OR " +
	  			        "(Old_Value IS NULL AND New_Value IS NULL))");
	  			    stmt.setInt(1, logRowEntryId);
	  			    stmt.executeUpdate();
  			    }
  			}

  			cvs.close();
  			rs.close();
  			stmt.close();
  			
  			// an UPDATE may change the found set, but we want to record
  			// the changes to all rows that were found by the UPDATE
  			// (and the preceding SELECT), so we have to change the
  			// conditions to ensure that we find the same rows again.
  			
  			StringBuffer newConditions = new StringBuffer();
  			
  			for (Iterator i = changedRowIds.iterator(); i.hasNext();)
  			{
  			    newConditions.append(m_PrimaryKeyField + " = " + i.next());
  			    if (i.hasNext())
  			    {
  			        newConditions.append(" OR ");
  			    }
  			}
  			
  			String newCond = newConditions.toString();
  			if (newCond.length() > 0)
  			{
  				this.conditions = newCond;
  			}
  			else if (type != SqlChange.DELETE)
  			{
  				throw new DatabaseException("No records created or updated", 
  					changeQuery);
  			}
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to store old values in " +
                "change tracking tables. Perhaps there is a problem " +
                "with the tables?", e);
        }
	}
	
	private PreparedStatement prepareAndLogError(String query) 
	throws DatabaseException
	{
		try
		{
			return conn.prepareStatement(query);
		}
		catch (SQLException e)
		{
			throw new DatabaseException("Failed to prepare query", e, query);
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
		{
			throw new AssertionError("this method can only be used "+
					"when the command type is UPDATE or DELETE");
		}
		
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
			if (fields.containsKey("ID"))
			{
				insertedRowId = Integer.parseInt(fields.get("ID").toString());
			}
			else
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
			}
			
			conditions = m_PrimaryKeyField + " = " + insertedRowId;

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
		
        boolean oldAutoCommit;
        
        try
        {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        }
        catch (SQLException e) 
        {
            throw new DatabaseException("Failed to enable transactions", e);
        }
    
        String tempQuery = "INSERT INTO change_log " +
            "SET User = ?, Date_Time = NOW(), DB_Type = 'SQL', " +
            "    DB_Name = ?, Table_Name = ?, Cmd_Type = ?"; 
        
        Map<String, String> tempParams = new HashMap<String, String>();
		tempParams.put("User", username);
		tempParams.put("DB_Name", database);
		tempParams.put("Table_Name", table);
		tempParams.put("Cmd_Type", type.toString());
        
		try
		{
            PreparedStatement stmt = prepareAndLogError(tempQuery);
			stmt.setString(1, username);
			stmt.setString(2, database);
			stmt.setString(3, table);
			stmt.setString(4, type.toString());
			stmt.executeUpdate();
			stmt.close();
        }
        catch (SQLException e) 
        {
        	if (e instanceof MysqlDataTruncation)
        	{
        		MysqlDataTruncation mdte = (MysqlDataTruncation)e;
        		/*
        		Pattern pat = Pattern.compile("Data (truncated)|(too long) " +
        				"for column '([^']*)' at row (\\d+) *");
        				*/
        		Pattern pat = Pattern.compile("Data .* for column '([^']*)'");
        		Matcher mat = pat.matcher(e.getMessage());
        		if (mat.find())
        		{
        			String column = mat.group(1);
                    throw new DatabaseException("Failed to create change_log " +
                    		"entry: data truncation for '" + 
                    		tempParams.get(column) + "'", e, tempQuery);
        		}
        	}
            throw new DatabaseException("Failed to create change_log entry", 
                e, tempQuery);
        }

        try
        {
            PreparedStatement stmt = prepareAndLogError("SELECT LAST_INSERT_ID()");
			ResultSet rs = executeQueryAndLogError(stmt);
			rs.next();
			this.id = new Integer(rs.getInt(1));
			rs.close();
			stmt.close();
        }
        catch (SQLException e) 
        {
            throw new DatabaseException("Failed to get last insert ID", e);
        }

		if (type == UPDATE || type == DELETE)
		{
			captureOldValues();
		}
		
        try
        {
        	changeQuery = sb.toString();
    		PreparedStatement stmt = prepareAndLogError(changeQuery);
    			
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
                    else
                    {
                    	stmt.setObject(++i, value);
                    }
    			}
    		}
    		
    		stmt.executeUpdate();
    		// System.out.println(stmt.toString());
    		stmt.close();
        }
        catch (SQLException e) 
        {
        	changeQuery = null;
            throw new DatabaseException("Failed to execute " +
                "the requested operation. Please check your field names " +
                "and values", e, sb.toString());
        }

		if (type == INSERT || type == UPDATE)
		{
			captureNewValues();
		}

    	changeQuery = null;

        try
        {
    		conn.commit();
    		conn.setAutoCommit(oldAutoCommit);
        }
        catch (SQLException e) 
        {
            throw new DatabaseException("Failed to restore transaction state", 
                e);
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
	
	public void setDate(String column, java.util.Date date)
	{
		long time = date.getTime();
		java.sql.Timestamp d = new java.sql.Timestamp(time);
		fields.put(column, d);
	}

	public void setObject(String column, Object value)
	{
		fields.put(column, value);
	}

	public void setConstant(String column, String value) 
    {
        throw new UnsupportedOperationException("Not supported yet");
        // fields.put(column, new Constant(value));
	}

	/**
	 * Returns the ID of the change_log record that represents this change.
	 */
	public Integer getId()
	{
		return this.id;
	}
	
	/**
	 * Returns the unique ID (normally the ID column) of the row inserted
	 * using a SQL INSERT command. Normally this ID is auto-numbered and
	 * cannot be known until after the INSERT is completed. 
	 * @return
	 */
	public int getInsertedRowId()
	{
		if (type != INSERT)
		{
			throw new IllegalStateException(
				"Only INSERT changes have an inserted ID");
		}
		return insertedRowId;
	}
	
	/**
	 * Returns the conditions with which this SqlChange was created,
	 * or after an INSERT or UPDATE is executed, the conditions which
	 * match the changed rows.  
	 */
	public Object getConditions()
	{
		return conditions;
	}
	
	/**
	 * Returns a List of ChangedRow objects, one per row changed in the
	 * destination table, which describes the changes made. Can only be
	 * called after execute().
	 * @return
	 */
	public List getChangedRows() throws DatabaseException
	{
		String query =
			"SELECT Unique_ID, Row_ID, Col_Name, Old_Value, " +
			"       New_Value " +
			"FROM   changed_rows " +
			"STRAIGHT_JOIN changed_values " +
			"  ON   changed_values.Row_ID = changed_rows.ID " +
			"WHERE  changed_rows.Log_ID = " + id;
		
		try
		{
			PreparedStatement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery();
			
			int oldRowId = -1;
			ChangedRow cr = null;
			List<ChangedRow> results = new ArrayList<ChangedRow>();
			
			while (rs.next())
			{
				int rowId = rs.getInt("Row_ID");
				
				if (rowId != oldRowId)
				{
					cr = new ChangedRow(rs.getInt("Unique_ID"));
					results.add(cr);
					oldRowId = rowId;
				}
				
				String colName = rs.getString("Col_Name");
				String oldValue = rs.getString("Old_Value");
				String newValue = rs.getString("New_Value");
				
				cr.put(new ChangedValue(colName, oldValue, newValue));
			}
			
			return results;
		}
		catch (SQLException e)
		{
			throw new DatabaseException("Failed to get the list of " +
					"changed rows", e, query);
		}
	}
	
	/**
	 * Returns a List of new SqlChanges which, if executed, would undo
	 * the changes made by this one. If the same SqlChange is executed
	 * multiple times, then the reverse change will undo only the
	 * last one.
	 * 
	 * One SqlChange may require many to reverse it, as it may affect many
	 * rows in different ways, for example replacing a number of different
	 * values of a particular column with the same value, which cannot
	 * easily be undone with a single SQL statement.
	 * 
	 * @return the reverse SqlChange
	 * @throws UnsupportedOperationException if the change cannot be
	 * reversed (because the code hasn't been written yet)
	 */
	public SqlChange[] reverse() throws DatabaseException
	{
		if (type == SqlChange.INSERT || 
			type == SqlChange.UPDATE || 
			type == SqlChange.DELETE)
		{
			List changedRows = getChangedRows();
			List<SqlChange> reverseChanges = new ArrayList<SqlChange>();
			
			for (Iterator i = changedRows.iterator(); i.hasNext();)
			{
				ChangedRow cr = (ChangedRow)i.next();
				SqlChange rev = null;
				
				if (type == SqlChange.INSERT)
				{
					rev = new SqlChange(username, database, SqlChange.DELETE,
						table, "ID = " + cr.getUniqueID(), conn);
				}
				else if (type == SqlChange.UPDATE)
				{
					rev = new SqlChange(username, database, SqlChange.UPDATE,
						table, "ID = " + cr.getUniqueID(), conn);
				}
				else if (type == SqlChange.DELETE)
				{
					rev = new SqlChange(username, database, SqlChange.INSERT,
							table, null, conn);
				}

				if (rev.getType() != SqlChange.DELETE)
				{
					for (Iterator j = cr.iterator(); j.hasNext();)
					{
						ChangedValue cv = (ChangedValue)j.next();
						rev.setObject(cv.getName(), cv.getOldValue());
					}
				}
				
				reverseChanges.add(rev);
			}
			
			SqlChange [] revs = new SqlChange [reverseChanges.size()];
			return (SqlChange[])reverseChanges.toArray(revs);
		}
		else if (type == SqlChange.DELETE)
		{
			
		}
		throw new UnsupportedOperationException(type + 
				" cannot be reversed yet");	
	}
	
	/**
	 * Executes the reverse SqlChanges to undo the effects of executing()
	 * this change.
	 */
	public void undo() throws DatabaseException
	{
		SqlChange [] revs = reverse();
		for (int i = 0; i < revs.length; i++)
		{
			revs[i].execute();
		}
	}
}

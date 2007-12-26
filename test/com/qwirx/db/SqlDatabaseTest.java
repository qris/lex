/*
 * Created on 27-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;

/**
 * @author chris
 *
 * Tests the SqlDatabase and SqlChange classes, specifically change tracking.
 */
public class SqlDatabaseTest extends TestCase
{
	private SqlDatabase db;
    private static final String dsn = 
    	"jdbc:mysql://localhost:3306/test?user=test";
	
    public void testDatabasePreparedStatements() throws Exception
    {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        // + "&useServerPrepStmts=false";
        Connection conn = DriverManager.getConnection(dsn);

        PreparedStatement stmt = conn.prepareStatement("SELECT ?");
        stmt.setString(1, "foo");

        ResultSet rs = stmt.executeQuery();
        rs.next();
        assertEquals("foo", rs.getString(1));
        rs.close();
        
        stmt.close();
    }
    
	public SqlDatabaseTest()
	throws DatabaseException, SQLException, IllegalAccessException,
	InstantiationException, ClassNotFoundException
	{
		Class.forName("com.mysql.jdbc.Driver").newInstance();
    	Connection conn = DriverManager.getConnection(dsn);
		db = new SqlDatabase(conn, "test", "test");
	}
	
	public void setUp() throws Exception
	{
		try
		{
			db.executeDirect("DROP TABLE logtest");
		}
		catch (DatabaseException e)
		{
			// ignore errors, we don't mind if table doesn't exist yet
		}
		
		db.executeDirect("CREATE TABLE logtest (" +
				"ID int4 not null auto_increment primary key, " +
				"t_int int4," +
				"t_str varchar(40)," +
				"t_txt mediumtext," + 
				"t_flt decimal(10,2)," + // not supported by JDBC?
				"t_dat date, "+
				"t_dtm datetime, "+
                "t_dat2 DATE NOT NULL"+
				")");
	}		

	public void tearDown() throws Exception 
	{
		db.executeDirect("DROP TABLE logtest");
	}
	
	private String changeType = null;
	private int    logRecordId;
	
	private void getChangeTypeAndId(int xaId) 
	throws DatabaseException, SQLException
	{
		PreparedStatement stmt = db.prepareSelect(
			"SELECT ID, Cmd_Type FROM change_log WHERE ID = ?");
		stmt.setInt(1, xaId);
		ResultSet rs = db.select();
		rs.next();
		logRecordId = rs.getInt(1); 
		changeType  = rs.getString(2);
		db.finish();
	}
	
	private void assertCurrentValues(String query, ChangedRow cr) 
	throws DatabaseException, SQLException
	{
		db.prepareSelect(query);
		ResultSet rs = db.select();
		rs.next();

		for (Iterator i = cr.iterator(); i.hasNext();)
		{
			Object n = i.next();
			assertEquals(ChangedValue.class, n.getClass());
			ChangedValue cv = (ChangedValue)n;
			String colName  = cv.getName();
			String curValue = null;
            
			try
            {
                curValue = rs.getString(colName);
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

            String expValue = cv.getNewValue();
			assertEquals("Column " + colName + " has wrong value",
			    expValue, curValue);
		}

		db.finish();
	}
	
	private void assertChangeLogUpdated(int rowLogId, ChangedRow cr) 
	throws DatabaseException, SQLException
	{
		List changedValues = cr.getValues();
		
		db.prepareSelect(
				"SELECT ID, Col_Name, Old_Value, " +
				"New_Value FROM changed_values WHERE Row_ID = " + rowLogId);
		
		ResultSet rs = db.select();
		while (rs.next())
		{
			String colName  = rs.getString(2);
			ChangedValue cv = cr.get(colName);
			assertNotNull("Unknown row added to log: " + colName + 
			    " changed from " + rs.getString(3) +
				" to " + rs.getString(4), cv);
			
			{
				String expOldValue = cv.getOldValue();
				String curOldValue = rs.getString(3);
				boolean wasNull = rs.wasNull();
				if (expOldValue == null)
				{
					assertTrue("Old value was not NULL but " + curOldValue + 
							" in changed_values row "+rs.getInt(1),
							wasNull);
				}
				else
				{
					assertEquals("Wrong old value logged for "+colName, 
							expOldValue, curOldValue);
				}
			}
			
			{
				String expNewValue = cv.getNewValue();
				String curNewValue = rs.getString(4);
				boolean wasNull = rs.wasNull();
				if (expNewValue == null) {
					assertTrue("New value was not NULL " +
							"in changed_values row "+rs.getInt(1),
							wasNull);
				} else {
					assertEquals("Wrong new value logged for "+colName, 
							expNewValue, curNewValue);
				}
			}
			
			rs.getString(3);
			
			changedValues.remove(cv);
		}
		
		db.finish();

		assertEquals("Expected column change logs not found: " + 
				changedValues.toString(), 0, changedValues.size());
	}
	
	private SqlChange insertTestRecord() throws DatabaseException, SQLException
	{
		SqlChange ch = (SqlChange)db.createChange(SqlChange.INSERT, 
				"logtest", null);
		ch.setString("t_int", "1234");
		ch.setString("t_str", "Hello World");
		ch.setString("t_txt", "A somewhat longer string");
		ch.setString("t_flt", "3.14159"); // will be truncated
		ch.setString("t_dat", "1979-01-07"); // happy birthday
		ch.setString("t_dtm", "1979-01-07 06:35:00");
        ch.setString("t_dat2", "0000-00-00"); // crashes some versions
        // of mysql jdbc when getting column values
		ch.execute();
		getChangeTypeAndId(ch.getId().intValue());
		return ch;
	}
	
	private void assertEquals(ChangedRow rowA, ChangedRow rowB)
	{
		List fieldsA = rowA.getColumns();
		
		for (Iterator i = rowB.getColumns().iterator(); i.hasNext();)
		{
			String colName = (String)i.next();
			String valA = rowA.get(colName).getOldValue();
			String valB = rowB.get(colName).getOldValue();
			if (valA != null && valB == null)
			{
				assertEquals(colName, valA, "null");
			}
			assertEquals(colName, valA, valB);

			valA = rowA.get(colName).getNewValue();
			valB = rowB.get(colName).getNewValue();
			if (valA != null && valB == null)
			{
				assertEquals(colName, valA, "null");
			}
			assertEquals(colName, valA, valB);
			
			fieldsA.remove(colName);
		}
		
		assertEquals(fieldsA.toString(), 0, fieldsA.size());
	}
	
	public void testInsert() throws Exception 
	{
		SqlChange insert = insertTestRecord();
		assertEquals("INSERT", changeType);
		
		PreparedStatement stmt = db.prepareSelect(
				"SELECT ID FROM changed_rows " +
				"WHERE Log_ID = ? AND Unique_ID = ?");
		stmt.setInt(1, logRecordId);
		stmt.setInt(2, 1); // unique ID of only row in "logtest" table.
		ResultSet rs = db.select();
		rs.next();
		int rowLogId = rs.getInt(1);
		if (rs.next())
		{
			throw new AssertionError("too many rows added to changed_rows table");
		}
		db.finish();
		
		ChangedRow expectedRowChange = new ChangedRow(new ChangedValue[]{
			new ChangedValue("ID",     null, "1"),
			new ChangedValue("t_int",  null, "1234"),
			new ChangedValue("t_str",  null, "Hello World"),
			new ChangedValue("t_txt",  null, "A somewhat longer string"),
	        new ChangedValue("t_flt",  null, "3.14"),
			new ChangedValue("t_dat",  null, "1979-01-07"), // happy birthday
			new ChangedValue("t_dtm",  null, "1979-01-07 06:35:00.0"),
	        new ChangedValue("t_dat2", null, "0000-00-00"),
		});
		
		assertCurrentValues("SELECT * FROM logtest", expectedRowChange);
		assertChangeLogUpdated(rowLogId, expectedRowChange);
		
		List rows = insert.getChangedRows();
		assertEquals(1, rows.size());
		expectedRowChange = (ChangedRow)rows.get(0);
		assertChangeLogUpdated(rowLogId, expectedRowChange);
		
		SqlChange delete = insert.reverse();
		assertEquals(SqlChange.DELETE, delete.getType());
		assertEquals("ID = 1", delete.getConditions());
		delete.execute();
		rows = delete.getChangedRows();
		assertEquals(1, rows.size());
		ChangedRow actualReverseRowChange = (ChangedRow)rows.get(0);
		
		ChangedRow expectedReverseRowChange = new ChangedRow(
				new ChangedValue[]{
			new ChangedValue("ID",     "1", null),
			new ChangedValue("t_int",  "1234", null),
			new ChangedValue("t_str",  "Hello World", null),
			new ChangedValue("t_txt",  "A somewhat longer string", null),
	        new ChangedValue("t_flt",  "3.14", null),
			new ChangedValue("t_dat",  "1979-01-07", null), // happy birthday
			new ChangedValue("t_dtm",  "1979-01-07 06:35:00.0", null),
	        new ChangedValue("t_dat2", "0000-00-00", null),
		});
		assertEquals(expectedReverseRowChange, expectedRowChange.reverse());
        assertEquals(expectedReverseRowChange, actualReverseRowChange);
        assertEquals(0, db.getSingleInteger("SELECT COUNT(1) FROM logtest"));
	}
	
	public void testUpdate() throws Exception
	{
		SqlChange testInsert = insertTestRecord();
		int testId = testInsert.getInsertedRowId();

		Change testUpdate = db.createChange(SqlChange.UPDATE, 
				"logtest", "t_int = 1234");
		testUpdate.setInt   ("t_int", 23456);
		testUpdate.setString("t_str", "Hello Again");
		testUpdate.setString("t_txt", "Another longer string");
		testUpdate.setString("t_flt", "2.81718"); // will be truncated
		testUpdate.setString("t_dat", "1980-10-15"); // happy birthday
		testUpdate.setString("t_dtm", "1980-10-15 12:34:56.0");
		testUpdate.execute();
		
		getChangeTypeAndId(testUpdate.getId().intValue());
		assertEquals("UPDATE", changeType);

		PreparedStatement stmt = db.prepareSelect(
				"SELECT ID FROM changed_rows " +
				"WHERE Log_ID = ? AND Unique_ID = ?");
		stmt.setInt(1, logRecordId);
		stmt.setInt(2, testId);
		ResultSet rs = db.select();
		
		int rowLogId;
		
		try
		{
			rs.next();
			rowLogId = rs.getInt(1);
		}
		catch (SQLException e) 
		{
			System.err.println(stmt.toString());
			throw(e);
		}
		
		assertFalse("too many rows added to changed_rows table", rs.next());
		db.finish();

		ChangedRow expectedRowChange = new ChangedRow();
		expectedRowChange.put(new ChangedValue("t_int", 
				"1234",                     
				"23456"));
		expectedRowChange.put(new ChangedValue("t_str", 
				"Hello World",
				"Hello Again"));
		expectedRowChange.put(new ChangedValue("t_txt", 
				"A somewhat longer string",
				"Another longer string"));
        expectedRowChange.put(new ChangedValue("t_flt", "3.14", "2.82"));
		expectedRowChange.put(new ChangedValue("t_dat", 
				"1979-01-07", "1980-10-15"));
		expectedRowChange.put(new ChangedValue("t_dtm", 
				"1979-01-07 06:35:00.0", "1980-10-15 12:34:56.0"));

		assertCurrentValues("SELECT * FROM logtest", expectedRowChange);
		assertChangeLogUpdated(rowLogId, expectedRowChange);
	}

	public void testDelete() throws Exception
	{
		insertTestRecord();

		Change ch = db.createChange(SqlChange.DELETE, 
				"logtest", null);
		ch.execute();
		
		getChangeTypeAndId(ch.getId().intValue());
		assertEquals("DELETE", changeType);

		PreparedStatement stmt = db.prepareSelect(
				"SELECT ID FROM changed_rows " +
				"WHERE Log_ID = ? AND Unique_ID = ?");
		stmt.setInt(1, logRecordId);
		stmt.setInt(2, 1); // unique ID of only row in "logtest" table.
		ResultSet rs = db.select();
		
		int rowLogId;
		
		try
		{
			rs.next();
			rowLogId = rs.getInt(1);
		}
		catch (SQLException e)
		{
			System.err.println(stmt.toString());
			throw(e);
		}
		
		if (rs.next())
		{
			throw new AssertionError("too many rows added to changed_rows table");
		}
		db.finish();

		ChangedRow cr = new ChangedRow();
		cr.put(new ChangedValue("ID",     "1",	                      null));
		cr.put(new ChangedValue("t_int",  "1234",	                  null));
		cr.put(new ChangedValue("t_str",  "Hello World",              null));
		cr.put(new ChangedValue("t_txt",  "A somewhat longer string", null));
        cr.put(new ChangedValue("t_flt",  "3.14",                     null));
		cr.put(new ChangedValue("t_dat",  "1979-01-07",               null));
		cr.put(new ChangedValue("t_dtm",  "1979-01-07 06:35:00.0",    null));
        cr.put(new ChangedValue("t_dat2", "0000-00-00",               null));

		stmt = db.prepareSelect("SELECT * FROM logtest");
		rs = stmt.executeQuery();
		assertFalse("Row not deleted as expected", rs.next());
		db.finish();
		
		assertChangeLogUpdated(rowLogId, cr);
	}

	public void testUpdateWhichChangesFoundSet() throws Exception
	{
	    insertTestRecord();

	    Change ch = db.createChange(SqlChange.UPDATE, 
	        "logtest", null);
	    ch.setInt   ("t_int", 23456);
	    ch.setString("t_str", "Hello Again");
	    ch.setString("t_txt", "Another longer string");
	    ch.setString("t_flt", "2.81718"); // will be truncated
	    ch.setString("t_dat", "1980-10-15"); // happy birthday
	    ch.setString("t_dtm", "1980-10-15 12:34:56.0");
	    ch.execute();

	    getChangeTypeAndId(ch.getId().intValue());
	    assertEquals("UPDATE", changeType);

	    PreparedStatement stmt = db.prepareSelect(
	        "SELECT ID FROM changed_rows " +
	    "WHERE Log_ID = ? AND Unique_ID = ?");
	    stmt.setInt(1, logRecordId);
	    stmt.setInt(2, 1); // unique ID of only row in "logtest" table.
	    ResultSet rs = db.select();

	    int rowLogId;

	    try
	    {
	        rs.next();
	        rowLogId = rs.getInt(1);
	    }
	    catch (SQLException e) 
	    {
	        System.err.println(stmt.toString());
	        throw(e);
	    }

	    assertFalse("too many rows added to changed_rows table", rs.next());
	    db.finish();

	    ChangedRow cr = new ChangedRow();
	    cr.put(new ChangedValue("t_int", 
	        "1234",                     
	    "23456"));
	    cr.put(new ChangedValue("t_str", 
	        "Hello World",
	    "Hello Again"));
	    cr.put(new ChangedValue("t_txt", 
	        "A somewhat longer string",
	    "Another longer string"));
	    cr.put(new ChangedValue("t_flt", "3.14", "2.82"));
	    cr.put(new ChangedValue("t_dat", 
	        "1979-01-07", "1980-10-15"));
	    cr.put(new ChangedValue("t_dtm", 
	        "1979-01-07 06:35:00.0", "1980-10-15 12:34:56.0"));

	    assertCurrentValues("SELECT * FROM logtest", cr);
	    assertChangeLogUpdated(rowLogId, cr);
	}
	
	public void testConnectorCannotReadZeroDateAsString() throws Exception
	{
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        Connection conn = DriverManager.getConnection(dsn + 
    		"zeroDateTimeBehaviour=convertToNull" +
    		"&noDatetimeStringSync=true", "test", "");
        boolean threwException = false;
        
        try
        {
            PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE temp (ID int4 not null auto_increment primary key, " +
                "tm date not null)");
            stmt.execute();
            stmt = conn.prepareStatement("INSERT INTO temp " +
            		"SET tm = \"0000-00-00\"");
            stmt.execute();
            stmt = conn.prepareStatement("SELECT tm FROM temp");
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals("0000-00-00", rs.getString(1));
            rs.close();
            stmt.close();
        }
        catch (SQLException e)
        {
            if (e.getMessage().equals("Value '0000-00-00' " +
                "can not be represented as java.sql.Date"))
            {
                threwException = true;
            }
        }
        finally
        {
            PreparedStatement stmt = conn.prepareStatement("DROP TABLE temp");
            stmt.execute();
        }
        
        assertFalse(threwException);
        
        conn.close();
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(SqlDatabaseTest.class);
	}

}


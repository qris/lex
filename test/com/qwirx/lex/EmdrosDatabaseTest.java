/*
 * Created on 27-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import jemdros.EmdrosException;
import jemdros.SetOfMonads;
import jemdros.Table;
import jemdros.TableIterator;
import jemdros.TableRow;
import jemdros.TableRowIterator;
import junit.framework.TestCase;

import com.qwirx.db.Change;
import com.qwirx.db.ChangedRow;
import com.qwirx.db.ChangedValue;
import com.qwirx.db.ChangedValueString;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosChange;
import com.qwirx.lex.emdros.EmdrosDatabase;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EmdrosDatabaseTest extends TestCase 
{
	private final SqlDatabase    logDb;
	private final EmdrosDatabase emdros;
    
    public void setUp() throws Exception
    {
        if (logDb.getSingleInteger("SELECT COUNT(1) FROM user_text_access " +
            "WHERE User_Name = \"test\" AND Monad_First = 1000000 " +
            "AND Monad_Last = 1000005 AND Write_Access = 1") == 0)
        {
            Change ch = logDb.createChange(SqlChange.INSERT,
                "user_text_access", null);
            ch.setString("User_Name", "test");
            ch.setInt("Monad_First", 1000000);
            ch.setInt("Monad_Last",  1000005);
            ch.setInt("Write_Access", 1);
            ch.execute();
        }
    }
	
	public EmdrosDatabaseTest() throws Exception 
    {
		logDb  = Lex.getSqlDatabase("test");
		emdros = Lex.getEmdrosDatabase("test", "localhost", logDb);
	}
	
	/* Crashes Emdros
	public void testRetrieveColumnZeroShouldNotCrash() 
	throws DatabaseException {
		Table t = emdros.getTable(
				"GET FEATURES argument_name FROM OBJECT WITH ID_D = 69977 [phrase]");
		TableRow tr = t.iterator().next();
		String value = tr.getColumn(0);
	}
	*/
	
	public void testSimpleQueryShouldWork() 
	throws DatabaseException
    {
		emdros.getTable("SELECT OBJECTS AT MONAD = 1 [Word] GO");
		emdros.getTable("SELECT OBJECTS HAVING MONADS IN {1-10} [ALL] GO");
		emdros.getSheaf("SELECT ALL OBJECTS IN {1-10} WHERE [Word] GO");
	}
	
	public void testTableIteratorShouldWork()
	throws Exception
    {
		Table table = emdros.getTable("SELECT OBJECTS HAVING MONADS " +
				"IN {1-10} [ALL] GO");
		
		// Print table
		int count = table.size();
		TableIterator ti = table.iterator();
		
		while (ti.hasNext())
		{
		    // Get iterator for current row
		    TableRowIterator tri = ti.next().iterator();
		    count--;
		    
		    while (tri.hasNext())
		    {
		        tri.next();
		    }
		}
		
		assertTrue(count == 0);
	}

	public void testWordShouldHaveMoreThanOneFeature()
	throws DatabaseException
    {
		Table table = emdros.getTable("SELECT FEATURES FROM [word] GO");
		assertTrue("Not enough features in object type 'word'",
				table.size() > 1);
		/*
		TableIterator ti = table.iterator();
		while (ti.hasNext()) {
			TableRow tr = ti.next();
			System.out.println(tr.iterator().next());
		}
		*/
	}

	public void testWordShouldStillHaveFeaturesAfterQueries()
	throws Exception 
	{
		emdros.getSheaf("SELECT ALL OBJECTS IN {1-28735} "+
				"WHERE [verse "+
				"       book    = Genesis AND "+
				"       chapter = 2 AND "+
				"       verse   = 4 "+
				"       [clause [word get lexeme]]]");
		
		Table table = emdros.getTable("SELECT FEATURES FROM [word] GO");
		assertTrue("Not enough features in object type 'word'",
				table.size() > 1);
		
		TableIterator ti = table.iterator();
		while (ti.hasNext()) {
			TableRow tr = ti.next();
			System.out.println(tr.iterator().next());
		}
	}

    /*
	private String changeType = null;
	private int    logRecordId;
	
	public void getChangeTypeAndId(int xaId) 
	throws DatabaseException, SQLException {
		PreparedStatement stmt = logDb.prepareSelect(
			"SELECT ID, Cmd_Type FROM change_log WHERE ID = ?");
		stmt.setInt(1, xaId);
		ResultSet rs = logDb.select();
		rs.next();
		logRecordId = rs.getInt(1); 
		changeType  = rs.getString(2);
		logDb.finish();
	}
    */
	
	public void assertCurrentValues(int objectID_D, String objectType, 
			ChangedRow cr) 
	throws DatabaseException, EmdrosException
	{
		StringBuffer query = new StringBuffer("GET FEATURES ");
		
		for (Iterator i = cr.iterator(); i.hasNext(); ) {
			ChangedValue cv = (ChangedValue)( i.next() );
			query.append(cv.getName());
			if (i.hasNext()) query.append(",");
		}
		
		query.append(" FROM OBJECT WITH ID_D = "+objectID_D+" ["+objectType+"]");
		
		Table currentValues = emdros.getTable(query.toString());
		TableRow tr = currentValues.iterator().current();
		int i = 0;
		
		for (Iterator ci = cr.iterator(); ci.hasNext(); )
        {
			Object n = ci.next();
			// assertEquals(ChangedValue.class, n.getClass());
			ChangedValue cv = (ChangedValue)n;
			String colName  = cv.getName();
			String expValue = cv.getNewValue();
			String curValue = tr.getColumn(++i + 1);
			assertEquals("Column "+colName+" has wrong value", expValue, curValue);
		}

		logDb.finish();
	}
	
	public void assertChanges(int rowLogId, ChangedRow originalCr) 
	throws DatabaseException, SQLException
	{
        ChangedRow cr = new ChangedRow(originalCr);
        
		System.out.println("SELECT ID, Col_Name, Old_Value, " +
				"New_Value FROM changed_values WHERE Row_ID = " + rowLogId);
		
		logDb.prepareSelect(
				"SELECT ID, Col_Name, Old_Value, " +
				"New_Value FROM changed_values WHERE Row_ID = " + rowLogId);
		
		ResultSet rs = logDb.select();
		while (rs.next())
        {
			String colName  = rs.getString(2);
			ChangedValue cv = cr.get(colName);
			assertNotNull("Unknown row added to log: " 
					+ colName, cv);
			
			{
				String expOldValue = cv.getOldValue();
				String curOldValue = rs.getString(3);
				if (expOldValue == null) {
					assertTrue("Old value was not NULL " +
							"in changed_values row "+rs.getInt(1),
							rs.wasNull());
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
				if (expNewValue == null) {
					assertTrue("New value was not NULL " +
							"in changed_values row "+rs.getInt(1),
							rs.wasNull());
				} else {
					assertEquals("Wrong new value logged for "+colName, 
							expNewValue, curNewValue);
				}
			}
			
			rs.getString(3);
			
			cr.remove(colName);
		}
		logDb.finish();

		Iterator i = cr.iterator();
		if (i.hasNext()) 
        {
			ChangedValue cv = (ChangedValue)( i.next() );
			assertTrue("Expected column change log not found: "+ 
					cv.getName(), false);
		}
	}
	
	private int applyChanges(String objectType, ChangedRow cr, boolean byMonads) 
	throws DatabaseException
    {
		EmdrosChange ch;
        
        if (byMonads)
        {
            ch = (EmdrosChange)emdros.createChange(EmdrosChange.UPDATE,
                objectType, null);
            ch.setMonads(emdros.getObjectMonads(objectType, 
                new int[]{cr.getUniqueID()}));
        }
        else
        {
            ch = (EmdrosChange)emdros.createChange(EmdrosChange.UPDATE,
				objectType, new int[]{cr.getUniqueID()});
        }
        
		for (Iterator i = cr.iterator(); i.hasNext(); )
        {
			ChangedValue cv = (ChangedValue)i.next();
			if (cv instanceof ChangedValueString)
            {
				ch.setString(cv.getName(), cv.getNewValue());
            }
			else
            {
				ch.setConstant(cv.getName(), cv.getNewValue());
            }
		}
		ch.execute();
		return ch.getId().intValue();
	}
	
	private int getFirstChangedRowLogId(int logId) 
	throws DatabaseException, SQLException
    {
		logDb.prepareSelect(
				"SELECT ID FROM changed_rows WHERE Log_ID = "+logId);
		ResultSet rs = logDb.select();
		rs.next();
		int id = rs.getInt(1);
		logDb.finish();
		return id;
	}
	
	public void testUpdate() throws Exception
	{
        EmdrosChange ch = (EmdrosChange)emdros.createChange(
            EmdrosChange.CREATE, "phrase", null);
        ch.setMonads(new SetOfMonads(1000000, 1000005));
        ch.execute();
		int testPhraseId = ch.getInsertedRowId();

        try
        {
    		ChangedRow forward = new ChangedRow(testPhraseId);
    		forward.put(new ChangedValueString("argument_name", "x", "foobar"));
    		forward.put(new ChangedValue("determination", "determined", "indetermined"));
    		forward.put(new ChangedValue("phrase_function", "PreC", "Time"));
    		forward.put(new ChangedValue("is_apposition", "false", "true"));
    		forward.put(new ChangedValue("number_within_clause", "2", "4"));
    		forward.put(new ChangedValue("phrase_type", "NP", "VP"));
    		ChangedRow reverse = forward.reverse();
    		
    		applyChanges("phrase", reverse, false);
    		
    		// check current values of reversed change, before making changes,
    		// to ensure that the values now are the same as the ones we will
    		// replace later by reversing the change.
    		assertCurrentValues    (testPhraseId, "phrase", reverse);
    		
    		int fid = applyChanges("phrase", forward, false);
    		assertCurrentValues   (testPhraseId, "phrase", forward);
    		int flr = getFirstChangedRowLogId(fid);
    		assertChanges (flr, forward);
    		
    		int rid = applyChanges("phrase", reverse, false);
    		assertCurrentValues   (testPhraseId, "phrase", reverse);
    		int rlr = getFirstChangedRowLogId(rid);
    		assertChanges (rlr, reverse);
    
            // now do the same using update by monads
            fid = applyChanges ("phrase", forward, true);
            assertCurrentValues(testPhraseId, "phrase", forward);
            flr = getFirstChangedRowLogId(fid);
            assertChanges (flr, forward);
            
            rid = applyChanges ("phrase", reverse, true);
            assertCurrentValues(testPhraseId, "phrase", reverse);
            rlr = getFirstChangedRowLogId(rid);
            assertChanges (rlr, reverse);
        }
        finally
        {
            ch = (EmdrosChange)emdros.createChange(EmdrosChange.DELETE,
                "phrase", new int[]{testPhraseId});
            ch.execute();
        }
	}

	public static void main(String[] args)
    {
		junit.textui.TestRunner.run(EmdrosDatabaseTest.class);
	}

}


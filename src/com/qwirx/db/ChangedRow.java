/*
 * Created on 01-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ChangedRow
{
	private int m_UniqueID;
	private Map<String, ChangedValue> m_Values = 
		new Hashtable<String, ChangedValue>();

    public ChangedRow(int uniqueID)
    {
    	m_UniqueID = uniqueID;
    }

    public ChangedRow(int uniqueID, ChangedValue[] values)
    {
    	this(uniqueID);
    	
        for (int i = 0; i < values.length; i++)
        {
            m_Values.put(values[i].getName(), values[i]);
        }
    }

    public ChangedRow(ChangedRow other)
    {
    	this(other.m_UniqueID);
        m_Values = new Hashtable<String, ChangedValue>(other.m_Values);
    }
    
    public int getUniqueID() { return m_UniqueID; }

	/*
	public ChangedRow(Integer ID, Integer uniqueID, Integer logID)
	{
		m_ID = ID;
		m_UniqueID = uniqueID;
		m_LogID = logID;
	}
	*/
	
	public void put(ChangedValue change) 
	{
		m_Values.put(change.getName(), change);
	}
	
	public ChangedValue get(String columnName)
	{
		return m_Values.get(columnName);
	}
    
    public List<String> getColumns()
    {
        return new ArrayList<String>(m_Values.keySet());
    }
    
    public List<ChangedValue> getValues()
    {
        return new ArrayList<ChangedValue>(m_Values.values());
    }
	
	public Iterator iterator() {
		return m_Values.values().iterator();
	}
	
	public void remove(String columnName)
	{
		m_Values.remove(columnName);
	}
	
	public ChangedRow reverse()
	{
		ChangedRow reverse = new ChangedRow(m_UniqueID);
		for (Iterator i = iterator(); i.hasNext(); ) 
		{
			ChangedValue orig = (ChangedValue)( i.next() );
			reverse.put(orig.reverse());
		}
		return reverse;
	}
	
	public String toString()
	{
		return m_Values.values().toString();
	}

}

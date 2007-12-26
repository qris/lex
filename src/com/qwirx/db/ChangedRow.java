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
public class ChangedRow {
	private Map m_Values = new Hashtable();

    public ChangedRow() { }

    public ChangedRow(ChangedValue[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            m_Values.put(values[i].getName(), values[i]);
        }
    }
	
	public void put(ChangedValue change) {
		m_Values.put(change.getName(), change);
	}
	
	public ChangedValue get(String columnName) {
		return (ChangedValue)( m_Values.get(columnName) );
	}
    
    public List getColumns()
    {
        return new ArrayList(m_Values.keySet());
    }
    
    public List getValues()
    {
        return new ArrayList(m_Values.values());
    }
	
	public Iterator iterator() {
		return m_Values.values().iterator();
	}
	
	public void remove(String columnName) {
		m_Values.remove(columnName);
	}
	
	public ChangedRow reverse() {
		ChangedRow reverse = new ChangedRow();
		for (Iterator i = iterator(); i.hasNext(); ) {
			ChangedValue orig = (ChangedValue)( i.next() );
			reverse.put(orig.reverse());
		}
		return reverse;
	}
}

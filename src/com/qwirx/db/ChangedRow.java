/*
 * Created on 01-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ChangedRow {
	private Map values = new Hashtable();
	
	public void put(ChangedValue change) {
		values.put(change.getName(), change);
	}
	
	public ChangedValue get(String columnName) {
		return (ChangedValue)( values.get(columnName) );
	}
	
	public Iterator iterator() {
		return values.values().iterator();
	}
	
	public void remove(String columnName) {
		values.remove(columnName);
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

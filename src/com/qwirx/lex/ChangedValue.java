/*
 * Created on 01-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ChangedValue {
	private String name, oldValue, newValue;
	
	public ChangedValue(String columnName, String oldValue, String newValue) {
		this.name     = columnName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public String getName    () { return name; }
	public String getOldValue() { return oldValue; }
	public String getNewValue() { return newValue; }
	
	public ChangedValue reverse() {
		return new ChangedValue(name, newValue, oldValue);
	}
}

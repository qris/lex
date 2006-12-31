/*
 * Created on 04-Jan-2005
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
public class ChangedValueString extends ChangedValue {
	public ChangedValueString(String columnName, String oldValue, String newValue) {
		super(columnName, oldValue, newValue);
	}
	
	
	public ChangedValue reverse() {
		return new ChangedValueString(getName(), getNewValue(), getOldValue());
	}

}

/*
 * Created on 02-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db;


/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Database 
{
	void executeDirect(String query) throws DatabaseException;
	Change createChange(ChangeType type, String table, 
			Object conditions) throws DatabaseException;
}

/*
 * Created on 27-Dec-2004
 *
 * Represents and logs changes to database objects, Emdros or SQL.
 */
package com.qwirx.lex;

public interface Change 
{
	public void execute() throws DatabaseException;
	public void setInt     (String column, long   value);
	public void setString  (String column, String value);
	public void setConstant(String column, String value);
	public int  getId();
}
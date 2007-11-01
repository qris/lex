/**
 * 
 */
package com.qwirx.db.sql;

public class DbColumn 
{
	String name, type, defaultValue;
	boolean isPrimaryKey, isNullAllowed, isAutoNumbered;
	
	public DbColumn
    (
        String name, 
        String type, 
        boolean isNullAllowed,
        boolean isPrimaryKey, 
        boolean isAutoNumbered
    ) 
    {
		this(name, type, isNullAllowed);
		this.isPrimaryKey   = isPrimaryKey;
		this.isAutoNumbered = isAutoNumbered;
	}

	public DbColumn(String name, String type, boolean isNullAllowed) 
    {
		this.name           = name;
		this.type           = type;
		this.isNullAllowed  = isNullAllowed;
	}

	public String getSpec()
    {
    	String sql = name + " " + type;
    	
		if (!isNullAllowed)
			sql += " NOT NULL";
		if (defaultValue != null)
			sql += " DEFAULT "+defaultValue;
		if (isAutoNumbered)
			sql += " AUTO_INCREMENT";
		if (isPrimaryKey)
			sql += " PRIMARY KEY";

		return sql;
    }
}
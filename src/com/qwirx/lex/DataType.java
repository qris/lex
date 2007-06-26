/*
 * Created on 31-Jul-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;

public class DataType 
{
	public int id, supertype_id, depth;
	public String name;
	
	private static Connection m_sql;
	public static void setConnection(Connection sql) 
    {
		m_sql = sql;
	}
	
    public static DataType[] getByQuery(String query, Object [] args) 
    throws SQLException
    {
    	PreparedStatement stmt  = null;
    	ResultSet         rs    = null;
    	Vector            types = new Vector();
    	
    	try 
        {
    		stmt = m_sql.prepareStatement(
    				"SELECT ID, Name, Supertype_ID "+
					"FROM object_types " +
					"WHERE "+query);
    		
    		for (int i = 0; i < args.length; i++) 
            {
    			Object arg = args[i];
    			if (arg instanceof String) {
    				stmt.setString(i+1, (String)arg);
    			} else if (arg instanceof Integer) {
    				stmt.setInt(i+1, ((Integer)arg).intValue());
    			}
    		}

    		rs = stmt.executeQuery();
			
			if (!rs.next())
				return new DataType[0];
			
			DataType type     = new DataType();
			type.id           = rs.getInt   ("ID");
			type.supertype_id = rs.getInt   ("Supertype_ID");
			type.name         = rs.getString("Name");
			types.add(type);
    	} 
        finally 
        {
			Lex.cleanUpSql(stmt, rs);
    	}

    	DataType [] typeArray = new DataType[types.size()];
    	types.copyInto(typeArray);
    	return typeArray;
    }
    
    public static DataType get(String name) throws SQLException 
    {
        DataType [] types = getByQuery("Name = ?", new Object[]{name});
        if (types == null || types.length == 0)
            return null;
    	return types[0];
    }
    
    public static DataType get(int ID) 
    throws SQLException
    {
        DataType [] types = getByQuery("ID = ?", new Object[]{new Integer(ID)});
        if (types == null || types.length == 0)
            return null;
        return types[0];
    }
    
    public static DataType [] getCompatible(String rootTypeName) 
    throws SQLException
    {
    	Vector store = new Vector();
    	DataType root = get(rootTypeName);
        if (root == null) return null;
        
    	root.depth = 0; 
    	store.add(root);
    	getCompatible(root, store, new Hashtable());
    	DataType [] result = new DataType [store.size()];
    	store.copyInto(result);
    	return result;
    }

    public static void getCompatible(DataType root, Vector store, 
    		Hashtable typesByName) 
    throws SQLException
    {
    	DataType [] subtypes = getByQuery
			("Supertype_ID = ?", new Object[]{new Integer(root.id)});
    	if (subtypes == null) return;
    	for (int i = 0; i < subtypes.length; i++) {
    		subtypes[i].depth = root.depth + 1;
    		store.add(subtypes[i]);
    		getCompatible(subtypes[i], store, typesByName);
    	}
    }
    
    public static DataType [] getAll() 
    throws SQLException
    {
    	return getCompatible("object");
    }
}
/*
 * Created on 26-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.emdros;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import jemdros.EmdrosEnv;
import jemdros.MatchedObject;
import jemdros.MonadSetElement;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.Table;
import jemdros.TableIterator;
import jemdros.TableRow;

import org.apache.log4j.Logger;

import com.qwirx.lex.Change;
import com.qwirx.lex.Database;
import com.qwirx.lex.DatabaseException;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EmdrosDatabase implements Database 
{
	private EmdrosEnv env;
	private Connection conn;
	private String username, userhost, database;
    private static final Logger m_LOG = Logger.getLogger(EmdrosDatabase.class);
	
	public EmdrosDatabase(EmdrosEnv env, String username, String userhost,
	    String database, Connection conn) 
    {
		this.env = env;
		this.conn = conn;
		this.username = username;
        this.userhost = userhost;
		this.database = database;
	}
	
	public void executeDirect(String query) throws DatabaseException 
    {
        long startTime = System.currentTimeMillis();
        
		// Execute query
		boolean[] bCompilerResult = new boolean[1];

		boolean bDBResult = env.executeString(query, bCompilerResult, false, false);
	
		if (!bDBResult) {
			throw new DatabaseException(
					"Database error: "+env.getDBError(), query);
		}

		if (!bCompilerResult[0]) {
			throw new DatabaseException(
					"Compiler error: "+env.getCompilerError(), query);
		}
        
        long totalTime = System.currentTimeMillis() - startTime;
        m_LOG.info(totalTime+" ms to "+query);
	}

	public Sheaf getSheaf(String query) 
	throws DatabaseException 
    {
		executeDirect(query);
		
        if (! env.isSheaf()) 
        {
			throw new DatabaseException("result is not a sheaf", query);
		}
		
        return env.takeOverSheaf();
	}

	public Table getTable(String query) 
	throws DatabaseException 
    {
		executeDirect(query);
		
        if (! env.isTable()) 
        {
			throw new DatabaseException("result is not a table", query);
		}
		
        return env.takeOverTable();
	}
	
	public int getMinM() throws DatabaseException 
    {
		Table min_m_table = getTable("SELECT MIN_M");
		return Integer.parseInt(
			min_m_table.iterator().next().iterator().next()
			);
	}

	public int getMaxM() throws DatabaseException 
    {
		Table max_m_table = getTable("SELECT MAX_M");
		return Integer.parseInt(
			max_m_table.iterator().next().iterator().next()
			);
	}
	
	public Map getEnumerationConstants(String type, boolean byName) 
	throws DatabaseException 
    {
		Hashtable result = new Hashtable();
		
		Table table = getTable("SELECT ENUMERATION CONSTANTS FROM "+type);
		
		TableIterator rows = table.iterator();
		while (rows.hasNext()) {
			TableRow row = rows.next();
			
			String name   = row.getColumn(1);
			String number = row.getColumn(2);
			
			if (byName)
				result.put(name, number);
			else
				result.put(number, name);
		}
		
		return result;
	}
    
    public String getMonadSet(String query)
    throws DatabaseException
    {
        StringBuffer result = new StringBuffer();
        result.append("{");
        
        Table table = getTable("MONAD SET CALCULATION "+query);
        TableIterator rows = table.iterator();
        
        while (rows.hasNext()) 
        {
            TableRow row = rows.next();
            
            String min = row.getColumn(1);
            String max = row.getColumn(2);
            
            result.append(min+"-"+max);
            if (rows.hasNext())
            {
                result.append(",");
            }
        }
        
        result.append("}");
        return result.toString();
        
    }

    public String getMonadSet(String access, int min_m, int max_m)
    throws DatabaseException
    {
        return getMonadSet(access+" INTERSECT {"+min_m+"-"+max_m+"}");
    }
    
	public Change createChange(Object changeType, String objectType, 
			Object objectIds) throws DatabaseException
	{
        int [] id_ds = (int [])objectIds;
        
        try
        {
            if (!canWriteTo(objectType, id_ds))
            {
                throw new DatabaseException("You do not have permission "+
                    "to modify this object", changeType + " " + objectType +
                    " " + objectIds);
            }
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to determine object access",
                e, null);
        }
        
		return new EmdrosChange(username+"@"+userhost, database, 
			(EmdrosChange.Type)changeType, objectType, conn, this, 
			id_ds);
	}
	
    public void createFeatureIfMissing(String objectType, String feature, 
    		String featureType) 
    throws DatabaseException 
    {
		TableIterator features =
			getTable("SELECT FEATURES FROM ["+objectType+"]").iterator();
			
		while (features.hasNext()) 
        {
			TableRow tr = features.next();
			String name = tr.getColumn(1);
			if (name.equals(feature))
            {
			    return;
            }
		}
		
		executeDirect("UPDATE OBJECT TYPE "+
			"["+objectType+" ADD "+feature+" : "+featureType+";]");
    }

    public void createObjectTypeIfMissing(String objectType) 
    throws DatabaseException 
    {
        boolean haveType = false;
    
        TableIterator features =
            getTable("SELECT OBJECT TYPES").iterator();
            
        while (features.hasNext()) 
        {
            TableRow tr = features.next();
            String name = tr.getColumn(1);
            if (name.equals(objectType))
            {
                haveType = true;
                break;
            }
        }
        
        if (!haveType)
        {
            executeDirect("CREATE OBJECT TYPE "+
                "["+objectType+"]");
        }
    }
    
    public String getVisibleMonadString()
    throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement
        (
            "SELECT Monad_First, Monad_Last " +
            "FROM   user_text_access " +
            "WHERE  (User_Name = ? OR User_Name = 'anonymous')"
        );
        stmt.setString(1, username);
        
        ResultSet rs = stmt.executeQuery();
        StringBuffer result = new StringBuffer();
        result.append("{");
        boolean haveMonads = false;
        
        while (rs.next()) 
        {
            haveMonads = true;
            String first = rs.getString(1);
            String last  = rs.getString(2);
            result.append(first+"-"+last);
            if (!rs.isLast())
            {
                result.append(",");
            }
        }
        
        result.append("}");
        
        stmt.close();
        rs.close();
        
        if (!haveMonads)
        {
            return null;
        }
        
        return result.toString();
    }

    public boolean canWriteTo(MatchedObject object)
    throws SQLException
    {
        SetOfMonads monads = new SetOfMonads();
        object.getSOM(monads, false);
        return canWriteTo(monads);
    }

    public boolean canWriteTo(String objectType, int objectId)
    throws DatabaseException, SQLException
    {
        return canWriteTo(objectType, new int[]{objectId});
    }
    
    public boolean canWriteTo(String objectType, int[] objectIds)
    throws DatabaseException, SQLException
    {
        String query = "GET MONADS FROM OBJECTS WITH ID_DS = ";
        
        for (int i = 0; i < objectIds.length; i++)
        {
            query += objectIds[i];
            if (i < objectIds.length - 1)
            {
                query += ",";
            }
        }
        
        Table table = getTable(query + " ["+objectType+"]");

        TableIterator rows = table.iterator();
        SetOfMonads monads = new SetOfMonads();
        
        while (rows.hasNext()) 
        {
            TableRow row = rows.next();
            int first = Integer.parseInt(row.getColumn(2)); 
            int last  = Integer.parseInt(row.getColumn(3));
            monads.add(first, last);
        }
        
        return canWriteTo(monads);
    }

    private boolean canWriteTo(SetOfMonads monads)
    throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement
        (
            "SELECT Monad_First, Monad_Last " +
            "FROM   user_text_access " +
            "WHERE  (User_Name = ? OR User_Name = 'anonymous') " +
            "AND    Write_Access = '1'"
        );
        stmt.setString(1, username);
        
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) 
        {
            int first = rs.getInt(1);
            int last  = rs.getInt(2);
            MonadSetElement mse = new MonadSetElement(first, last);
            monads.removeMSE(mse);
        }
        
        stmt.close();
        rs.close();

        return monads.isEmpty();
    }

}

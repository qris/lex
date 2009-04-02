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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import jemdros.BadMonadsException;
import jemdros.EmdrosEnv;
import jemdros.EmdrosException;
import jemdros.FlatSheaf;
import jemdros.MatchedObject;
import jemdros.MonadSetElement;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.Table;
import jemdros.TableException;
import jemdros.TableIterator;
import jemdros.TableRow;
import jemdros.TableRowException;
import jemdros.eCharsets;
import jemdros.eOutputKind;

import org.apache.log4j.Logger;

import com.qwirx.db.Change;
import com.qwirx.db.ChangeType;
import com.qwirx.db.Database;
import com.qwirx.db.DatabaseException;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EmdrosDatabase implements Database 
{
	private EmdrosEnv env;
	private Connection m_LogDatabase;
	private String username, userhost, database;
    private static final Logger m_LOG = Logger.getLogger(EmdrosDatabase.class);
	
	public EmdrosDatabase(String dbHost, String dbName, String dbUser,
        String dbPass, String logUser, String logFrom, Connection logDb) 
    throws DatabaseException, EmdrosException
    {
        env = new EmdrosEnv(eOutputKind.kOKConsole, 
            eCharsets.kCSISO_8859_1, dbHost, dbUser, dbPass, dbName); 

        if (!env.connectionOk()) 
        {
            throw new DatabaseException("Failed to connect to database",
                new Exception(env.getDBError()));
        }

        m_LogDatabase = logDb;
        this.username = logUser;
        this.userhost = logUser + "@" + logFrom;
		this.database = dbName;
	}

    public boolean isAlive()
    {
        return env.connectionOk();
    }
    
    public void setLogConnection(Connection logDb)
    {
        m_LogDatabase = logDb;
    }
    
	public void executeDirect(String query) throws DatabaseException 
    {
        m_LOG.info("Starting query: " + query);
        
        long startTime = System.currentTimeMillis();
        
		// Execute query
		boolean[] bCompilerResult = new boolean[1];

        boolean bDBResult = false;
        
        try
        {
            bDBResult = env.executeString(query, bCompilerResult, false, false);
        }
        catch (Exception e)
        {
            throw new DatabaseException("Failed to execute query", e, query);
        }
	
		if (!bDBResult) {
			throw new DatabaseException(
					"Database error: "+env.getDBError(), query);
		}

		if (!bCompilerResult[0]) {
			throw new DatabaseException(
					"Compiler error", 
                    new Exception(env.getCompilerError()), query);
		}
        
        long totalTime = System.currentTimeMillis() - startTime;
        m_LOG.info(totalTime+" ms to "+query);
	}

	public Sheaf getSheaf(String query) 
	throws DatabaseException 
    {
		executeDirect(query);
		
        if (!env.isSheaf()) 
        {
			throw new DatabaseException("result is not a sheaf", query);
		}
		
        return env.takeOverSheaf();
	}

    public FlatSheaf getFlatSheaf(String query) 
    throws DatabaseException 
    {
        executeDirect(query);
        
        if (!env.isFlatSheaf()) 
        {
            throw new DatabaseException("result is not a flat sheaf", query);
        }
        
        return env.takeOverFlatSheaf();
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
    
    public List<String[]> getTableAsListOfArrays(String query, int [] columns)
    throws DatabaseException
    {
        Table table = getTable(query);
        TableIterator rows = table.iterator();
        List<String[]> results = new ArrayList();
        
        try
        {
            while (rows.hasNext()) 
            {
                TableRow row = rows.next();
                String [] values = new String [columns.length];
                
                for (int i = 0; i < columns.length; i++)
                {
                    values[i] = row.getColumn(columns[i]);
                }
                
                results.add(values);
            }
        }
        catch (TableException te)
        {
            throw new DatabaseException("Failed to read table from Emdros", te);
        }

        return results;
    }
	
	public int getMinM() throws DatabaseException 
    {
        int [] min_m = new int[1];
        
	    if (!env.getMin_m(min_m))
        {
	        throw new DatabaseException("Failed to get MIN_M", 
                new Exception(env.getDBError()));   
        }
        
        return min_m[0];
	}

    public int getMaxM() throws DatabaseException 
    {
        int [] max_m = new int[1];
        
        if (!env.getMax_m(max_m))
        {
            throw new DatabaseException("Failed to get MAX_M", 
                new Exception(env.getDBError()));   
        }
        
        return max_m[0];
    }

	public Map<String, String> getEnumerationConstants(String type,
        boolean byName) 
	throws DatabaseException 
    {
		Hashtable<String, String> result = new Hashtable<String, String>();
		
		Table table = getTable("SELECT ENUMERATION CONSTANTS FROM "+type);
		
		TableIterator rows = table.iterator();
        
        try 
        {
    		while (rows.hasNext()) 
            {
    			TableRow row = rows.next();
    			
    			String name   = row.getColumn(1);
    			String number = row.getColumn(2);
    			
    			if (byName)
                {
    				result.put(name, number);
                }
                else
                {
                    result.put(number, name);
                }
    		}
        }
        catch (TableException e)
        {
            throw new DatabaseException("Failed to get enumeration constants", 
                e, "SELECT ENUMERATION CONSTANTS FROM "+type);
        }
        
		return result;
	}

    public List<String> getEnumerationConstantNames(String type) 
    throws DatabaseException 
    {
        List<String> result = new ArrayList<String>();
        
        String query = "SELECT ENUMERATION CONSTANTS FROM " + type;
        Table table = getTable(query);
        
        TableIterator rows = table.iterator();
        
        try 
        {
            while (rows.hasNext()) 
            {
                TableRow row = rows.next();
                String name   = row.getColumn(1);
                // String number = row.getColumn(2);
                result.add(name);
            }
        }
        catch (TableException e)
        {
            throw new DatabaseException("Failed to get enumeration constants", 
                e, query);
        }
        
        return result;
    }
    
    public String getEnumConstNameFromValue(String enumName, int value)
    throws DatabaseException
    {
        boolean[] dbOK = new boolean[1];
        
        String result = env.getEnumConstNameFromValue(value, enumName, dbOK);
    
        if (!dbOK[0])
        {
            throw new DatabaseException("Failed to get name for enum " + 
                enumName + " value " + value,
                new Exception(env.getDBError())); 
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

        try
        {
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
        }
        catch (TableException e)
        {
            throw new DatabaseException("Failed to calculate monad set ",
                e, "MONAD SET CALCULATION "+query);
        }
        
        result.append("}");
        return result.toString();
        
    }

    public String getMonadSet(String access, int min_m, int max_m)
    throws DatabaseException
    {
        return getMonadSet(access+" INTERSECT {"+min_m+"-"+max_m+"}");
    }
    
    public SetOfMonads intersect(SetOfMonads set, int min_m, int max_m)
    throws EmdrosException
    {
        System.out.println(set.toString());
        System.out.println(min_m + " : " + max_m);
    	return SetOfMonads.intersect(set, new SetOfMonads(min_m, max_m));
    }
    
	public Change createChange(ChangeType changeType, String objectType, 
			Object objectIds)
    throws DatabaseException
	{
        int [] id_ds = (int [])objectIds;
        
        try
        {
            if (changeType != EmdrosChange.CREATE)
            {
                if (id_ds != null && !canWriteTo(objectType, id_ds))
                {
                    throw new DatabaseException("You do not have permission "+
                        "to modify this object", changeType + " " + objectType +
                        " " + objectIds);
                }
                // must check access to monads later
            }
        }
        catch (Exception e)
        {
            throw new DatabaseException("Failed to determine object access",
                e, null);
        }
        
		return new EmdrosChange(username+"@"+userhost, database, 
			(EmdrosChange.Type)changeType, objectType, m_LogDatabase, this, 
			id_ds);
	}
	
    public void createFeatureIfMissing(String objectType, String feature, 
    		String featureType) 
    throws DatabaseException 
    {
		TableIterator features =
			getTable("SELECT FEATURES FROM ["+objectType+"]").iterator();

        try
        {
    		while (features.hasNext()) 
            {
    			TableRow tr = features.next();
    			String name = tr.getColumn(1);
    			if (name.equals(feature))
                {
    			    return;
                }
    		}
        }
        catch (TableException e)
        {
            throw new DatabaseException("Failed to get features", 
                e, "SELECT FEATURES FROM ["+objectType+"]");
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
        
        try
        {
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
        }
        catch (TableException e)
        {
            throw new DatabaseException("Failed to get the list of " +
            		"object types", e, "SELECT OBJECT TYPES");
        }
        
        if (!haveType)
        {
            executeDirect("CREATE OBJECT TYPE "+
                "["+objectType+"]");
        }
    }
    
    public SetOfMonads getVisibleMonads()
    throws DatabaseException
    {
        SetOfMonads result = new SetOfMonads();
        
        try
        {
            PreparedStatement stmt = m_LogDatabase.prepareStatement
            (
                "SELECT Monad_First, Monad_Last " +
                "FROM   user_text_access " +
                "WHERE  (User_Name = ? OR User_Name = 'anonymous')"
            );
            stmt.setString(1, username);
            
            ResultSet rs = stmt.executeQuery();
            boolean haveMonads = false;
            
            while (rs.next()) 
            {
                haveMonads = true;
                int first = rs.getInt(1);
                int last  = rs.getInt(2);
                result.add(first, last);
            }
            
            stmt.close();
            rs.close();
        
            if (!haveMonads)
            {
                return null;
            }
            
            return result;
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to get monad " +
                    "access control data", e);
        }
        catch (BadMonadsException e)
        {
            throw new DatabaseException("Failed to get monad " +
                "access control: invalid data in access table", e);
        }
    }

    public boolean canWriteTo(MatchedObject object)
    throws DatabaseException
    {
        SetOfMonads monads = new SetOfMonads();
        object.getSOM(monads, false);
        return canWriteTo(monads);
    }

    public boolean canWriteTo(String objectType, int objectId)
    throws DatabaseException, EmdrosException
    {
        return canWriteTo(objectType, new int[]{objectId});
    }
    
    public SetOfMonads getObjectMonads(String type, int [] objectIds)
    throws DatabaseException
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
        
        Table table = getTable(query + " ["+type+"]");

        TableIterator rows = table.iterator();
        SetOfMonads monads = new SetOfMonads();
        
        try
        {
            while (rows.hasNext()) 
            {
                TableRow row = rows.next();
                int first = Integer.parseInt(row.getColumn(2)); 
                int last  = Integer.parseInt(row.getColumn(3));
                monads.add(first, last);
            }
        }
        catch (EmdrosException e)
        {
            throw new DatabaseException("Failed to get monads from object", 
                e, query);
        }
        
        return monads;
    }
    
    public boolean canWriteTo(String objectType, int[] objectIds)
    throws DatabaseException
    {
        return canWriteTo(getObjectMonads(objectType, objectIds));
    }

    public boolean canWriteTo(SetOfMonads monads)
    throws DatabaseException
    {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        String query = "SELECT Monad_First, Monad_Last " +
            "FROM   user_text_access " +
            "WHERE  (User_Name = ? OR User_Name = 'anonymous') " +
            "AND    Write_Access = '1'";
        
        try
        {
            stmt = m_LogDatabase.prepareStatement(query);
            stmt.setString(1, username);
            rs = stmt.executeQuery();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to find database access " +
            		"permissions", e, query);
        }
        
        int first = 0;
        int last  = 0;
        
        SetOfMonads copy = new SetOfMonads(monads);

        try
        {
            while (rs.next()) 
            {
                first = rs.getInt(1);
                last  = rs.getInt(2);
                MonadSetElement mse = new MonadSetElement(first, last);
                copy.removeMSE(mse);
            }

            stmt.close();
            rs.close();
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to construct monad set " +
                first + "-" + last, e);
        }
        catch (BadMonadsException e)
        {
            throw new DatabaseException("Failed to construct monad set " +
                first + "-" + last, e);
        }
        
        return copy.isEmpty();
    }
    
    public void delete()
    {
        env.delete();
    }
}

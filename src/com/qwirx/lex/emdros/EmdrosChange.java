/*
 * Created on 02-Jan-2005
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import jemdros.Table;
import jemdros.TableException;
import jemdros.TableIterator;
import jemdros.TableRow;
import junit.framework.ComparisonFailure;

import org.apache.log4j.Logger;

import com.qwirx.db.Change;
import com.qwirx.db.DatabaseException;

/**
 * @author chris
 *
 * Represents and logs a change to an Emdros database object.
 */
public final class EmdrosChange implements Change 
{
    public static class MonadSetEntry
    {
        private int m_first, m_last;
        public MonadSetEntry(int first, int last)
        {
            if (last < first)
            {
                throw new AssertionError("last must be greater or " +
                        "equal to first");
            }
            m_first = first;
            m_last  = last;
        }
        public int first() { return m_first; }
        public int last()  { return m_last;  }
    }
    
	private Type   changeType;
	private Map    features, featureIsConstant;
	private String username, database, objectType;
	private EmdrosDatabase emdros;
	private int    id;
	private int [] objectIds;
	private Connection logDb;
    private Set    objectIdsToGetMonadsFrom = null;
    private Set    monads = null;
    private static final Logger m_log = Logger.getLogger(EmdrosChange.class);
	
	public EmdrosChange(String username, String database, 
			Type changeType, String objectType, Connection logDb,
			EmdrosDatabase emdros, int [] objectIds) 
	{
		this.username   = username;
		this.database   = database;
		this.changeType = changeType;
		this.objectType = objectType;
		this.objectIds  = objectIds;
		this.emdros     = emdros;
		this.logDb      = logDb;

		this.features          = new Hashtable();
		this.featureIsConstant = new Hashtable();
	}

	final static class Type 
    {
		private String name;
		private Type(String name) { this.name = name; }
		public String toString()  { return this.name; }
	}

	public static final Type
		CREATE = new Type("CREATE"),
		UPDATE = new Type("UPDATE"),
		DELETE = new Type("DELETE");
    
    public void setMonads(Set source)
    {
        if (objectIdsToGetMonadsFrom != null)
        {
            throw new AssertionError("Cannot set monads directly " +
                    "and from objects");
        }
        
        monads = new TreeSet(source);
    }

    public void setMonads(MonadSetEntry [] source)
    {
        if (objectIdsToGetMonadsFrom != null)
        {
            throw new AssertionError("Cannot set monads directly " +
                    "and from objects");
        }

        monads = new TreeSet();
        
        for (int i = 0; i < source.length; i++)   
        {
            MonadSetEntry mse = source[i];
            monads.add(mse);
        }
    }

    public void setMonadsFromObjects(Set source)
    {
        if (monads != null)
        {
            throw new AssertionError("Cannot set monads directly " +
                    "and from objects");
        }
        objectIdsToGetMonadsFrom = new TreeSet(source);
    }

    public void setMonadsFromObjects(int [] sources)
    {
        if (monads != null)
        {
            throw new AssertionError("Cannot set monads directly " +
                    "and from objects");
        }

        objectIdsToGetMonadsFrom = new TreeSet();
        
        for (int i = 0; i < sources.length; i++)   
        {
            int ID_D = sources[i];
            objectIdsToGetMonadsFrom.add(new Integer(ID_D));
        }
    }

	private int insertObjectChangeLog(int changedID_D) 
	throws SQLException 
    {
		PreparedStatement stmt = prepareAndLogError(
				"INSERT INTO changed_rows SET Log_ID = ?, " +
				"Unique_ID = ?");
		stmt.setInt(1, this.id);
		stmt.setInt(2, changedID_D);
		stmt.executeUpdate();
		stmt.close();

		stmt = prepareAndLogError(
			"SELECT LAST_INSERT_ID()");
		ResultSet rs = executeQueryAndLogError(stmt);
		rs.next();
		int logObjectEntryId = rs.getInt(1);
		rs.close();
		stmt.close();

		return logObjectEntryId;
	}			

	private int findObjectChangeLog(int changedID_D) 
	throws SQLException 
    {
		PreparedStatement stmt = prepareAndLogError(
				"SELECT ID FROM changed_rows WHERE Log_ID = ? AND " +
				"Unique_ID = ?");
		stmt.setInt(1, this.id);
		stmt.setInt(2, changedID_D);
		ResultSet rs = executeQueryAndLogError(stmt);
		rs.next();
		int logObjectEntryId = rs.getInt(1);
		rs.close();
		stmt.close();

		return logObjectEntryId;
	}			

	private void captureValues(boolean createRowChangeLogs, boolean storeAsNewValue) 
	throws SQLException, DatabaseException, TableException
    {
		/*
		System.out.println("Capturing values for "+type.toString()+" ("+
				createRowChangeLogs+", "+storeAsNewValue+")");
				*/
		if (objectIds == null || objectIds.length == 0) {
			throw new AssertionError("objectIds must be a non-empty array");
		}
		
		Table allFeatureNames = emdros.getTable(
				"SELECT FEATURES FROM ["+objectType+"]");
		Vector featureNames = new Vector();
		
		StringBuffer featureQuery = new StringBuffer("GET FEATURES ");
		
		for (TableIterator rows = allFeatureNames.iterator(); 
			rows.hasNext(); ) 
		{
			TableRow row      = rows.next(); 
			String   name     = row.getColumn(1);
			String   computed = row.getColumn(4);
			
			if (computed.equals("true")) {
				continue;
			} else if (computed.equals("false")) {
				if (featureNames.size() != 0) featureQuery.append(", ");
				featureQuery.append(name);
				featureNames.add(name);
			} else {
				throw new AssertionError("feature computed column " +
						"must be 'true' or 'false'");
			}
		}
		
		if (featureNames.size() == 0) return;
		
		featureQuery.append(" FROM OBJECTS WITH ID_DS = ");
		
		for (int i = 0; i < objectIds.length; i++) {
			int id = objectIds[i];
			featureQuery.append(id + "");
			if (i < objectIds.length - 1)
				featureQuery.append(",");
		}
		
		featureQuery.append(" ["+objectType+"]");
		
		Table allFeatureValues = emdros.getTable(featureQuery.toString());
		String destColName = storeAsNewValue ? "New_Value" :  "Old_Value";

        String cvsQuery;
		if (createRowChangeLogs) {
			cvsQuery = 
					"INSERT INTO changed_values SET "+destColName+" = ?, " +
					"Row_ID = ?, Col_Name = ?";
		} else {
			cvsQuery = 
					"UPDATE changed_values SET "+destColName+" = ? " +
					"WHERE Row_ID = ? AND Col_Name = ?";
		}

        PreparedStatement cvs = prepareAndLogError(cvsQuery);

		for (TableIterator rows = allFeatureValues.iterator(); 
			rows.hasNext(); ) 
		{
			TableRow row = rows.next();
			int objectID_D = Integer.parseInt(row.getColumn(1));
			
			int logObjectEntryId;
			
			if (createRowChangeLogs) {
				logObjectEntryId = insertObjectChangeLog(objectID_D);
			} else { 
				logObjectEntryId = findObjectChangeLog(objectID_D);
			}
			
			for (int i = 0; i < featureNames.size(); i++) 
            {
                long startTime = System.currentTimeMillis();
                
				String featureName  = (String)( featureNames.elementAt(i) );
				String currentValue = row.getColumn(i + 2);
				cvs.setString(1, currentValue);
				cvs.setInt   (2, logObjectEntryId);
				cvs.setString(3, featureName);
				cvs.executeUpdate();
                
                long totalTime = System.currentTimeMillis() - startTime;
                m_log.info(totalTime+" ms to "+cvsQuery+" ("+
                        currentValue+","+logObjectEntryId+","+featureName+")");
			}

            {
                long startTime = System.currentTimeMillis();

                String query = "DELETE FROM changed_values " +
                    "WHERE Row_ID = ? AND Old_Value = New_Value";                
    			PreparedStatement stmt = prepareAndLogError(query);
    			stmt.setInt(1, logObjectEntryId);
    			stmt.executeUpdate();
    			stmt.close();
                
                long totalTime = System.currentTimeMillis() - startTime;
                m_log.info(totalTime+" ms to "+query+" ("+logObjectEntryId+")");
            }
		}

		cvs.close();
	}
	
	private PreparedStatement prepareAndLogError(String query) 
	throws SQLException 
    {
		try 
        {
			return logDb.prepareStatement(query);
		} 
        catch (SQLException e) 
        {
			System.err.println("Error preparing query ("+query+"): "+e);
			throw(e);
		}
	}
	
	private ResultSet executeQueryAndLogError(PreparedStatement stmt) 
	throws SQLException 
    {
		try 
        {
			return stmt.executeQuery();
		} 
        catch (SQLException e) 
        {
			System.err.println("Error executing query ("+stmt.toString()+"): "+e);
			throw(e);
		}
	}
	
	private void captureOldValues() 
	throws DatabaseException, SQLException, TableException
    {
		if (changeType != UPDATE && changeType != DELETE)
			throw new AssertionError("this method can only be used "+
					"when the command type is UPDATE or DELETE");
		
		captureValues(true, false);
	}
	
	private void captureNewValues() 
	throws SQLException, DatabaseException, TableException
	{
		if (changeType != CREATE && changeType != UPDATE)
			throw new AssertionError("this method can only be used "+
					"when the command type is CREATE or UPDATE");
		
		if (changeType == CREATE) {
			captureValues(true, true);
		} else {
			captureValues(false, true);
		}
	}

	public void execute() throws DatabaseException 
    {
        long startTime = System.currentTimeMillis();
        
		if (changeType != CREATE && objectIds.length == 0) 
        {
			throw new AssertionError("objectIds must be a non-empty array");
		}
		
		StringBuffer sb = new StringBuffer("Query not prepared yet");
		
		try 
        {
			boolean oldAutoCommit = logDb.getAutoCommit();
			logDb.setAutoCommit(false);
			
			PreparedStatement stmt = prepareAndLogError(
					"INSERT INTO change_log SET User = ?, Date_Time = NOW(), "+
					"DB_Type = 'Emdros', DB_Name = ?, Table_Name = ?, Cmd_Type = ?");
			stmt.setString(1, username);
			stmt.setString(2, database);
			stmt.setString(3, objectType);
			stmt.setString(4, changeType.toString());
			stmt.executeUpdate();
			stmt.close();
			
			stmt = prepareAndLogError("SELECT LAST_INSERT_ID()");
			ResultSet rs = executeQueryAndLogError(stmt);
			rs.next();
			this.id = rs.getInt(1);
			rs.close();
			stmt.close();
			
			if (changeType == UPDATE || changeType == DELETE) 
            {
				captureOldValues();
			}
			
			sb = new StringBuffer();
			
			if (changeType == CREATE) 
            {
				sb.append("CREATE OBJECT ");

                if (monads != null)
                {
                    sb.append("FROM MONADS = { ");
                    
                    for (Iterator i = monads.iterator(); 
                        i.hasNext(); )
                    {
                        MonadSetEntry mse = (MonadSetEntry)( i.next() );
                        sb.append(mse.first() + "-" + mse.last());
                        if (i.hasNext())
                        {
                            sb.append(",");
                        }
                    }

                    sb.append("}");
                }
                else if (objectIdsToGetMonadsFrom != null)
                {
                    sb.append("FROM ID_DS = ");
                    
                    for (Iterator i = objectIdsToGetMonadsFrom.iterator(); 
                        i.hasNext(); )
                    {
                        Integer id = (Integer)( i.next() );
                        sb.append(id.toString());
                        if (i.hasNext())
                        {
                            sb.append(",");
                        }
                    }
                }
                else
                {
                    throw new IllegalStateException("You must first call "+
                            "setMonads() or setMonadsFromObjects() to set "+
                            "the monad range for the object to be created");
                }                
			} 
            else if (changeType == UPDATE) 
            {
				sb.append("UPDATE OBJECT ");
            }
            else if (changeType == DELETE)
            {
                sb.append("DELETE OBJECT ");
            }
            else 
            {
                throw new AssertionError("Unsupported change type "+changeType);
            }
            
            if (changeType == UPDATE || changeType == DELETE)
            {
                sb.append("BY ID_DS = ");
				for (int i = 0; i < objectIds.length; i++) 
                {
					sb.append(objectIds[i]+"");
					if (i < objectIds.length - 1)
						sb.append(",");
				}
            }

            sb.append(" ["+objectType);
            
            if (changeType == CREATE || changeType == UPDATE)
            {
				sb.append(" ");
			
                for (Iterator i = features.entrySet().iterator(); 
                    i.hasNext(); ) 
                {
					Map.Entry e = (Map.Entry)( i.next() );
					sb.append(e.getKey() + " := ");
					
					Boolean isConstant = (Boolean)(
							featureIsConstant.get(e.getKey()));
					String value = (String)( e.getValue() );
					if (isConstant == Boolean.FALSE) {
						sb.append("\"" + 
							value.replaceAll("\"", "\\\"")
							.replaceAll("\\\\", "\\\\") + "\"");
					} else if (isConstant == Boolean.TRUE) {
						sb.append(value);
					}
					sb.append("; ");
				}
			}

            sb.append("]");

            // m_log.warn(sb.toString());
			
			Table ids = emdros.getTable(sb.toString());
			
			if (changeType == CREATE) 
            {
				objectIds = new int [ids.size()];
				int i = 0;
				for (TableIterator ti = ids.iterator(); ti.hasNext(); ) 
                {
					int id = Integer.parseInt(ti.next(1));
					objectIds[i++] = id;
				}
			}
			
			if (changeType == CREATE || changeType == UPDATE) 
            {
				captureNewValues();
			}
			
			logDb.commit();
			logDb.setAutoCommit(oldAutoCommit);

            long totalTime = System.currentTimeMillis() - startTime;
            m_log.info(totalTime + " ms to track "+sb.toString());
		} 
        catch (SQLException e) 
        {
            m_log.error(sb.toString(), e);
			throw new DatabaseException(e, sb.toString());
		}
        catch (TableException e) 
        {
            m_log.error(sb.toString(), e);
            throw new DatabaseException(e, sb.toString());
        }        
	}
	
	public void setInt(String feature, long value) 
    {
		features.put(feature, Long.toString(value));
		featureIsConstant.put(feature, Boolean.TRUE);
	}

	public void setString(String feature, String value) 
    {
		features.put(feature, value);
		featureIsConstant.put(feature, Boolean.FALSE);
	}

	public void setConstant(String feature, String value) 
    {
		features.put(feature, value);
		featureIsConstant.put(feature, Boolean.TRUE);
	}

	public int getId() 
    {
		return this.id;
	}
	
	public int getInsertedRowId() 
    {
		if (changeType != CREATE)
        {
			throw new IllegalStateException(
				"Only CREATE changes have an inserted ID");
        }
        if (objectIds.length != 1)
        {
            throw new ComparisonFailure("Wrong number of objects created", 
                "1", objectIds.length+"");
        }
		return objectIds[0];
	}
}

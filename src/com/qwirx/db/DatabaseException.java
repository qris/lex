/*
 * Created on 03-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.db;

import org.apache.log4j.Logger;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DatabaseException extends Exception 
{
	private final String query;
    private static final Logger LOG = Logger.getLogger(DatabaseException.class); 
	
    public DatabaseException(String message, Exception original, String query) 
    {
        super(message, original);

        if (original != null)
        {
            setStackTrace(original.getStackTrace());
        }

        this.query = query;

        LOG.error(toString(), original);
    }

    public DatabaseException(String message, Exception original) 
    {
        this(message, original, null);
    }

    public DatabaseException(String message, String query) 
    {
        this(message, null, query);
	}
	
    /*
    public DatabaseException(Exception original, String query) 
    {
        this(null, original, query);
	}
	*/
	
	public String toString() 
    {
        StringBuffer out = new StringBuffer();
        
		if (getMessage() != null) 
        {
            out.append(getMessage());
		}
        
        if (getCause() != null)
        {
            if (out.length() > 0) 
            {
                out.append(": ");
            }
            out.append(getCause().toString());
        }
        
        if (query != null)
        {
            if (out.length() > 0) 
            {
                out.append(" in query ");
            }
            out.append(query);
        }
		
		return out.toString();
	}
}

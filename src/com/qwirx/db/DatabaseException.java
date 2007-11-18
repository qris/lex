/*
 * Created on 03-Jan-2005
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
public class DatabaseException extends Exception 
{
	private final String message, query;
	private final Exception original;
	
    public DatabaseException(String message, Exception original, String query) 
    {
        this.message  = message;
        this.query    = query;
        this.original = original;
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
        
		if (message != null) 
        {
            out.append(message);
		}
        
        if (original != null)
        {
            if (out.length() > 0) 
            {
                out.append(": ");
            }
            out.append(original.toString());
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

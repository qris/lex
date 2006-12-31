/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public abstract class EdgeBase implements Edge 
{
	public Edge part(int partNum) 
    {
		return this.parts()[partNum];
	}
	public String attribute(String name) 
    { 
		return (String)( attributes().get(name) ); 
	}
    
	RuleEdge m_container;
    RulePart     m_location;
    
    public void bindTo(RuleEdge container, RulePart location)
    throws AlreadyBoundException
    {
        if (this.m_container != null && this.m_container != container)
        {
            throw new AlreadyBoundException(this, container, location);
        }
        this.m_container = container;
        this.m_location  = location;
    }
    
    public RuleEdge getBoundInstance() { return m_container; }
    public RulePart     getBoundLocation() { return m_location;  }

    public Edge getBoundCopy()
    {
        Edge newInstance = getUnboundCopy();
        try 
        {
            newInstance.bindTo(getBoundInstance(), getBoundLocation());
        }
        catch (AlreadyBoundException e)
        {
            throw new RuntimeException(e);
        }
        return newInstance;
    }
    
    public class AlreadyBoundException extends Exception
    {
        public static final long serialVersionUID = 1;
        public AlreadyBoundException(Edge thisInstance,
                Edge newContainer, RulePart newLoc)
        {
            super(thisInstance+" is already bound to "+
                    thisInstance.getBoundLocation()+" of "+
                    thisInstance.getBoundInstance());
        }
    }

    /**
     * Asserts equality (equivalence) between two Instances, 
     * either Rules or Words
     * @param a The first Instance (expected)
     * @param b The second Instance (actual)
     */
    public boolean equals(Object object)
    {
        Edge other = (Edge)object;
        
        if (! other.symbol().equals(symbol()))
            return false;

        if (this instanceof RuleEdge)
        {
            if (! (other instanceof RuleEdge))
                return false;
                
            RuleEdge ra = (RuleEdge)this;
            RuleEdge rb = (RuleEdge)other;
            if (ra.rule() != rb.rule())
                return false;
        }
        else if (this instanceof WordEdge)
        {
            if (! (other instanceof WordEdge))
                return false;
            
            WordEdge wa = (WordEdge)this;
            WordEdge wb = (WordEdge)other;
            if (!(wa.toString().equals(wb.toString())))
                return false;
        }
        
        Map bCopy = new Hashtable(other.attributes());
        for (Iterator key = attributes().keySet().iterator(); key.hasNext(); )
        {
            Object thisKey = key.next();
            if (attributes().get(thisKey) != other.attributes().get(thisKey))
                return false;
            bCopy.remove(thisKey);
        }
        
        if (bCopy.size() > 0)
            return false;
        
        int min = parts().length;
        if (min > other.parts().length)
            min = other.parts().length;
        
        for (int i = 0; i < min; i++)
        {
            if (! parts()[i].equals(other.parts()[i]))
                return false;
        }
        
        if (parts().length > min)
            return false;
        
        if (other.parts().length > min)
            return false;
        
        if (getBoundInstance() != null)
        {
            if (other.getBoundInstance() == null)
                return false;
            
            if (getBoundInstance().rule() !=
                other.getBoundInstance().rule())
                return false;
            
            if (getBoundLocation() != other.getBoundLocation())
                return false;
        }
    
        return true;
    }
}
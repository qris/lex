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

import org.apache.log4j.Logger;


public class RulePart 
{
	private final String  symbol;
	private final String  name;
	private final boolean m_isTerminal, m_canRepeat, m_canSkip; 
	private final Map     conditions;
	private static final Logger m_log = Logger.getLogger(RulePart.class);
    
	public RulePart(String symbol, String name, boolean terminal,
			Map conditions, boolean canRepeat, boolean canSkip) 
    {
		this.symbol       = symbol;
		this.name         = name;
		this.m_isTerminal = terminal;
		this.conditions   = conditions;
        this.m_canRepeat  = canRepeat;
        this.m_canSkip    = canSkip;
	}
	public RulePart(String symbol, boolean terminal, Map conditions, 
            boolean canRepeat, boolean canSkip)
    {
		this(symbol, (String)null, terminal, conditions, canRepeat, canSkip);
	}
    public static RulePart fromString(String description)
    {
        boolean canRepeat = false;
        boolean canSkip   = false;
        
        if (description.endsWith("?"))
        {
            canSkip = true;
            description = description.substring(0, description.length() - 1);
        }
        else if (description.endsWith("*"))
        {
            canSkip = true;
            canRepeat = true;
            description = description.substring(0, description.length() - 1);
        }
        else if (description.endsWith("+"))
        {
            canRepeat = true;
            description = description.substring(0, description.length() - 1);
        }

        if (!(description.startsWith("{") && description.endsWith("}"))) 
        {
            return new RulePart(description, true, new Hashtable(), 
                    canRepeat, canSkip);
        }
        
        description = description.substring(1, description.length() - 1);
        
        String [] symbolAndConditions = description.split("\\.", 2);
        Map conditions = new Hashtable();
        
        if (symbolAndConditions.length == 2) 
        {
            String [] conditionStrings = 
                symbolAndConditions[1].split(",");
            
            for (int j = 0; j < conditionStrings.length; j++) 
            {
                String [] nameAndValue = 
                    conditionStrings[j].split("=");
                
                if (nameAndValue.length == 1)
                {
                    // valid attribute but value unspecified, 
                    // to be filled in later
                    conditions.put(nameAndValue[0], null);
                }
                else if (nameAndValue.length == 2) 
                {
                }
                else
                {
                    throw new IllegalArgumentException
                    (
                            "Invalid condition: "+
                            conditionStrings[j]
                    );
                }
                conditions.put(nameAndValue[0], nameAndValue[1]);
            }
            
            description = symbolAndConditions[0];
        } 
        
        String [] symbolAndName = description.split(":");
        String name = null;

        if (symbolAndName.length == 1) 
        {
            return new RulePart(symbolAndName[0], false, conditions,
                    canRepeat, canSkip);
        } 
        else if (symbolAndName.length == 2) 
        {
            name = symbolAndName[1];
            return new RulePart(symbolAndName[0], name, false, conditions,
                    canRepeat, canSkip);
        } 

        throw new IllegalArgumentException
        (
                "Invalid symbol format: "+description
        );
    }
	public String symbol()     { return symbol; }
	public String name()       { return name;   }
	public boolean terminal()  { return m_isTerminal; }
    public boolean canRepeat() { return m_canRepeat; }
    public boolean canSkip()   { return m_canSkip; }
    public Map conditions()    { return new Hashtable(conditions); }
    
    public boolean matches(Edge edge)
    {
        if (terminal()) 
        {
            String literalToMatch = symbol();

            if (edge instanceof MorphEdge)
            {
                MorphEdge me = (MorphEdge)edge;
                
                if (literalToMatch.startsWith("-") == me.hasPrev())
                {
                    if (literalToMatch.startsWith("-"))
                    {
                        literalToMatch = literalToMatch.substring(1);
                    }
                }
                else
                {
                    // cannot match
                    return false;
                }
                
                if (literalToMatch.endsWith("-") == me.hasNext())
                {
                    if (literalToMatch.endsWith("-"))
                    {
                        literalToMatch = literalToMatch.substring(0, 
                            literalToMatch.length() - 1);
                    }
                }
                else
                {
                    // cannot match
                    return false;
                }
            }
            else if (! (edge instanceof WordEdge)) 
            {
                m_log.debug
                (
                        "wanted "+literalToMatch+" but found "+edge
                );
                return false;
            }
            
            if (! literalToMatch.equals(edge.symbol())) 
            {
                m_log.debug
                (
                        "wanted "+literalToMatch+" but found "+edge
                );
                return false;
            }
        } 
        else 
        {
            String symbolToMatch = symbol(); 
            String symbolOnStackName = edge.symbol();
            
            if (edge instanceof WordEdge) 
            {
                m_log.debug
                (
                        "wanted "+symbolToMatch+" but found "+edge
                );
                return false;
            }
            
            if (! symbolToMatch.equals(symbolOnStackName)) 
            {
                m_log.debug
                (
                        "wanted "+symbolToMatch+" but found "+symbolOnStackName
                );
                return false;
            }
        }
        
        Map attributes = edge.attributes();
        
        for (Iterator i = conditions.keySet().iterator(); i.hasNext();)
        {
            String name  = (String)( i.next() );
            String expectedValue = (String)( conditions.get(name) );
            
            if (!attributes.containsKey(name))
            {
                m_log.debug("Missing attribute "+name+" in "+edge);
                return false;
            }
            
            String actualValue = (String)attributes.get(name);
            
            if (actualValue == null)
            {
                // this is acceptable, will unify later
            }
            else if (! actualValue.equals(expectedValue))
            {
                m_log.debug("Wrong value for attribute "+name+" in "+edge+
                    ": expected '" + expectedValue +"' but found '" +
                    actualValue + "'");
                return false;
            }
        }

        return true;
    }
    
    /**
     * For use in unit tests only. Tests that a rule part matches a set
     * of instances, for example if it's a repeating rule then it
     * can repeat and match over all of them.
     * @param instances The list of instances to match.
     * @return true if this part matches all of them, false if not.
     */
    public boolean matches (Edge [] instances)
    {
        if (instances.length == 0 && !m_canSkip)   return false;
        if (instances.length >  1 && !m_canRepeat) return false;

        for (int i = 0; i < instances.length; i++)
        {
            Edge instance = instances[i];
            if (!matches(instance))
                return false;
        }
        
        return true;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        if (!m_isTerminal) buf.append("{");
        
        buf.append(symbol);
        if (name != null) buf.append(":").append(name);
        
        if (conditions.size() > 0)
        {
            buf.append(".");
            for (Iterator i = conditions.keySet().iterator(); i.hasNext(); )
            {
                String name  = (String)( i.next() );
                String value = (String)( conditions.get(name) );
                buf.append(name).append("=").append(value);
                if (i.hasNext()) buf.append(",");
            }
        }
        
        if (!m_isTerminal) buf.append("}");
        
        if (m_canRepeat && m_canSkip)
            buf.append("*");
        else if (m_canRepeat)
            buf.append("+");
        else if (m_canSkip)
            buf.append("?");
        
        return buf.toString();
    }
}
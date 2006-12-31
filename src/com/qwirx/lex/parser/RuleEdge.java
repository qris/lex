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
import java.util.Vector;
import java.util.Map.Entry;

public class RuleEdge extends EdgeBase implements Cloneable
{
	private final Edge [] parts;
	private final Rule    rule;
	private Map attribs = new Hashtable();
	
	public Rule   rule()   { return rule; }
	public String symbol() { return rule.symbol(); }

    /**
     * Constructs a new RuleInstance using the default constructor, but binds
     * it to the given RulePart, which must be part of a Rule, to an instance
	 * of which this RuleInstance should be passed in the parts[] array, thus
	 * binding it to the given instance as well as the location.
     * @param rule The rule of which this is an instance
     * @param parts The instances which this instance contains
     * @param location The RulePart to which this instance should be bound
     */
    public RuleEdge(Rule rule, Edge [] parts, RulePart location)
    {
        this(rule, parts);
        try
        {
            bindTo(null, location);
        }
        catch (AlreadyBoundException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Constructs a new RuleInstance by shallow copying the 
     * given instances and attaching the copies to the rule.
     * @param rule The rule of which this is an instance
     * @param parts The instances which this instance contains
     */
    public RuleEdge(Rule rule, Edge [] parts) 
    {
		super();
        this.rule  = rule;

        this.parts = new Edge [parts.length];
        
        for (int i = 0; i < this.parts.length; i++)
        {
            Edge instanceOrig = parts[i];
            
            RulePart location = instanceOrig.getBoundLocation();
            if (location == null)
            {
                throw new IllegalArgumentException
                (
                        "'"+instanceOrig+"' is not bound to any location "
                );
            }
            
            boolean found = false;
            for (int p = 0; p < rule().parts().length; p++)
            {
                RulePart rulePart = rule().parts()[p];
                if (location == rulePart)
                {
                    found = true;
                    break;
                }
            }
            
            if (!found)
            {
                throw new IllegalArgumentException
                (
                        "'"+instanceOrig+"' is bound to a location "+
                        "in a different rule ("+location+")"
                );
            }
            
            try 
            {
                Edge instanceCopy = instanceOrig.getUnboundCopy();
                instanceCopy.bindTo(this, location);
                this.parts[i] = instanceCopy;
            }
            catch (AlreadyBoundException e)
            {
                throw new RuntimeException(e);
            }
        }

		findAttributes();
	}
    
	public String toString() 
    {
		StringBuffer buf = new StringBuffer();
		appendString(buf);
		return buf.toString();
	}
    
	public void appendString(StringBuffer buf) 
    {
		buf.append("{");
		buf.append(rule().symbol());
		buf.append(" ");
		
		Map attribs = attributes();
		for (Iterator i = attribs.entrySet().iterator(); i.hasNext(); ) 
        {
			Entry e = (Entry)( i.next() );
			buf.append(e.getKey());
			buf.append('=');
			buf.append(e.getValue());
			if (i.hasNext())
				buf.append(',');
			else
				buf.append(' ');
		}
		
		for (int i = 0; i < parts.length; i++) 
        {
			parts[i].appendString(buf);
            
            String name = parts[i].getBoundLocation().name();
			if (name != null) 
            {
				buf.append(":");
				buf.append(name);
			}
            
			if (i < parts.length - 1)
				buf.append(' ');
		}
		buf.append("}");
	}
    
	public void appendStringPrettyPrint(StringBuffer buf) 
    {
		appendStringPrettyPrint(buf, "");
	}
    
	private void appendStringPrettyPrint(StringBuffer buf, String indent) 
    {
		buf.append(indent);
		buf.append("{");
		buf.append(rule().symbol());
		buf.append(" ");
		
		Map attribs = attributes();
		for (Iterator i = attribs.entrySet().iterator(); i.hasNext(); ) 
        {
			Entry e = (Entry)( i.next() );
			buf.append(e.getKey());
			buf.append('=');
			buf.append(e.getValue());
			if (i.hasNext())
				buf.append(',');
		}
		
		buf.append('\n');
		
		for (int i = 0; i < parts.length; i++) 
        {
			Edge part = parts[i];
            
			if (part instanceof RuleEdge) 
            {
				RuleEdge rulePart = (RuleEdge)part;
				rulePart.appendStringPrettyPrint(buf, indent + "\t");
			} 
            else 
            {
				buf.append(indent + "\t");
				part.appendString(buf);
			}
            
            String name = parts[i].getBoundLocation().name();
            if (name != null) 
            {
                buf.append(":");
                buf.append(name);
            }
            
			buf.append('\n');
		}
		buf.append(indent);
		buf.append("}");
	}
    
	public String[] words() 
    {
		Vector v = new Vector();
		appendWords(v);
		return (String[])( v.toArray(new String[v.size()]) );
	}
    
	public void appendWords(Vector words) 
    {
		for (int i = 0; i < parts.length; i++) 
        {
			parts[i].appendWords(words);
		}
	}
    
	public Edge[] parts() 
    {
		Edge [] result = new Edge[parts.length];
		System.arraycopy(parts, 0, result, 0, parts.length);
		return result;
	}
	
    public String partName(int partNum) 
    {
        if (partNum >= rule().parts().length)
        {
            throw new ArrayIndexOutOfBoundsException
            (
                    "Rule "+rule()+" has no part number "+partNum
            );
        }
		return rule().parts()[partNum].name();
	}
	
    public Map attributes() { return attribs; }
	private void findAttributes() 
    {
		attribs = new Hashtable();
		
		for (Iterator i = rule().copiedAttributes().entrySet().iterator();
			i.hasNext(); )
		{
			Entry e = (Entry)( i.next() );
			Object name = e.getKey();
			RulePart[] ruleParts = rule().parts();
			
			for (int p = 0; p < ruleParts.length; p++) 
			{
				String partName = ruleParts[p].name();
				
				if (partName == null)
					continue;
				
				if (! partName.equals(e.getValue()))
					continue;
				
				Edge partInstance = parts[p];
                
				Object value = partInstance.attributes().get(name);
                
				if (value == null) 
                {
					throw new IllegalStateException(
							partInstance.toString()+
							" has no attribute named "+
							name+" to copy into "+this);
				}
                
				Object oldValue = attribs.get(name);
                
				if (oldValue != null && ! oldValue.equals(value)) 
                {
					throw new IllegalStateException(
							"Unable to unify "+oldValue+" and "+value+
							" for attribute "+name+" of "+this);
				}
                
				if (oldValue == null) 
                {
					attribs.put(name, value);
				}
			}
		}

		for (Iterator i = rule().fixedAttributes().entrySet().iterator();
			i.hasNext(); ) 
        {
			Entry e = (Entry)( i.next() );
			
            Object name     = e.getKey();
			Object value    = e.getValue();
			Object oldValue = attribs.get(name);
			
            if (oldValue != null && ! oldValue.equals(value)) 
            {
				throw new IllegalStateException(
						"Unable to unify "+oldValue+" and "+value+
						" for attribute "+name+" of "+this);
			}
            
			if (oldValue == null) 
            {
				attribs.put(name, value);
			}
		}
	}
    
	public int getDepthScore() 
    {
		return getDepthScore(1);
	}
    
	private int getDepthScore(int initialDepth) 
    {
		int d = initialDepth;
		for (int i = 0; i < parts.length; i++) 
        {
			if (parts[i] instanceof RuleEdge) 
            {
				d += ((RuleEdge)(parts[i])).getDepthScore(initialDepth+1);
			}
		}
		return d;
	}
    
    public Edge getUnboundCopy()
    {
        return new RuleEdge(rule, parts);
    }
    
    public boolean isAt(int position)
    {
        for (int i = 0; i < parts.length; i++)
        {
            if (parts[i].isAt(position))
            {
                return true;
            }
        }
        
        return false;
    }
}
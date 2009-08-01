/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.qwirx.lex.TreeNode;
import com.qwirx.lex.parser.Rule.Attribute;
import com.qwirx.lex.parser.Rule.CopiedAttribute;

public class RuleEdge extends EdgeBase implements Cloneable
{
    private RuleEdge m_UnboundOriginal = null;
	private final Edge [] parts;
	private final Rule    rule;
	
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
	}
    
	public String toString() 
    {
		StringBuffer buf = new StringBuffer();
		appendString(buf, true);
		return buf.toString();
	}

    public String toStringWithoutAttributes() 
    {
        StringBuffer buf = new StringBuffer();
        appendString(buf, false);
        return buf.toString();
    }
    
    public void appendString(StringBuffer buf) 
    {
        appendString(buf, true);
    }

	public void appendString(StringBuffer buf, boolean withAttributes) 
    {
		buf.append("{");
		buf.append(rule().symbol());
        buf.append(" ");
        
        if (withAttributes)
        {
    		Map<String, String> attribs = null;
            
            try 
            {
                attribs = attributes();
                List<String> keys =  new ArrayList<String>(attribs.keySet());
                Collections.sort(keys);
    
                for (Iterator<String> i = keys.iterator(); i.hasNext(); ) 
                {
                    String name = i.next();
                    String value = attribs.get(name);
                    buf.append(name);
                    buf.append('=');
                    
                    if (value == null)
                    {
                        buf.append("unknown");
                    }
                    else if (value.contains(","))
                    {
                        buf.append('"');
                        buf.append(value);
                        buf.append('"');
                    }
                    else
                    {
                        buf.append(value);
                    }
                    
                    if (i.hasNext())
                    {
                        buf.append(',');
                    }
                    else
                    {
                        buf.append(' ');
                    }
                }
            }
            catch (UnificationException e)
            {
                buf.append("(failed to unify: "+e+") ");
            }
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

    abstract static class UnificationException extends IllegalStateException
    {
    }

    class CannotUnifyException extends UnificationException
    {
        private final String m_Attrib, m_OldValue, m_NewValue;
        
        public CannotUnifyException(String attrib, String oldValue,
            String newValue) 
        { 
            m_Attrib = attrib;
            m_OldValue = oldValue;
            m_NewValue = newValue;
        }
        
        public String toString()
        {
            return "Unable to unify "+m_OldValue+" and "+m_NewValue+
                " for attribute "+m_Attrib;
        }
    }

    public static class MissingPartException extends UnificationException
    {
        private final CopiedAttribute m_Attrib;
        
        public MissingPartException(CopiedAttribute attrib) 
        {
            m_Attrib = attrib;
        }
        
        public String toString()
        {
            return "Missing rule part/variable " + m_Attrib.getSource();
        }
    }

    public static class MissingAttributeException extends UnificationException
    {
        private final Attribute m_Attrib;
        
        public MissingAttributeException(Attribute attrib) 
        {
            m_Attrib = attrib;
        }
        
        public String toString()
        {
            return "Missing attribute " + m_Attrib;
        }
    }
    
    private List m_EdgeAttributes = new ArrayList();
    
    public void addAttribute(Attribute attr)
    {
        m_EdgeAttributes.add(attr);
    }
    
    private void mergeAttributeInto(Attribute in, Map<String, String> out)
    {
        String name = in.getName();
        String oldValue = out.get(name);
        String newValue = in.getValue(this);
        
        if (out.containsKey(name) && oldValue != null && 
            ! oldValue.equals(newValue))
        {
            throw new CannotUnifyException(name, oldValue, newValue);
        }
        
        if (! in.exists(this)) 
        {
            throw new IllegalStateException(
                    in.toString()+
                    " does not exist to copy into " +
                    this.toStringWithoutAttributes());
        }
        
        if (oldValue != null && ! oldValue.equals(newValue)) 
        {
            throw new CannotUnifyException(name, oldValue, newValue);
        }
        
        if (oldValue == null) 
        {
            out.put(name, newValue);
        }
    }

	public Map<String, String> attributes() 
    {
		Map attribs = new HashMap();
		
		for (Iterator i = rule().attributes().iterator(); i.hasNext(); )
		{
            Attribute attr = (Attribute)(i.next());
            mergeAttributeInto(attr, attribs);
		}

        for (Iterator i = m_EdgeAttributes.iterator(); i.hasNext(); )
        {
            Attribute attr = (Attribute)(i.next());
            mergeAttributeInto(attr, attribs);
        }

        return attribs;
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
        RuleEdge e = new RuleEdge(rule, parts);

        if (m_UnboundOriginal != null)
        {
            e.m_UnboundOriginal = m_UnboundOriginal;
        }
        else
        {
            e.m_UnboundOriginal = this;
        }
        
        for (Iterator i = m_EdgeAttributes.iterator(); i.hasNext();)
        {
            Attribute attr = (Attribute)i.next();
            e.addAttribute(attr);
        }

        return e;
    }
    
    public Edge getUnboundOriginal()
    {
        return m_UnboundOriginal;
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
    
    public boolean includes(Edge other)
    {
        for (int i = 0; i < parts.length; i++)
        {
            if (parts[i].includes(other))
            {
                return true;
            }
        }
        
        return false;
    }

    public boolean uses(Rule rule)
    {
        if (this.rule == rule)
        {
            return true;
        }
        
        for (int i = 0; i < parts.length; i++)
        {
            if (parts[i].uses(rule))
            {
                return true;
            }
        }
        
        return false;
    }
    

    public void getLeavesInto(List leaves)
    {
        for (int i = 0; i < parts.length; i++)
        {
            parts[i].getLeavesInto(leaves);
        }
    }
    
    public boolean overlaps(Edge other)
    {
        List otherLeaves = new ArrayList();
        other.getLeavesInto(otherLeaves);
        
        for (Iterator i = otherLeaves.iterator(); i.hasNext(); )
        {
            Edge otherLeaf = (Edge)( i.next() );
            if (includes(otherLeaf))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public String getHtmlLabel()
    {
        return symbol().replaceAll("/(.*)", "<sub>$1</sub>");
    }

    public TreeNode toTree()
    {
        TreeNode node = new TreeNode(getHtmlLabel());
        
        for (int i = 0; i < parts.length; i++)
        {
            node.add(parts[i].toTree());
        }
        
        return node;
    }
    
    public int getLeftPosition()
    {
        int pos = parts[0].getLeftPosition();
        for (int i = 1; i < parts.length; i++)
        {
            int p2 = parts[i].getLeftPosition();
            if (pos > p2) pos = p2;
        }
        return pos;
    }
    
    public int getRightPosition()
    {
        int pos = parts[0].getRightPosition();
        for (int i = 1; i < parts.length; i++)
        {
            int p2 = parts[i].getRightPosition();
            if (pos < p2) pos = p2;
        }
        return pos;
    }

    public int getDepth()
    {
        int maxChildDepth = 1;
        
        for (int i = 0; i < parts.length; i++)
        {
            int d = parts[i].getDepth();
            if (maxChildDepth < d)
            {
                maxChildDepth = d;
            }
        }
        
        return maxChildDepth + 1;
    }
    
    public boolean isTerminal()
    {
        return false;
    }
}
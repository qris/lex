/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.qwirx.lex.ImmutableMap;
import com.qwirx.lex.TreeNode;
import com.qwirx.lex.parser.Rule.Attribute;

public class WordEdge extends EdgeBase
{
	private final String surface;
    private static final Edge[]   parts   = new Edge[0];
    private static final ImmutableMap attribs = new ImmutableMap(new HashMap());
    private int m_Position;
    private WordEdge m_UnboundOriginal = null;
    
    /**
     * Default constructor, creates an instance of the given text as a 
     * "word" object.
     * @param surface The text of the word
     */
    public WordEdge(String surface, int position) 
    {
		super();
		this.surface = surface;
        this.m_Position = position;
	}
    
    /**
     * Constructs a word instance that's bound to a specified part of a rule.
     * Useful for example as follows:
     * <code>
     * List matches = rule.applyTo
     * (
     *     new Instance[]
     *     {
     *         new RuleInstance
     *         (
     *             cat, new Instance[]
     *             {
     *                 new WordInstance("cat", cat.part(0))
     *             }
     *         ),
     *         new RuleInstance
     *         (
     *             dog, new Instance[]
     *             {
     *                 new WordInstance("dog", dog.part(0))
     *             }
     *         )
     *     }
     * );
     * </code>
     * @param surface
     * @param boundLocation
     */
    public WordEdge(String surface, int position, RulePart boundLocation)
    {
        this(surface, position);
        
        try
        {
            bindTo(null, boundLocation);
        }
        catch (AlreadyBoundException e)
        {
            // should not happen in a newly created instance!
            throw new RuntimeException(e);
        }
    }
    
	public String toString() 
    {
		return surface;
	}
	public void appendString(StringBuffer buf) 
    {
		buf.append('"');
		buf.append(surface);
		buf.append('"');
	}
	public void appendWords(Vector words) 
    {
		words.add(surface);
	}
	public Edge[] parts() 
    {
		return parts;
	}
	public String partName  (int partNum) { return null; }
	public String symbol    () { return surface; }
	public Map    attributes() { return attribs; }
    public void addAttribute(Attribute attr)
    {
        attribs.put(attr.getName(), attr.getValue(this));
    }
    
    public Edge getUnboundCopy()
    {
        WordEdge e = new WordEdge(surface, m_Position);

        if (m_UnboundOriginal != null)
        {
            e.m_UnboundOriginal = m_UnboundOriginal;
        }
        else
        {
            e.m_UnboundOriginal = this;
        }

        return e;
    }
    
    public Edge getUnboundOriginal()
    {
        return m_UnboundOriginal;
    }
    
    public boolean isAt(int position)
    {  
        return m_Position == position;
    }
    
    public boolean includes(Edge other)
    {
        if (!(other instanceof WordEdge)) return false;
        WordEdge w = (WordEdge)other;
        return this.surface.equals(w.surface) &&
        this.m_Position == w.m_Position;
    }
    
    public String getHtmlLabel()
    {
        return surface;
    }
    
    public TreeNode toTree()
    {
        return new TreeNode(getHtmlLabel());
    }
    
    public void getLeavesInto(List leaves)
    {
        leaves.add(this);
    }
    
    public boolean overlaps(Edge other)
    {
        return other.includes(this);
    }

    public int getLeftPosition()
    {
        return m_Position;
    }
    
    public int getRightPosition()
    {
        return m_Position;
    }
    
    public int getDepth()
    {
        return 0; // surface
    }
    
    public boolean isTerminal()
    {
        return true;
    }
}
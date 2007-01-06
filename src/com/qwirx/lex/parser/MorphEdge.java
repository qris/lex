/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.qwirx.lex.HebrewConverter;
import com.qwirx.lex.ImmutableMap;
import com.qwirx.lex.TreeNode;

public class MorphEdge extends EdgeBase
{
	private final String m_Symbol, m_Surface;
    private static final Edge[] m_Parts = new Edge[0];
    private static final ImmutableMap m_Attribs = 
        new ImmutableMap(new HashMap());
    private int m_Position;
    private MorphEdge m_UnboundOriginal = null;
    
    /**
     * Default constructor, creates an instance of the given text as a 
     * "word" object.
     * @param surface The text of the word
     */
    public MorphEdge(String symbol, String surface, int position) 
    {
		super();
        m_Symbol   = symbol;
		m_Surface  = surface;
        m_Position = position;
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
        buf.append(m_Symbol);
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
        
        buf.append('"');
        buf.append(m_Surface);
        buf.append('"');
        buf.append("}");
    }

	public void appendWords(Vector words) 
    {
		words.add(m_Surface);
	}
    
	public Edge[] parts() 
    {
		return m_Parts;
	}
    
	public String partName  (int partNum) { return null; }
	public String symbol    () { return m_Symbol; }
	public Map    attributes() { return m_Attribs; }
    
    public Edge getUnboundCopy()
    {
        MorphEdge e = new MorphEdge(m_Symbol, m_Surface, m_Position);
        
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
        if (!(other instanceof MorphEdge)) return false;
        MorphEdge m = (MorphEdge)other;
        return this.m_Surface.equals(m.m_Surface) &&
            this.m_Symbol.equals(m.m_Symbol) &&
            this.m_Position == m.m_Position;
    }
    
    public String getHtmlLabel()
    {
        return m_Symbol.replaceAll("/(.*)", "<sub>$1</sub>");
    }
    
    public String getHtmlSurface()
    {
        return HebrewConverter.toHtml(m_Surface);
    }
    
    public TreeNode toTree()
    {
        TreeNode node = new TreeNode(getHtmlLabel());
        node.createChild(getHtmlSurface());
        return node;
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
        return 1; // surface + morphological analysis
    }
    
    public boolean isTerminal()
    {
        return false; // contains a morpheme inside
    }
}
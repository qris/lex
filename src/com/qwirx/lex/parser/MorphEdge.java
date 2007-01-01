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

import com.qwirx.lex.ImmutableMap;

public class MorphEdge extends EdgeBase
{
	private final String m_Symbol, m_Surface;
    private static final Edge[] m_Parts = new Edge[0];
    private static final ImmutableMap m_Attribs = 
        new ImmutableMap(new HashMap());
    private int m_Position;
    
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
        return new MorphEdge(m_Symbol, m_Surface, m_Position);
    }
    public boolean isAt(int position)
    {
        return m_Position == position;
    }
}
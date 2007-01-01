package com.qwirx.lex.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Chart
{
    private List m_Edges = new ArrayList();
    
    public List getEdges()
    {
        return new ArrayList(m_Edges);
    }
    
    public void add(Edge e)
    {
        m_Edges.add(e);
    }
    
    public void add(List edges)
    {
        for (Iterator i = edges.iterator(); i.hasNext(); )
        {
            add((Edge)( i.next() ));
        }
    }
    
    public List getEdgesAt(int pos)
    {
        List result = new ArrayList();
        
        for (Iterator i = m_Edges.iterator(); i.hasNext(); )
        {
            Edge e = (Edge)( i.next() );
            if (e.isAt(pos))
            {
                result.add(e);
            }
        }
        
        return result;
    }
    
    public String toString() { return m_Edges.toString(); }
}

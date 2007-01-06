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

    public int size()
    {
        return m_Edges.size();
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
    
    public Edge get(int index)
    {
        return (Edge)( m_Edges.get(index) );
    }
    
    public String toString() { return m_Edges.toString(); }
    
    public List filter(String goal, List requiredEdges, boolean verbose) 
    {
        List goals = new ArrayList();
        
        for (Iterator i = m_Edges.iterator(); i.hasNext(); ) 
        {
            Edge edge = (Edge)( i.next() );
            if (! edge.symbol().equals(goal))
            {
                if (verbose)
                {
                    System.out.println("Rejected non-goal edge: " + edge);
                }   
                
                continue;
            }
            
            boolean hasHoles = false;
            
            for (Iterator j = requiredEdges.iterator(); j.hasNext(); )
            {
                Edge required = (Edge)( j.next() );
                if (!edge.includes(required))
                {
                    hasHoles = true;
                    if (verbose)
                    {
                        System.out.println("Rejected edge: " + edge +
                            ": does not contain " + required);
                    }
                    break;
                }
            }
            
            if (hasHoles)
            {
                continue;
            }
            
            goals.add(edge);

            if (verbose)
            {
                System.out.println("Accepted edge: " + edge);
            }   
        }
        
        return goals;
    }
}

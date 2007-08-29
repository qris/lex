package com.qwirx.lex.parser;

import java.util.ArrayList;
import java.util.List;

public class MorphRule
{
    private int m_id;
    private String [] m_leftParts;
    private String m_right;
    
    public MorphRule(int id, String left, String right)
    {
        m_id = id;
        m_leftParts = left.split("-");
        m_right = right;
    }
    
    public List match(String input, int position)
    {
        if (!input.equals(m_right))
        {
            return null;
        }
        
        List edges = new ArrayList();
        for (int i = 0; i < m_leftParts.length; i++)
        {
            edges.add(new MorphEdge(m_leftParts[i], m_leftParts[i],
                position++, i > 0, i < m_leftParts.length - 1));
        }
        
        return edges;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        
        for (int i = 0; i < m_leftParts.length; i++)
        {
            buf.append(m_leftParts[i]);
            if (i < m_leftParts.length - 1)
            {
                buf.append("-");
            }
        }
        
        buf.append(" := ");
        buf.append(m_right);
        return buf.toString();
    }
}

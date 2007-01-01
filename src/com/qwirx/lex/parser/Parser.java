/*
 * Created on 26-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.qwirx.lex.DatabaseException;
import com.qwirx.lex.sql.SqlDatabase;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Parser 
{
	private Rule [] rules;
	
	public Parser(SqlDatabase db) throws DatabaseException, SQLException 
    {
		List rules = new ArrayList();
		
		try 
        {
			db.prepareSelect(
					"SELECT ID,Symbol,Lexeme FROM lexicon_entries " +
                    "WHERE Symbol IS NOT NULL " +
                    "AND   Lexeme IS NOT NULL");
			ResultSet rs = db.select();
			while (rs.next()) 
            {
				int    id    = rs.getInt(1);
				String left  = rs.getString(2);
				String right = rs.getString(3);
                Rule r = Rule.makeFromString(id, left, right);
                rules.add(r);
			}
		} 
        finally 
        {
			db.finish();
		}
		
		this.rules = (Rule[])( rules.toArray(new Rule[0]) );
	}
	
	public Parser(Rule[] rules) 
    {
		this.rules = rules;
	}
    
    private boolean m_Verbose = false;
    
    public void setVerbose(boolean verbose)
    {
        m_Verbose = verbose;
    }
	
	public Chart parse(List input) 
    {
        Chart chart = new Chart();
        
		for (int i = 0; i < input.size(); i++)
        {
            Edge inputEdge = (Edge)( input.get(i) );
            chart.add(inputEdge);
            
			for (int r = 0; r < rules.length; r++) 
            {
				/*
                if (r == 4 && s == 5)
                {
                    System.out.println("boo");
                }
				*/

                Rule rule = rules[r];
                List newEdges = rule.applyTo(chart, i);
                chart.add(newEdges);
			}
		}

        if (m_Verbose)
        {
            System.out.println("Parse finished.");
        }
        
        return chart;
	}

	public List parseFor(List input, String goal) 
    {
		Chart chart = parse(input);
		
        List edges = chart.getEdges();
		List goals = new ArrayList();
        
		for (Iterator i = edges.iterator(); i.hasNext(); ) 
        {
            Edge edge = (Edge)( i.next() );
            if (! edge.symbol().equals(goal))
            {
                if (m_Verbose)
                {
                    System.out.println("Rejected non-goal edge: " + edge);
                }   
                
                continue;
            }
            
            boolean hasHoles = false;
            
            for (Iterator j = input.iterator(); j.hasNext(); )
            {
                Edge other = (Edge)( j.next() );
                if (!edge.includes(other))
                {
                    hasHoles = true;
                    if (m_Verbose)
                    {
                        System.out.println("Rejected edge: " + edge +
                            ": does not contain " + other);
                    }
                    break;
                }
            }
            
            if (hasHoles)
            {
                continue;
            }
            
   			goals.add(edge);

            if (m_Verbose)
            {
                System.out.println("Accepted edge: " + edge);
            }   
		}
		
		return goals;
	}

    public List parseFor(String input, String goal) 
    {
        List edges = new ArrayList();
        String[] words = input.split(" ");
        
        for (int i = 0; i < words.length; i++) 
        {
            edges.add(new WordEdge(words[i], i));
        }
        
        return parseFor(edges, goal);
    }
}

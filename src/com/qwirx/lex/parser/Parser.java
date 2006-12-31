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
		Vector ruleV = new Vector();
		
		try 
        {
			db.prepareSelect(
					"SELECT ID,Symbol,Lexeme FROM lexicon_entries");
			ResultSet rs = db.select();
			while (rs.next()) 
            {
				int    id    = rs.getInt(1);
				String left  = rs.getString(2);
				String right = rs.getString(3);
				Rule r = Rule.makeFromString(id, left, right);
				ruleV.add(r);
			}
		} 
        finally 
        {
			db.finish();
		}
		
		this.rules = (Rule[])( ruleV.toArray(new Rule[0]) );
	}
	
	public Parser(Rule[] rules) 
    {
		this.rules = rules;
	}
	
	public Chart parse(String input) 
    {
		String[] words = input.split(" ");
        Chart chart = new Chart(words.length);
		
		for (int i = 0; i < words.length; i++) 
        {
            chart.add(new WordEdge(words[i], i));
            
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

		System.out.println("Parse finished.");
        
        return chart;
	}

    public List parseFor(String input, String goal)
    {
        return parseFor(input, goal, false);
    }
    
    /*
    private String getStringFromArray(Object [] array)
    {
        StringBuffer sb = new StringBuffer();
        
        for (int i = 0; i < array.length; i++)
        {
            sb.append(array[i]);
            if (i < array.length - 1)
            {
                sb.append(" ");
            }
        }
        
        return sb.toString();
    }
    */

	public List parseFor(String input, String goal, boolean verbose) 
    {
		Chart chart = parse(input);
		
        List edges = chart.getEdges();
		List goals = new ArrayList();
        
		for (Iterator i = edges.iterator(); i.hasNext(); ) 
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
            
            for (int j = 0; j < chart.getWidth(); j++)
            {
                if (! edge.isAt(j))
                {
                    hasHoles = true;
                    break;
                }
            }
            
            if (hasHoles)
            {
                if (verbose)
                {
                    System.out.println("Rejected edge with holes: " + edge);
                }   
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

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
import java.util.List;

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
    private MorphRule [] m_Morph;
	
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
	
    public Parser(Rule[] rules, MorphRule[] morphs) 
    {
        this.rules   = rules;
        this.m_Morph = morphs;
    }
    
 	public Parser(Rule[] rules) 
    {
		this(rules, new MorphRule [0]);
	}
    
    private boolean m_Verbose = false;
    
    public void setVerbose(boolean verbose)
    {
        m_Verbose = verbose;
    }
	
	public Chart parse(List inputOrig) 
    {
        List inputCopy = new ArrayList(inputOrig);
        Chart chart = new Chart();
        
		for (int i = 0; i < inputCopy.size(); i++)
        {
            Edge inputEdge = (Edge)( inputCopy.get(i) );
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
                inputCopy.addAll(i + 1, newEdges);
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
        return parse(input).filter(goal, input, m_Verbose);
	}

    public List parseFor(String input, String goal) 
    {
        List edges = new ArrayList();
        String[] words = input.split(" ");
        
        int position = 0;
        
        for (int i = 0; i < words.length; i++) 
        {
            List morphEdges = null;
            
            for (int m = 0; m < m_Morph.length && morphEdges == null; m++)
            {
                MorphRule mr = m_Morph[m];
                morphEdges = mr.match(words[i], position);
            }
            
            if (morphEdges == null)
            {
                morphEdges = new ArrayList();
                morphEdges.add(new WordEdge(words[i], position));
            }
            
            edges.addAll(morphEdges);
            position += morphEdges.size();
        }
        
        return parseFor(edges, goal);
    }
}

/*
 * Created on 26-Dec-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.qwirx.lex.DatabaseException;
import com.qwirx.lex.parser.Rule.Match;
import com.qwirx.lex.sql.SqlDatabase;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Parser {
	private Rule [] rules;
	
	public Parser(SqlDatabase db) throws DatabaseException, SQLException {
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
	
	public Edge[][] parse(String input) 
    {
		String[] words = input.split(" ");
		
		Vector stacks = new Vector();
		stacks.add(new Stack());
		
		for (int i = 0; i < words.length; i++) 
        {
            for (int s = 0; s < stacks.size(); s++) 
            {
                Stack stack = (Stack)( stacks.get(s) );
                stack.push(new WordEdge(words[i]));
            }

			for (int s = 0; s < stacks.size(); s++) 
            {
				Stack oldStack = (Stack)( stacks.get(s) );
			
				for (int r = 0; r < rules.length; r++) 
                {
					/*
                    if (r == 4 && s == 5)
                    {
                        System.out.println("boo");
                    }
					*/

                    Rule rule = rules[r];
                    List matches = rule.applyTo(oldStack);
                    
                    for (Iterator m = matches.iterator(); m.hasNext(); )
                    {
                        Match match = (Match)( m.next() );
                        // System.out.println(stacks.size() + ": " + match.stack);
                        stacks.add(match.stack);
                    }
				}
			}
		}

		System.out.println("Parse finished.");
		Edge[][] results = new Edge[stacks.size()][];

		for (int i = 0; i < stacks.size(); i++) 
        {
			Stack stack = (Stack)( stacks.get(i) );
			// System.out.println("Stack "+(i+1)+" contains:");

			Edge[] thisResult = new Edge[stack.size()];

			for (int j = 0; j < stack.size(); j++) 
            {
				// System.out.println(j+": "+
				//	stack.get(j).toString());
				thisResult[j] = (Edge)( stack.get(j) );
			}
			
			results[i] = thisResult;
		}
		
		return results;
	}

    public Edge[][] parseFor(String input, String goal)
    {
        return parseFor(input, goal, false);
    }
    
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

	public Edge[][] parseFor(String input, String goal, boolean verbose) 
    {
		Edge[][] parseResults = parse(input);
		
		Vector results = new Vector();
		for (int i = 0; i < parseResults.length; i++) 
        {
			Edge[] result = parseResults[i];
			if (result.length > 1) 
			{
                if (verbose)
                {
                    System.out.println("Rejected stack " + (i+1) +
                        ": too long: " + getStringFromArray(result));
                }   
                continue;
			}
			
            String symbol = result[0].symbol();
			if (! symbol.equals(goal)) 
            {
			    if (verbose)
                {
                    System.out.println("Rejected stack " + (i+1) +
                        ": not " + goal + ": " + getStringFromArray(result));
                }
                continue;
            }
            
   			results.add(result);
            
            if (!verbose)
            {
                continue;
            }
            
    		System.out.println("Accepted stack "+(i+1)+":");
            
            for (int j = 0; j < result.length; j++) 
            {
				if (result[j] instanceof RuleEdge) 
                {
					RuleEdge instance = (RuleEdge)( result[j] );
					StringBuffer buf = new StringBuffer();
					instance.appendStringPrettyPrint(buf);
					System.out.println(buf.toString());
				} 
                else 
                {
					System.out.print(result[j]);
					System.out.print(" ");
				}
			}
            
			System.out.println("");
		}
		
		Edge[][] resultArray = new Edge[results.size()][];
		resultArray = (Edge[][])( results.toArray(resultArray) );
		return resultArray;
	}
}

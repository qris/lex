package com.qwirx.lex.translit;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.qwirx.db.sql.DbColumn;
import com.qwirx.db.sql.DbTable;
import com.qwirx.db.sql.SqlDatabase;

public class DatabaseTransliterator
{
    private Logger m_Log = Logger.getLogger(getClass());
    
    public static void checkDatabase(Connection dbconn) throws Exception
    {
        new DbTable("translit_rules", "utf8",
            new DbColumn[]{
                new DbColumn("ID",          "INT(11)",     false, 
                        true, true),
                new DbColumn("Priority",    "INT(11)",     true), 
                new DbColumn("Precedent",   "TEXT", true),
                new DbColumn("Original",    "TEXT", true),
                new DbColumn("Succeedent",  "TEXT", true),
                new DbColumn("Replacement", "TEXT", true),
            }
        ).check(dbconn, true);
    }
    
    public static class Rule
    {
        private Logger m_Log = Logger.getLogger(getClass());
        
        String precedent, original, succeedent, replacement;
        Pattern m_BeforePattern, m_AfterPattern;
        
        public Rule(String precedent, String original, String succeedent,
            String replacement)
        throws Exception
        {
            this.precedent = precedent;
            this.original = original;
            this.succeedent = succeedent;
            this.replacement = replacement;
            try
            {
                m_BeforePattern = Pattern.compile(precedent + "$");
                m_AfterPattern = Pattern.compile("^" + original + succeedent);
            }
            catch (PatternSyntaxException e)
            {
                m_Log.error("Failed to construct pattern for rule " +
                    toString(), e);
                throw new Exception("Failed to construct pattern for rule " +
                    toString(), e);
            }
        }
        
        public boolean matchesAfter(String after)
        {
            return m_BeforePattern.matcher(after).find();
        }

        public boolean matchesBefore(String before)
        {
            return m_AfterPattern.matcher(before).find();
        }
        
        public boolean matches(String before, String after)
        {
            return matchesAfter(before) && matchesBefore(after);
        }

        public String toString()
        {
            return precedent + original + succeedent + " => " +
                precedent + replacement + succeedent;
        }
    }
    
    private List<Rule> m_Rules;
    
    public DatabaseTransliterator(SqlDatabase db)
    throws Exception
    {
        m_Rules = new ArrayList<Rule>();
        List<String[]> results = db.getTableAsList("SELECT Precedent, " +
                "Original, Succeedent, Replacement " +
                "FROM translit_rules ORDER BY Priority, ID");
        
        for (Iterator<String[]> i = results.iterator(); i.hasNext();)
        {
            String [] result = i.next();
            m_Rules.add(new Rule(result[0], result[1], result[2], result[3]));
        }
    }
    
    public String transliterate(String input)
    {
        StringBuffer output = new StringBuffer();
        
        for (int pos = 0; pos < input.length(); /* pos += result.length() */)
        {
            String before = input.substring(0, pos);
            String after = input.substring(pos);
            String consumed = null, result = null;

            for (Iterator<Rule> i = m_Rules.iterator(); i.hasNext();)
            {
                Rule rule = i.next();
                
                if (rule.matches(before, after))
                {
                    consumed = rule.original;
                    result = rule.replacement;
                    break;
                }
            }
            
            if (consumed == null)
            {
                // noting matched, this is probably bad!
                // copy and consume one character and try again
                consumed = after.substring(0, 1);
                result = consumed;
                m_Log.warn("No transliteration for character '" + result + "'");
            }
            
            output.append(result);
            pos += consumed.length();
        }
        
        return output.toString();
    }
}

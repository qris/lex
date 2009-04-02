package com.qwirx.lex.translit;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.qwirx.db.sql.DbColumn;
import com.qwirx.db.sql.DbTable;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;

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
        Pattern m_BeforePattern, m_AfterPattern, m_ConsumePattern;
        
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
                m_ConsumePattern = Pattern.compile("^" + original);
            }
            catch (PatternSyntaxException e)
            {
                m_Log.error("Failed to construct pattern for rule " +
                    toString(), e);
                throw new Exception("Failed to construct pattern for rule " +
                    toString(), e);
            }
        }
        
        public boolean matchesStartOf(String after)
        {
            return m_AfterPattern.matcher(after).find();
        }

        public boolean matchesEndOf(String before)
        {
            return m_BeforePattern.matcher(before).find();
        }
        
        public boolean matches(String before, String after)
        {
            return matchesEndOf(before) && matchesStartOf(after);
        }
        
        public String consumes(String after)
        {
            Matcher matcher = m_ConsumePattern.matcher(after);
            matcher.find();
            return matcher.group();
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
    
    /**
     * Transliterate the nth morpheme of the given list. Convenience method.
     * @param morphemes a list of morphemes forming a broken-up word
     * @param index the index of the morpheme to transliterate
     * @return
     */
    public String transliterate(List<Morpheme> morphemes, int index)
    {
        StringBuffer before = new StringBuffer();
        StringBuffer after = new StringBuffer();
        
        for (int i = 0; i < morphemes.size(); i++)
        {
            Morpheme morpheme = morphemes.get(i);
            
            if (i < index)
            {
                before.append(morpheme.getSurface());
            }
            
            if (i > index)
            {
                after.append(morpheme.getSurface());
            }
        }
        
        return transliterate(morphemes.get(index).getSurface(),
            before.toString(), after.toString());
    }
    
    /**
     * Pass in a morpheme, together with the text which precedes and
     * follows it, as this text may affect the transliteration.
     * @param input
     * @param precedingText
     * @param followingText
     * @return
     */
    public String transliterate(String input, String precedingText,
        String followingText)
    {
        StringBuffer output = new StringBuffer();
        
        for (int pos = 0; pos < input.length(); /* pos += result.length() */)
        {
            String before = precedingText + input.substring(0, pos);
            String after = input.substring(pos) + followingText;
            String consumed = null, result = null;

            for (Iterator<Rule> i = m_Rules.iterator(); i.hasNext();)
            {
                Rule rule = i.next();
                
                if (rule.matches(before, after))
                {
                    consumed = rule.consumes(after);
                    result = rule.replacement;
                    break;
                }
            }
            
            if (consumed == null)
            {
                // nothing matched, this is probably bad!
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

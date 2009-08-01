package com.qwirx.lex.translit;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.StringListConstIterator;
import jemdros.StringVector;

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

        new DbTable("translit_rule_attribs", "utf8",
            new DbColumn[]{
                new DbColumn("ID",          "INT(11)", false, true, true),
                new DbColumn("Rule_ID",     "INT(11)", false), 
                new DbColumn("Name",        "VARCHAR(20)", false),
                new DbColumn("Value",       "VARCHAR(20)", false),
            }
        ).check(dbconn, true);
    }
    
    public static class Rule
    {
        private Logger m_Log = Logger.getLogger(getClass());
        
        private int m_Id;
        private String precedent, original, succeedent, replacement;
        private Pattern m_BeforePattern, m_AfterPattern, m_ConsumePattern;
        private Map<String, String> m_Attribs;
        
        public Rule(int id, String precedent, String original,
            String succeedent, String replacement, Map<String, String> attribs)
        throws Exception
        {
            m_Id = id;
            this.precedent = precedent;
            this.original = original;
            this.succeedent = succeedent;
            this.replacement = replacement;
            m_Attribs = new HashMap<String, String>(attribs);
            
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
        
        public boolean matchesAttributes(Map inputAttributes)
        {
            for (String ruleAttrName : m_Attribs.keySet())
            {
                if (!inputAttributes.containsKey(ruleAttrName))
                {
                    return false;
                }

                String expectedValue = m_Attribs.get(ruleAttrName);
                if (!inputAttributes.get(ruleAttrName).equals(expectedValue))
                {
                    return false;
                }
            }
            
            return true;
        }
        
        public boolean matches(String before, String after,
            Map<String, String> inputAttributes)
        {
            return matchesEndOf(before) && matchesStartOf(after) &&
                matchesAttributes(inputAttributes);
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
        Map<Integer, Map<String, String>> ruleAttribs =
            new HashMap<Integer, Map<String, String>>();
        List<String[]> results = db.getTableAsList("SELECT ID, Rule_ID, " +
            "Name, Value FROM translit_rule_attribs");
        for (String [] values : results)
        {
            Integer id = Integer.valueOf(values[1]);
            Map<String, String> attribs = ruleAttribs.get(id);
            
            if (attribs == null)
            {
                attribs = new HashMap<String, String>();
                ruleAttribs.put(id, attribs);
            }
            
            String name = values[2];
            if (attribs.containsKey(name))
            {
                throw new IllegalArgumentException("Transliteration rule " + 
                    id + " has multiple attributes called '" + name + "'");
            }
            attribs.put(name, values[3]);
        }
        
        m_Rules = new ArrayList<Rule>();
        results = db.getTableAsList("SELECT ID, Precedent, " +
                "Original, Succeedent, Replacement " +
                "FROM translit_rules ORDER BY Priority, ID");
        
        for (Iterator<String[]> i = results.iterator(); i.hasNext();)
        {
            String [] result = i.next();
            Integer id = Integer.valueOf(result[0]);

            Map<String, String> attribs = ruleAttribs.get(id);
            if (attribs == null)
            {
                attribs = new HashMap<String, String>();
            }
            
            m_Rules.add(new Rule(id.intValue(), result[1], result[2],
                result[3], result[4], attribs));
        }
    }
    
    public Set<String> getRequiredFeatures()
    {
        Set<String> features = new HashSet<String>();
        
        for (Rule rule : m_Rules)
        {
            for (String attribName : rule.m_Attribs.keySet())
            {
                if (attribName.startsWith("word_"))
                {
                    features.add(attribName.substring(5));
                }
            }
        }
        
        return features;
    }

    /**
     * Convenience method. Transliterate the nth morpheme of the given list.
     * Gets attributes from the supplied MatchedObject which must be a [word].
     * @param morphemes a list of morphemes forming a broken-up word
     * @param index the index of the morpheme to transliterate
     * @return
     */
    public String transliterate(List<Morpheme> morphemes, int index,
        MatchedObject word)
    throws EmdrosException
    {
        if (! word.getObjectTypeName().equals("word"))
        {
            throw new IllegalArgumentException("MatchedObject must be a [word]");
        }
        
        Map<String, String> attributes = new HashMap<String, String>();
        
        StringVector features = word.getFeatureList().getAsVector();
        for (int i = 0; i < features.size(); i++)
        {
            String feature = features.get(i);
            String value = word.getFeatureAsString(i);
            attributes.put("word_" + feature, value);
        }
        
        return transliterate(morphemes, index, attributes);
    }

    /**
     * Convenience method. Transliterate the nth morpheme of the given list. 
     * @param morphemes a list of morphemes forming a broken-up word
     * @param index the index of the morpheme to transliterate
     * @return
     */
    public String transliterate(List<Morpheme> morphemes, int index,
        Map<String, String> inputAttributes)
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
            before.toString(), after.toString(), inputAttributes);
    }
    
    /**
     * For use in unit tests only!
     * Pass in a morpheme, together with the text which precedes and
     * follows it, as this text may affect the transliteration.
     * @param input
     * @param precedingText
     * @param followingText
     * @return
     */
    public String transliterate(String input, String precedingText,
        String followingText, Map<String, String> inputAttributes)
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
                
                if (rule.matches(before, after, inputAttributes))
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

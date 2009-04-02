package com.qwirx.lex.hebrew;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class WivuLexicon
{
    private Logger m_LOG = Logger.getLogger(getClass());
    
    public static class Key
    {
        private String m_Lexeme, m_Form;
        private int m_HashCode;
        public Key(String lexeme, String form)
        {
            m_Lexeme = lexeme;
            m_Form = form;
            m_HashCode = m_Lexeme.hashCode() ^ m_Form.hashCode();
        }
        public int hashCode() { return m_HashCode; }
        public boolean equals(Object other)
        {
            Key otherKey = (Key)other;
            return this.m_Lexeme.equals(otherKey.m_Lexeme) &&
                this.m_Form.equals(otherKey.m_Form);
        }
        public String toString()
        {
            return "{" + m_Lexeme + "," + m_Form + "}";
        }
    }
    
    public static class Entry
    {
        private String m_Gloss;
        public Entry(String gloss)
        {
            m_Gloss = gloss;
        }
        public String getGloss() { return m_Gloss; }
    }
    
    private Map<Key, Entry []> m_LexiconEntries =
        new HashMap<Key, Entry []>();
    
    private void loadFile(String filename) throws IOException
    {
        InputStream in = getClass().getResourceAsStream("/nl/knaw/dans/wivu/" +
            filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        int lineNumber = 0;
        
        for (String line = reader.readLine(); line != null;
            line = reader.readLine())
        {
            lineNumber++;
            String [] fields = line.split("\t");
            
            if (fields.length == 1 && fields[0].equals(""))
            {
                // ignore black lines
                continue;
            }
            
            if (fields.length != 7)
            {
                throw new IllegalArgumentException("Error in " + filename + 
                    ":" + lineNumber + ": wrong number of fields: " +
                    "expected 7 but found " + fields.length);
            }
            
            String lexeme = fields[0];
            lexeme = lexeme.replaceFirst("\\**$", "");
            
            String form = fields[5];
            if (form.equals("N/A"))
            {
                form = "";
            }
            
            Key key = new Key(lexeme, form);
            Entry value = new Entry(fields[6]);
            
            Entry [] oldEntries = m_LexiconEntries.get(key);
            
            /*
            if (oldEntries != null)
            {
                throw new IllegalArgumentException("duplicate value for key " +
                    key);
            }
            */
            
            if (oldEntries == null)
            {
                oldEntries = new Entry [0];
            }
            
            Entry [] newEntries = new Entry [oldEntries.length + 1];
            System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
            newEntries[oldEntries.length] = value;
            
            m_LexiconEntries.put(key, newEntries);
        }
    }
    
    public WivuLexicon() throws Exception
    {
        m_LOG.info("Loading WIVU lexicon");
        loadFile("hebrew-lexicon.txt");
        loadFile("aramaic-lexicon.txt");
    }
    
    public Entry [] getEntry(String lexeme, String form)
    {
        Key key = new Key(lexeme, form);
        return m_LexiconEntries.get(key);
    }
}

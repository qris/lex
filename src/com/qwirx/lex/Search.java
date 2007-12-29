package com.qwirx.lex;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jemdros.MatchedObject;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;

import org.xml.sax.SAXException;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;

public class Search
{
    private String m_Query;
    private EmdrosDatabase m_Emdros;
    
    public Search(String query, EmdrosDatabase emdros)
    {
        m_Query  = query;
        m_Emdros = emdros;
    }
    
    private static class ResultLocation
    {
        private SetOfMonads m_Monads;
        public ResultLocation(SetOfMonads monads)
        {
            m_Monads = monads;
        }
    }
    
    public List<SearchResult> run() 
    throws DatabaseException, IOException, SAXException, SQLException
    {
        HebrewMorphemeGenerator generator = 
            new HebrewMorphemeGenerator(m_Emdros);
        
        String getFeatures = "GET " +
            "lexeme, " +
            "phrase_dependent_part_of_speech, " +
            "graphical_preformative, " +
            "graphical_root_formation, " +
            "graphical_lexeme, " +
            "graphical_verbal_ending, " +
            "graphical_nominal_ending, " +
            "graphical_pron_suffix";
        
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            m_Emdros.getVisibleMonadString() + " " +
            "WHERE [verse GET book, chapter, verse, verse_label " +
            "       [clause "+
            "        [" +
            "         [word FIRST lexeme = '"+m_Query+"' " + getFeatures + "] " + 
            "         [word " + getFeatures + "]* " +
            "         [word LAST " + getFeatures + "] " +
            "        ]" +
            "        OR" +
            "        [" +
            "         [word FIRST " + getFeatures + "] " +
            "         [word " + getFeatures + "]* " +
            "         [word lexeme = '"+m_Query+"' " + getFeatures + "] " + 
            "         [word " + getFeatures + "]* " +
            "         [word LAST " + getFeatures + "] " +
            "        ]" +
            "        OR" +
            "        [" +
            "         [word FIRST " + getFeatures + "] " +
            "         [word " + getFeatures + "]* " +
            "         [word LAST lexeme = '"+m_Query+"' " + getFeatures + "] " + 
            "        ]" +
            "       ]"+
            "      ]");
             
        List<SearchResult> results = new ArrayList<SearchResult>();
        
        SheafConstIterator sci = sheaf.const_iterator();
        while (sci.hasNext())
        {
            Straw straw = sci.next();
            MatchedObject verse = straw.const_iterator().next();
            
            SheafConstIterator clause_iter =
                verse.getSheaf().const_iterator();
                
            while (clause_iter.hasNext())
            {
                MatchedObject clause =
                    clause_iter.next().const_iterator().next();

                String lexemes = "";
                
                StrawConstIterator word_iter =
                    clause.getSheaf().const_iterator().next().const_iterator();
                    
                while (word_iter.hasNext())
                {
                    MatchedObject word = word_iter.next();
                    
                    if (word.getEMdFValue("lexeme").toString().equals(m_Query))
                    {
                        lexemes += "<strong>";
                    }
                    
                    lexemes += HebrewConverter.wordToHtml(word, generator);
                    
                    if (word.getEMdFValue("lexeme").toString().equals(m_Query))
                    {
                        lexemes += "</strong>";
                    }
            
                    lexemes += " ";
                }

                SearchResult result = new SearchResult(
                    verse.getFeatureAsString(
                        verse.getEMdFValueIndex("verse_label")),
                    lexemes,
                    "clause.jsp?book=" + 
                    m_Emdros.getEnumConstNameFromValue("book_name_e",
                        verse.getEMdFValue("book").getInt()) +
                    "&amp;chapter=" + verse.getEMdFValue("chapter") +
                    "&amp;verse="   + verse.getEMdFValue("verse") +
                    "&amp;clause="  + clause.getID_D()
                );
                
                results.add(result);
            }
        }
        
        return results;
    }
    
    public static class SearchResult
    {
        private String m_Location, m_Description, m_LinkUrl;
        public SearchResult(String location, String description,
            String linkUrl)
        {
            m_Location = location;
            m_Description = description;
            m_LinkUrl = linkUrl;
        }
        public String getLocation() { return m_Location; }
        public String getDescription() { return m_Description; }
        public String getLinkUrl() { return m_LinkUrl; }
    }
}

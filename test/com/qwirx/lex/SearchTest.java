package com.qwirx.lex;

import java.util.Iterator;
import java.util.List;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import junit.framework.TestCase;

import org.aptivate.webutils.HtmlIterator;
import org.aptivate.webutils.HtmlIterator.Attributes;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.qwirx.lex.Search.SearchResult;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;

public class SearchTest extends TestCase
{
    private EmdrosDatabase m_Emdros;
    
    public void setUp() throws Exception
    {
        m_Emdros = Lex.getEmdrosDatabase("test", "test");
    }
    
    public void tearDown()
    {
        Lex.putEmdrosDatabase(m_Emdros);
    }
    
    private void assertSearchResultsMatch(String query) throws Exception
    {
        List<SearchResult> actualResults = new Search(query, m_Emdros).run();
        Iterator<SearchResult> actualIterator = actualResults.iterator();
        
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
            "         [word FIRST lexeme = '"+query+"' " + getFeatures + "] " + 
            "         [word " + getFeatures + "]* " +
            "         [word LAST " + getFeatures + "] " +
            "        ]" +
            "        OR" +
            "        [" +
            "         [word FIRST " + getFeatures + "] " +
            "         [word " + getFeatures + "]* " +
            "         [word lexeme = '"+query+"' " + getFeatures + "] " + 
            "         [word " + getFeatures + "]* " +
            "         [word LAST " + getFeatures + "] " +
            "        ]" +
            "        OR" +
            "        [" +
            "         [word FIRST " + getFeatures + "] " +
            "         [word " + getFeatures + "]* " +
            "         [word LAST lexeme = '"+query+"' " + getFeatures + "] " + 
            "        ]" +
            "       ]"+
            "      ]");
             
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
                    
                    if (word.getEMdFValue("lexeme").toString().equals(query))
                    {
                        lexemes += "<strong>";
                    }
                    
                    lexemes += HebrewConverter.wordToHtml(word, generator);
                    
                    if (word.getEMdFValue("lexeme").toString().equals(query))
                    {
                        lexemes += "</strong>";
                    }
            
                    lexemes += " ";
                }

                assertTrue(actualIterator.hasNext());
                SearchResult actual = actualIterator.next();
                assertEquals(verse.getEMdFValue("verse_label").getString(),
                    actual.getLocation());
                assertEquals(lexemes, actual.getDescription());
                assertEquals("clause.jsp?book=" + 
                    m_Emdros.getEnumConstNameFromValue("book_name_e",
                        verse.getEMdFValue("book").getInt()) +
                    "&amp;chapter=" + verse.getEMdFValue("chapter") +
                    "&amp;verse="   + verse.getEMdFValue("verse") +
                    "&amp;clause="  + clause.getID_D(),
                    actual.getLinkUrl()
                );
            }
        }
        
        assertFalse(actualIterator.hasNext());
    }

    public void testSearchCode() throws Exception
    {
        assertSearchResultsMatch("CMJM/");
        assertSearchResultsMatch("W");
        assertSearchResultsMatch("foo");
    }
    
    public void testSearchQueryPage() throws Exception
    {
        WebConversation conv = new WebConversation();
        WebResponse response = conv.getResponse("http://localhost:8080/lex/" +
            "search.jsp");
        HtmlIterator i = new HtmlIterator(response.getText());
        i.assertStart("html");
        i.assertStart("head");
        i.assertSimple("title", "Lex: Search");
        i.assertEmpty("link", new Attributes().add("rel", "stylesheet")
            .add("href", "style.css"));
        i.assertSimple("style", 
            "div.topmenu a.search_jsp { " +
            "background-color: white; " +
            "text-decoration: none; }",
            new Attributes().type("text/css"));
        i.assertEnd("head");
        i.assertStart("body");
        
        i.assertStart("h1");
        i.assertText("Lex");
        i.assertStart("font", 
            new Attributes().style("font-size: medium"));
        i.assertText("by");
        i.assertSimple("a", "Chris Wilson", 
            new Attributes().href("http://www.qwirx.com"));
        i.assertEnd("font");
        i.assertEnd("h1");
        
        i.assertStart("h2");
        i.assertText("RLM for BH:");
        i.assertSimple("a", "Nicolai Winther-Nielsen",
            new Attributes().href("http://www.winthernielsen.dk"));
        i.assertEnd("h2");

        i.assertStart("div", new Attributes().clazz("topmenu"));
        i.assertSimple("a", "Home", 
            new Attributes().clazz("index_jsp").href("index.jsp"));
        i.assertSimple("a", "Databases", 
            new Attributes().clazz("db_jsp").href(null));
        i.assertSimple("a", "Text", 
            new Attributes().clazz("clause_jsp").href("clause.jsp"));
        i.assertSimple("a", "Search", 
            new Attributes().clazz("search_jsp").href("search.jsp"));
        i.assertSimple("a", "Lexicon", 
            new Attributes().clazz("lsedit_jsp").href("lsedit.jsp"));
        i.assertSimple("a", "Parser", 
            new Attributes().clazz("parse_jsp").href("parse.jsp"));
        i.assertSimple("a", "Rules", 
            new Attributes().clazz("rules_jsp").href("rules.jsp"));
        /*
        i.assertSimple("a", "Database Dump", 
            new Attributes().clazz("dump_jsp").href("dump.jsp"));
        */
        i.assertSimple("a", "Login", 
            new Attributes().clazz("login_jsp").href("login.jsp?next=search.jsp"));
        i.assertEmpty("div", new Attributes().clazz("clearer"));
        i.assertEnd("div", new Attributes().clazz("topmenu"));
        
        i.assertEmpty("script", new Attributes().type("text/javascript"));
        
        i.assertStart("form", new Attributes().method("GET").clazz("bigborder"));
        i.assertSimple("p", "Search for Hebrew word by root " +
                "(surface consonants):");
        i.assertStart("p");
        i.assertEmpty("input", new Attributes().name("q"));
        i.assertEmpty("input", new Attributes().type("submit").value("Search"));
        i.assertEnd("p");
        i.assertStart("p");
        i.assertEmpty("input", new Attributes().type("checkbox")
            .name("limit_loc").value("1")
            .add("onclick", "return enableLimitControls()"));
        i.assertText("Limit search to");
        
        List books = m_Emdros.getEnumerationConstantNames("book_name_e");
        i.assertSelectFromList("book", "", books, false, 0);
        i.assertEnd("p");
        i.assertStart("p");
        i.assertText("Return only the first");
        i.assertInput("max_results", "100");
        i.assertText("clauses");
        i.assertEnd("p");
        i.assertEmpty("hr");

        i.assertSimple("p", "Three Latin consonants are expected, in upper " +
                "or lower case, from the set:");
        
        String latin  = ">BGDHWZXVJKLMNS<PYQRFCT";
        String hebrew = HebrewConverter.toHebrew(latin);
        String trans  = HebrewConverter.toTranslit(latin);
        
        i.assertStart("table", new Attributes().clazz("grid"));
        
        i.assertStart("tr");
        i.assertSimple("th", "Consonants");
        for (int j = 0; j < latin.length(); j++)
        {
            String c = latin.substring(j, j + 1);
            i.assertSimple("td", c);
        }
        i.assertEnd("tr");
        
        i.assertStart("tr");
        i.assertSimple("th", "Hebrew");
        for (int j = 0; j < hebrew.length(); j++)
        {
            String c = hebrew.substring(j, j + 1);
            i.assertSimple("td", c);
        }
        i.assertEnd("tr");

        i.assertStart("tr");
        i.assertSimple("th", "Transliteration");
        for (int j = 0; j < trans.length(); j++)
        {
            String c = trans.substring(j, j + 1);
            i.assertSimple("td", c);
        }
        i.assertEnd("tr");

        i.assertEnd("table");
        i.assertEnd("form");
        
        i.assertEmpty("script", new Attributes().type("text/javascript"));

        i.assertEmpty("hr");
        i.assertStart("p");
        i.assertText("&copy;");
        i.assertSimple("a", "Lex Project", 
            new Attributes().href("http://rrg.qwirx.com/trac/lex"));
        i.assertText("2005-2007.");
        i.assertEnd("p");
        i.assertEnd("body");
        i.assertEnd("html");
        i.assertEndDocument();
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(SearchTest.class);
    }

}

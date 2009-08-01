package com.qwirx.lex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import junit.framework.TestCase;

import org.aptivate.web.utils.HtmlIterator;
import org.aptivate.web.utils.HtmlIterator.Attributes;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.Search.SearchResult;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class SearchTest extends TestCase
{
    private EmdrosDatabase m_Emdros;
    private DatabaseTransliterator m_Transliterator;
    
    public void setUp() throws Exception
    {
        SqlDatabase sql = Lex.getSqlDatabase("test");
        m_Emdros = Lex.getEmdrosDatabase("chris", "test", sql);
        m_Transliterator = new DatabaseTransliterator(sql);
    }
    
    public void tearDown()
    {
        Lex.putEmdrosDatabase(m_Emdros);
    }
    
    private void assertSearchResultsMatch(String query)
    throws Exception
    {
        Search search = new Search(m_Emdros, m_Transliterator);
        assertSearchResultsMatch(query, search, 100);
    }
    
    private void assertSearchResultsMatch(String query, int limit)
    throws Exception
    {
        Search search = new Search(m_Emdros, m_Transliterator);
        search.setMaxResults(limit);
        assertSearchResultsMatch(query, search, limit);
    }
    
    private void assertSearchResultsMatch(String query, Search search, 
        int limit)
    throws Exception
    {    
        List<SearchResult> actualResults = search.basic(query);
        Iterator<SearchResult> actualIterator = actualResults.iterator();
        
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();
        
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            m_Emdros.getVisibleMonads().toString() + " " +
            "WHERE [clause "+
            "       [word " +
            "        lexeme_wit = '"+query+"' OR " + 
            "        lexeme_wit = '"+query+"/' OR " +
            "        lexeme_wit = '"+query+"[' " +
            "       ]" +
            "      ]"
        );

        List<Integer> clauses = new ArrayList<Integer>();

        int count = 0;
        
        SheafConstIterator sci = sheaf.const_iterator();
        while (sci.hasNext())
        {
            Straw straw = sci.next();

            count++;
            if (count > limit) continue;

            MatchedObject clause = straw.const_iterator().next();
            clauses.add(new Integer(clause.getID_D()));
        }
        
        String mql = "SELECT ALL OBJECTS IN " +
            m_Emdros.getVisibleMonads().toString() + " " +
            "WHERE " +
            "[verse GET book, chapter, verse, verse_label " +
            " [clause ";
        
        if (clauses.size() == 0)
        {
            mql += "self = -1"; // never matches
        }
        else
        {
            for (Iterator<Integer> i = clauses.iterator(); i.hasNext();)
            {
                Integer clauseId = i.next();
                mql += "self = " + clauseId.toString();
                if (i.hasNext())
                {
                    mql += " OR ";
                }
            }
        }

        mql += "[word GET lexeme_wit, " +
            new HebrewMorphemeGenerator().getRequiredFeaturesString(false) +
            "]]]";

        sheaf = m_Emdros.getSheaf(mql);
        sci = sheaf.const_iterator();
        
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

                String original = "";
                String translit = "";
                
                SheafConstIterator word_iter = 
                    clause.getSheaf().const_iterator();
                    
                while (word_iter.hasNext())
                {
                    MatchedObject word = 
                        word_iter.next().const_iterator().next();
                    
                    String lexeme = word.getEMdFValue("lexeme_wit").getString();
                    
                    boolean isMatch = lexeme.equals(query) ||
                        lexeme.equals(query + "/") ||
                        lexeme.equals(query + "[");
                    
                    if (isMatch)
                    {
                        original += "<strong>";
                        translit += "<strong>";
                    }
                    
                    original += HebrewConverter.wordHebrewToHtml  (word, generator);
                    translit += HebrewConverter.wordTranslitToHtml(word,
                        generator, m_Transliterator);
                    
                    if (isMatch)
                    {
                        original += "</strong>";
                        translit += "</strong>";
                    }
            
                    original += " ";
                    translit += " ";
                }

                assertTrue("Expected to find clause " + clause.getID_D() +
                    " but was missing", actualIterator.hasNext());
                SearchResult actual = actualIterator.next();
                assertEquals(verse.getEMdFValue("verse_label").getString(),
                    actual.getLocation());
                assertEquals(actual.getLocation(), 
                    "<div class=\"hebrew\">" + original + "</div>\n" +
                    "<div class=\"translit\">" + translit + "</div>\n",
                    actual.getDescription());
                assertEquals(actual.getLocation(),
                    "clause.jsp?book=" + 
                    m_Emdros.getEnumConstNameFromValue("book_name_e",
                        verse.getEMdFValue("book").getInt()) +
                    "&amp;chapter=" + verse.getEMdFValue("chapter") +
                    "&amp;verse="   + verse.getEMdFValue("verse") +
                    "&amp;clause="  + clause.getID_D(),
                    actual.getLinkUrl()
                );
            }

            assertEquals(count, search.getResultCount());
        }
        
        assertFalse("Found " + (actualResults.size() - count + 1) +
            " more results than expected", actualIterator.hasNext());
    }

    public void testSearchCode1() throws Exception
    {
        Search search = new Search(m_Emdros, m_Transliterator);
        assertSearchResultsMatch("CMJM", search, 100);
    }
    
    public void testSearchCode() throws Exception
    {
        assertSearchResultsMatch("CMJM", 1); // noun
        assertSearchResultsMatch("CMJM");    // noun
        assertSearchResultsMatch("BR");      // verb
        assertSearchResultsMatch("W", 0);    // conjunction
        assertSearchResultsMatch("W", 1);    // conjunction
        assertSearchResultsMatch("W", 10);   // conjunction
        assertSearchResultsMatch("W", 100);  // conjunction
        assertSearchResultsMatch("foo", 0);  // no match
        assertSearchResultsMatch("foo", 1);  // no match
        assertSearchResultsMatch("foo");     // no match
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
        i.assertStart("span", 
            new Attributes().style("font-size: medium"));
        i.assertText("by");
        i.assertSimple("a", "Chris Wilson", 
            new Attributes().href("http://www.qwirx.com"));
        i.assertEnd("span");
        i.assertEnd("h1");
        
        i.assertStart("h2");
        i.assertText("RLM for BH:");
        i.assertSimple("a", "Nicolai Winther-Nielsen",
            new Attributes().href("http://3bm.dk/index.php?p=3"));
        i.assertEnd("h2");

        i.assertStart("div", new Attributes().clazz("topmenu"));
        i.assertSimple("a", "Home", 
            new Attributes().clazz("index_jsp").href("index.jsp"));
        i.assertSimple("a", "Databases", 
            new Attributes().clazz("db_jsp").href(null));
        i.assertSimple("a", "Published", 
            new Attributes().clazz("published_jsp").href("published.jsp"));
        i.assertSimple("a", "Browse", 
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
        i.assertSimple("a", "Wordnet", 
            new Attributes().clazz("wordnet_jsp").href("wordnet.jsp"));
        */
        i.assertSimple("a", "Login", 
            new Attributes().clazz("login_jsp").href("login.jsp?next=search.jsp"));
        i.assertEmpty("div", new Attributes().clazz("clearer"));
        i.assertEnd("div", new Attributes().clazz("topmenu"));
        
        i.assertEmpty("script", new Attributes().type("text/javascript"));
        
        i.assertStart("form", new Attributes().method("GET").clazz("bigborder"));
        i.assertSimple("p", "Simple search (enter surface consonants " +
                "for a Hebrew word):");
        i.assertStart("p");
        i.assertEmpty("input", new Attributes().name("q"));
        i.assertEmpty("input", new Attributes().type("submit").value("Search"));
        i.assertEnd("p");
        
        i.assertEmpty("hr");
        
        i.assertSimple("p", "Advanced search (enter an MQL query to nest " +
            "within [clause]):"); 
        i.assertStart("p");
        i.assertEmpty("input", new Attributes().name("aq").add("size", "40"));
        i.assertSubmit("advanced", "Search");
        i.assertEnd("p");
        
        i.assertEmpty("hr");
        
        i.assertStart("div", 
            new Attributes().add("id", "simple_adv_div").clazz("advanced"));
        
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
        /*
        String hebrew = HebrewConverter.toHebrew(latin);
        String trans  = HebrewConverter.toTranslit(latin, m_Transliterator);
        */
        
        i.assertStart("table", new Attributes().clazz("grid"));
        
        i.assertStart("tr");
        i.assertSimple("th", "Consonants");
        for (int j = 0; j < latin.length(); j++)
        {
            String c = latin.substring(j, j + 1);
            i.assertSimple("td", c);
        }
        i.assertEnd("tr");
        
        /*
        i.assertStart("tr");
        i.assertSimple("th", "Hebrew");
        for (int j = 0; j < latin.length(); j++)
        {
            String c = latin.substring(j, j + 1);
            String h = HebrewConverter.toHebrew(c);
            i.assertSimple("td", h, new Attributes().clazz("hebrew"));
        }
        i.assertEnd("tr");

        i.assertStart("tr");
        i.assertSimple("th", "Transliteration");
        for (int j = 0; j < latin.length(); j++)
        {
            String c = latin.substring(j, j + 1);
            String h = HebrewConverter.toTranslit(c);
            i.assertSimple("td", h);
        }
        i.assertEnd("tr");
        */

        i.assertEnd("table");
        i.assertEnd("div", new Attributes().clazz("advanced"));
        
        i.assertStart("div");
        i.assertInput("simple_adv_btn", "Advanced Options ")
            .assertAttribute("type", "button")
            .assertAttribute("onclick", "toggle(this, 'simple_adv_div')");
        i.assertEnd("div");
        i.assertEnd("form");
        
        i.assertEmpty("script", new Attributes().type("text/javascript"));

        i.assertEmpty("hr");
        i.assertStart("p");
        i.assertText("&copy;");
        i.assertSimple("a", "Lex Project", 
            new Attributes().href("http://rrg.qwirx.com/trac/lex"));
        i.assertText("2005-2008.");
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

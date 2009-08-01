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

public class SearchTest extends LexTestBase
{
    public SearchTest() throws Exception
    {
        
    }

    private DatabaseTransliterator m_Transliterator;
    
    public void setUp() throws Exception
    {
        m_Transliterator = new DatabaseTransliterator(getSql());
    }
    
    private void assertSearchResultsMatch(String query)
    throws Exception
    {
        Search search = new Search(getEmdros(), m_Transliterator);
        assertSearchResultsMatch(query, search, 100);
    }
    
    private void assertSearchResultsMatch(String query, int limit)
    throws Exception
    {
        Search search = new Search(getEmdros(), m_Transliterator);
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
        
        Sheaf sheaf = getEmdros().getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            getEmdros().getVisibleMonads().toString() + " " +
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
            getEmdros().getVisibleMonads().toString() + " " +
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

        sheaf = getEmdros().getSheaf(mql);
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
                    getEmdros().getEnumConstNameFromValue("book_name_e",
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
        Search search = new Search(getEmdros(), m_Transliterator);
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

        assertPageHeader(i);
        
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
        
        List books = getEmdros().getEnumerationConstantNames("book_name_e");
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

        assertPageFooter(i);
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(SearchTest.class);
    }

}

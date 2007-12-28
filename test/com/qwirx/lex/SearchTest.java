package com.qwirx.lex;

import junit.framework.TestCase;

import org.aptivate.webutils.HtmlIterator;
import org.aptivate.webutils.HtmlIterator.Attributes;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.qwirx.lex.hebrew.HebrewConverter;

public class SearchTest extends TestCase
{
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
        
        i.assertStart("form", new Attributes().method("POST").clazz("bigborder"));
        i.assertSimple("p", "Search for Hebrew word by root " +
                "(surface consonants):");
        i.assertStart("p");
        i.assertEmpty("input", new Attributes().name("q"));
        i.assertEmpty("input", new Attributes().type("submit").value("Search"));
        i.assertEnd("p");
        i.assertSimple("p", "Three Latin consonants are expected, in upper " +
                "or lower case, from the set:");
        String latin = ">BGDHWZXVJKLMNS<PYQRFCT";
        String hebrew = HebrewConverter.toHebrew(latin);
        i.assertStart("table", new Attributes().add("border", "1"));
        i.assertStart("tr");
        for (int j = 0; j < latin.length(); j++)
        {
            String c = latin.substring(j, j + 1);
            i.assertSimple("th", c);
        }
        i.assertEnd("tr");
        i.assertStart("tr");
        for (int j = 0; j < hebrew.length(); j++)
        {
            String c = hebrew.substring(j, j + 1);
            i.assertSimple("th", c);
        }
        i.assertEnd("tr");
        i.assertEnd("table");
        i.assertEnd("form");

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

package com.qwirx.lex;

import java.util.HashMap;
import java.util.Map;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.Table;
import jemdros.TableIterator;
import jemdros.TableRow;
import junit.framework.TestCase;

import org.aptivate.web.utils.HtmlIterator;
import org.aptivate.web.utils.HtmlIterator.Attributes;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;

public class LexTestBase extends TestCase
{
    private SqlDatabase m_SQL;
    private EmdrosDatabase m_Emdros;
    
    protected LexTestBase() throws Exception
    {
        m_SQL = Lex.getSqlDatabase("test");
        m_Emdros = Lex.getEmdrosDatabase("test", "test", m_SQL);
    }
    
    protected SqlDatabase getSql() { return m_SQL; }
    protected EmdrosDatabase getEmdros() { return m_Emdros; }
    
    protected EmdrosDatabase getUserEmdrosDatabase(String user)
    throws Exception
    {
        return Lex.getEmdrosDatabase(user, "test", m_SQL);
    }
    
    protected Map<String, Integer> m_Location = new HashMap<String, Integer>();

    protected MatchedObject getNestedObject(Sheaf sheaf)
    throws EmdrosException
    {
        SheafConstIterator sci = sheaf.const_iterator();
        
        if (sci.hasNext()) 
        {
            Straw straw = sci.next();
            StrawConstIterator swci = straw.const_iterator();
            if (swci.hasNext()) 
            {
                return swci.next();
            }
        }

        return null;
    }
    
    private void setLocation(SetOfMonads monads) throws Exception
    {
        Table table = getEmdros().getTable("SELECT OBJECTS HAVING MONADS IN " + 
            monads.toString() + " [ALL]");
        for (TableIterator ti = table.iterator(); ti.hasNext();)
        {
            TableRow row = ti.next();
            String type = row.getColumn(1);
            Integer id_d = Integer.valueOf(row.getColumn(3));
            m_Location.put(type, Integer.valueOf(id_d));
        }        
    }
    
    protected MatchedObject getWord(int monad)
    throws Exception
    {
        HebrewMorphemeGenerator hmg = new HebrewMorphemeGenerator();
        String query = "SELECT ALL OBJECTS IN {" + monad + "} " +
            "WHERE [word GET " + hmg.getRequiredFeaturesString(true) + "]";

        Sheaf sheaf = getEmdros().getSheaf(query);
        MatchedObject word = getNestedObject(sheaf);
        
        setLocation(new SetOfMonads(monad));
        
        return word;
    }

    protected SetOfMonads getClauseMonads(int clauseId)
    throws Exception
    {
        String query = "GET MONADS FROM OBJECTS WITH ID_DS = " + clauseId +
            "[clause]";
        
        Table monads = getEmdros().getTable(query);
        SetOfMonads som = new SetOfMonads();
        for (TableIterator ti = monads.iterator(); ti.hasNext();)
        {
            TableRow row = ti.next();
            som.add(Integer.parseInt(row.getColumn(2)),
                Integer.parseInt(row.getColumn(3)));
        }
        
        return som;
    }
    
    protected MatchedObject getClause(int clauseId, String [] features)
    throws Exception
    {
        String query = "SELECT ALL OBJECTS IN " + 
            getClauseMonads(clauseId).toString() + 
            " WHERE [clause";
        if (features.length > 0)
        {
            for (int i = 0; i < features.length; i++)
            {
                if (i == 0)
                {
                    query += " GET ";
                }
                else
                {
                    query += ", ";
                }
                query += features[i];
            }
        }
        query += "]";

        Sheaf sheaf = getEmdros().getSheaf(query);
        MatchedObject clause = getNestedObject(sheaf);
        setLocation(clause.getMonads());
        
        return clause;
    }
    
    protected void assertPageHeader(HtmlIterator i)
    {
        i.assertDTD("html", "-//W3C//DTD XHTML 1.0 Strict//EN",
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
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
    }
    
    protected void assertPageFooter(HtmlIterator i)
    {
        i.assertEmpty("hr");
        i.assertStart("p");
        i.assertText("&copy;");
        i.assertSimple("a", "Lex Project", 
            new Attributes().href("http://rrg.qwirx.com/trac/lex"));
        i.assertText("2005-2009.");
        i.assertEnd("p");
        i.assertEnd("body");
        i.assertEnd("html");
        i.assertEndDocument();
    }
}

package com.qwirx.lex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jemdros.FlatSheaf;
import jemdros.FlatSheafConstIterator;
import jemdros.FlatStraw;
import jemdros.FlatStrawConstIterator;
import jemdros.MatchedObject;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;

import org.aptivate.web.utils.HtmlIterator;

import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.qwirx.db.Change;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.lex.controller.ClauseController;
import com.qwirx.lex.controller.ClauseController.Cell;
import com.qwirx.lex.emdros.EmdrosChange;
import com.qwirx.lex.parser.MorphEdge;

public class ClauseControllerTest extends LexTestBase
{
    public ClauseControllerTest() throws Exception { }
    
    private void assertGlossCell(Cell cell, MatchedObject word)
    throws Exception
    {
        HtmlIterator i = new HtmlIterator("<td>" + cell.html + "</td>");
        i.assertStart("td");
        i.assertSimple("p", "[Wivu] hover");
        i.assertStart("form").assertAttribute("method", "post")
            .assertAttribute("name", "wage_28")
            .assertAttribute("class", "blue");
        i.assertText("Or enter a replacement:");
        i.assertFormHidden("wagw", "28");
        i.assertFormTextField("wagv", 
            word.getEMdFValue("wivu_alternate_gloss").getString());
        i.assertFormSubmit("wags", "Save");
        i.assertEnd("form");
        i.assertEnd("td");
        i.assertEndDocument();   
    }

    private WebConversation assertLogIn() throws Exception
    {
        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
        WebConversation wc = new WebConversation();
        WebResponse resp = wc.getResponse("http://localhost:8080/lex/clause.jsp");

        wc.setExceptionsThrownOnErrorStatus(false);
        resp = wc.getResponse("http://localhost:8080/lex/" +
                "login.jsp?next=clause.jsp");
        assertEquals(401, resp.getResponseCode());
        
        wc.setExceptionsThrownOnErrorStatus(true);
        wc.setAuthorization("test", "test");
        resp = wc.getResponse("http://localhost:8080/lex/" +
                "login.jsp?next=clause.jsp");

        return wc;
    }
    
    public void testWivuGlossCell() throws Exception
    {
        ClauseController controller = new ClauseController(getEmdros(),
            getSql(), 28740);

        MatchedObject word = getWord(27); // merahefet
        Change ch = getEmdros().createChange(EmdrosChange.UPDATE, "word", 
            new int[]{word.getID_D()});
        ch.setString("wivu_alternate_gloss", "");
        ch.execute();
        
        word = getWord(27);
        String gloss = controller.getWivuGloss(word);
        assertEquals("hover", gloss);

        Cell cell = controller.getWivuLexiconCell(word, true);
        assertGlossCell(cell, word);
        
        WebConversation wc = assertLogIn();        
        WebResponse resp = wc.getResponse(
            "http://localhost:8080/lex/clause.jsp" +
            "?book="    + m_Location.get("book") +
            "&chapter=" + m_Location.get("chapter") +
            "&verse="   + m_Location.get("verse") +
            "&clause="  + m_Location.get("clause"));
        WebForm glossForm = resp.getFormWithName("wage_28");
        assertNotNull(glossForm);
        
        glossForm.setParameter("wagv", "Hello");
        glossForm.submit();

        word = getWord(27);
        assertEquals("Hello", 
            word.getEMdFValue("wivu_alternate_gloss").getString());

        cell = controller.getWivuLexiconCell(word, true);
        assertGlossCell(cell, word);
        assertEquals("Hello", controller.getWivuGloss(word));
        
        HtmlIterator i = new HtmlIterator("<td>" + 
            controller.getWivuLexiconCell(word, false).html + "</td>");
        i.assertStart("td");
        i.assertStart("p");
        i.assertText("[Override]");
        i.assertSimple("strong", "Hello");
        i.assertEnd("p");
        i.assertSimple("p", "[Wivu] hover");
        i.assertEnd("td");
        i.assertEndDocument();
    }
    
    // There should be NO space after certain morphemes, indicated
    // by a dash in the WIT transliteration, or certain types of prepositions
    // and articles
    public void testGraphicalLexicalMorphemeDifference()
    throws Exception
    {
        String expected = "בְּרֵאשִׁ֖ית בָּרָ֣א אֱלֹהִ֑ים אֵ֥ת הַשָּׁמַ֖יִם וְאֵ֥ת הָאָֽרֶץ "; // Gen 1,1a
        ClauseController controller = new ClauseController(getEmdros(),
            getSql(), 28737);
        assertEquals(expected, controller.getHebrewText());
    }
    
    private ClauseController getController(int clauseId)
    throws Exception
    {
        return new ClauseController(getEmdros(), getSql(), clauseId);
    }
    
    public void testGloss() throws Exception
    {
        ClauseController controller = getController(28951); // Gen 2,24(b)
        List<Cell[]> columns = controller.getWordColumns();
        String [] firstRow = new String[]{
            "wᵊ=", "Ø-", "Ø-", "dāvaq-", "Ø=", "Ø", "", "bᵊ=", "ʔiš-", "t=", "ô"
        };
        String [] secondRow = new String[]{
            "CLM", "SEQU", "Qa", "cling", "3Msg", "CLT", "", "P", "woman",
            "FsgCS", "3Msg"
        };
        assertEquals(firstRow.length, secondRow.length);
        for (int i = 0; i < firstRow.length; i++)
        {
            assertEquals("" + i, firstRow[i],  columns.get(i)[0].html);
            assertEquals("" + i, secondRow[i], columns.get(i)[1].html);
        }
        assertEquals(firstRow.length, columns.size());
        
        controller = getController(593885); // IKON11,2(f)
        columns = controller.getWordColumns();
        assertEquals("P",    columns.get(0)[1].html);
        assertEquals("3Mpl",  columns.get(1)[1].html);
        assertEquals("",      columns.get(2)[1].html);
        assertEquals("PERF", columns.get(3)[1].html);
        assertEquals("Qa", columns.get(4)[1].html);
        assertEquals("cling", columns.get(5)[1].html);
        assertEquals("3Msg", columns.get(6)[1].html);
        assertEquals("CLT", columns.get(7)[1].html);
        assertEquals("", columns.get(8)[1].html);
        assertEquals("Solomon", columns.get(9)[1].html);
        assertEquals("MsgAB", columns.get(10)[1].html);
        assertEquals("CLT", columns.get(11)[1].html);
        assertEquals("", columns.get(12)[1].html);
        assertEquals("P", columns.get(13)[1].html);
        assertEquals("love", columns.get(14)[1].html);
        assertEquals("āʰ=", columns.get(15)[0].html);
        assertEquals("FsgAB", columns.get(15)[1].html);
        assertEquals("CLT", columns.get(16)[1].html);
        assertEquals(17, columns.size());
    }

    public void testMorphemeGeneration() throws Exception
    {
        ClauseController controller = getController(28951); // Gen 2,24(b))
        List<MorphEdge> morphEdges = controller.getMorphEdges();

        assertEquals("CONJ",     morphEdges.get(0).symbol());
        assertEquals("V/TAM",    morphEdges.get(1).symbol());
        assertEquals("V/STM",    morphEdges.get(2).symbol());
        assertEquals("V/NUC",    morphEdges.get(3).symbol());
        assertEquals("AG/PSA",   morphEdges.get(4).symbol());
        assertEquals("PRON/DCA", morphEdges.get(5).symbol());
        assertEquals("P",        morphEdges.get(6).symbol());
        assertEquals("N/NUC",    morphEdges.get(7).symbol());
        assertEquals("N/GNS",    morphEdges.get(8).symbol());
        assertEquals("N/POS",    morphEdges.get(9).symbol());
    }
    
    private void assertPublishedPage(WebResponse resp, int expectedClauseId,
        boolean expectFound) throws Exception
    {
        HtmlIterator i = new HtmlIterator(resp.getText());
        assertPageHeader(i, "Published Data", "published");
        i.assertSimple("h3", "Published Clauses");
        
        Sheaf sheaf = getEmdros().getSheaf("SELECT ALL OBJECTS IN " +
            "{" + getEmdros().getMinM() + "-" + getEmdros().getMaxM() + "} " +
            "WHERE [clause published = 1 GET predicate, logical_structure]");
        
        List<MatchedObject> results = new ArrayList<MatchedObject>();
        Map<Integer, String> clauseToVerseMap = new HashMap<Integer, String>();
        
        boolean expectedClauseFound = false;
        
        for (SheafConstIterator sci = sheaf.const_iterator(); sci.hasNext();)
        {
            Straw straw = sci.next();
            for (StrawConstIterator swci = straw.const_iterator();
                swci.hasNext();)
            {
                MatchedObject clause = swci.next();
                
                if (clause.getID_D() == expectedClauseId)
                {
                    expectedClauseFound = true;
                }
                
                results.add(clause);
                
                FlatSheaf fs = getEmdros().getFlatSheaf("GET OBJECTS " +
                        "HAVING MONADS IN " + clause.getMonads().toString() +
                        " [verse GET verse_label]");
                for (FlatSheafConstIterator fsci = fs.const_iterator(); 
                    fsci.hasNext();)
                {
                    FlatStraw fsw = fsci.next();
                    for (FlatStrawConstIterator fswci = fsw.const_iterator();
                        fswci.hasNext();)
                    {
                        MatchedObject verse = fswci.next();
                        
                        clauseToVerseMap.put(clause.getID_D(),
                            verse.getEMdFValue("verse_label").getString()
                            .replaceFirst("^ ", ""));
                    }
                }
            }
        }
        
        assertEquals(expectFound, expectedClauseFound);
        
        i.assertSimple("h4", "Displaying first " + results.size() + " of " +
            results.size() + " results.");
        
        i.assertStart("table").assertAttribute("class", "search_results");
        i.assertStart("tr");
        i.assertSimple("th", "Verb");
        i.assertSimple("th", "Logical Structure");
        i.assertSimple("th", "Reference");
        i.assertEnd("tr");
        
        for (MatchedObject clause : results)
        {
            i.assertStart("tr");
            i.assertSimple("td", clause.getEMdFValue("predicate").toString());
            i.assertSimple("td", clause.getEMdFValue("logical_structure").toString());
            i.assertStart("td");
            i.assertSimple("a", clauseToVerseMap.get(clause.getID_D()));
            i.assertEnd("td");
            i.assertEnd("tr");
        }
        
        i.assertEnd("table");
        
        assertPageFooter(i);
    }
    
    public void testPublishFeature() throws Exception
    {
        final int clauseId = 121803; // EXO 01,01(a)
        MatchedObject clause = getClause(clauseId, new String[]{"published"});

        Change ch = getSql().createChange(SqlChange.DELETE,
            "user_text_access", 
            "Monad_First = " + clause.getMonads().first() + " AND " +
            "Monad_Last = " + clause.getMonads().last() + " AND " + 
            "User_Name = \"test\"");
        ch.execute();

        ch = getSql().createChange(SqlChange.INSERT,
            "user_text_access", null);
        ch.setInt("Monad_First", clause.getMonads().first());
        ch.setInt("Monad_Last",  clause.getMonads().last());
        ch.setString("User_Name",   "test");
        ch.setInt("Write_Access", 1);
        ch.execute();

        ch = getSql().createChange(SqlChange.DELETE,
            "user_text_access", 
            "Monad_First = " + clause.getMonads().first() + " AND " +
            "Monad_Last = " + clause.getMonads().last() + " AND " + 
            "User_Name = \"anonymous\"");
        ch.execute();

        if (clause.getFeatureAsLong(0) != 0)
        {
            ch = getEmdros().createChange(EmdrosChange.UPDATE, "clause", 
                new int[]{clauseId});
            ch.setInt("published", 0);
            ch.execute();
        }
        
        SetOfMonads visible = getUserEmdrosDatabase("anonymous").getVisibleMonads();
        SetOfMonads overlap = SetOfMonads.intersect(visible,
            clause.getMonads());
        assertTrue(overlap.toString(), overlap.isEmpty());
        
        WebConversation anon = new WebConversation();
        WebResponse response = anon.getResponse("http://localhost:8080/lex/" +
                "published.jsp");
        assertPublishedPage(response, clauseId, false);

        WebConversation loggedin = assertLogIn();        
        loggedin.getResponse(
            "http://localhost:8080/lex/clause.jsp" +
            "?book="    + m_Location.get("book") +
            "&chapter=" + m_Location.get("chapter") +
            "&verse="   + m_Location.get("verse") +
            "&clause="  + m_Location.get("clause") +
            "&publish=Publish");
        
        clause = getClause(clauseId, new String[]{"published"});
        assertEquals(1, clause.getFeatureAsLong(0));
        
        visible = getUserEmdrosDatabase("anonymous").getVisibleMonads();
        overlap = SetOfMonads.intersect(visible, clause.getMonads());
        assertFalse(overlap.toString(), overlap.isEmpty());
        assertEquals(clause.getMonads().toString(), overlap.toString());
        
        response = anon.getResponse("http://localhost:8080/lex/" +
            "published.jsp");
        assertPublishedPage(response, clauseId, true);
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ClauseControllerTest.class);
    }
}

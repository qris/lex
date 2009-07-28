package com.qwirx.lex;

import java.util.List;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;

import org.aptivate.webutils.HtmlIterator;

import com.qwirx.lex.controller.ClauseController;
import com.qwirx.lex.controller.ClauseController.Cell;
import com.qwirx.lex.parser.MorphEdge;

public class ClauseControllerTest extends LexTestBase
{
    public ClauseControllerTest() throws Exception { }
    
    private MatchedObject getWord(int monad)
    throws Exception
    {
        StringBuffer query = new StringBuffer();
        query.append("SELECT ALL OBJECTS IN {" + monad + "} " +
            "WHERE [word GET lexeme_wit, wivu_lexicon_id, " +
            "phrase_dependent_part_of_speech, stem]");
        Sheaf sheaf = getEmdros().getSheaf(query.toString());
        
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

    public void testWivuGlossCell() throws Exception
    {
        ClauseController controller = new ClauseController(getEmdros(),
            getSql(), 28740);
        MatchedObject word = getWord(27); // merahefet
        String gloss = controller.getWivuGloss(word);
        assertEquals("hover", gloss);
        
        Cell glossCell = controller.getWivuLexiconCell(word, true);
        HtmlIterator i = new HtmlIterator(glossCell.html);
        
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
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ClauseControllerTest.class);
    }
}

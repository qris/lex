package com.qwirx.lex;

import java.util.List;

import junit.framework.TestCase;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.controller.ClauseController;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.parser.MorphEdge;

public class HebrewConverterTest extends TestCase
{
    SqlDatabase m_SQL;
    EmdrosDatabase m_Emdros;

    public void setUp() throws Exception
    {
        m_SQL = Lex.getSqlDatabase("test");
        m_Emdros = Lex.getEmdrosDatabase("test", "test", m_SQL);
    }
    
    private ClauseController getController(int clauseId)
    throws Exception
    {
        return new ClauseController(m_Emdros, m_SQL, clauseId);
    }
    
    public void testGloss() throws Exception
    {
        ClauseController controller = getController(28951); // Gen 2,24(b)
        List<String[]> columns = controller.getWordColumns();
        assertEquals("CLM",  columns.get(0)[1]);
        assertEquals("", columns.get(1)[1]);
        assertEquals("SER2-", columns.get(2)[1]);

        controller = getController(593885); // IKON11,2(f)
        columns = controller.getWordColumns();
        assertEquals("P-",    columns.get(0)[1]);
        assertEquals("3Mpl",  columns.get(1)[1]);
        assertEquals("",      columns.get(2)[1]);
        assertEquals("PERF-", columns.get(3)[1]);
        assertEquals("Qa-", columns.get(4)[1]);
        assertEquals("cling-", columns.get(5)[1]);
        assertEquals("3Msg-", columns.get(6)[1]);
        assertEquals("SUFF", columns.get(7)[1]);
        assertEquals("", columns.get(8)[1]);
        assertEquals("Solomon-", columns.get(9)[1]);
        assertEquals("MsgAB-", columns.get(10)[1]);
        assertEquals("SUFF", columns.get(11)[1]);
        assertEquals("", columns.get(12)[1]);
        assertEquals("P", columns.get(13)[1]);
        assertEquals("", columns.get(14)[1]);
        assertEquals("love-", columns.get(15)[1]);
        assertEquals("FsgAB-", columns.get(16)[1]);
        assertEquals("SUFF", columns.get(17)[1]);
        assertEquals(18, columns.size());
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
        junit.textui.TestRunner.run(HebrewConverterTest.class);
    }
}

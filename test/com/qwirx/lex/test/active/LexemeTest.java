package com.qwirx.lex.test.active;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jemdros.BadMonadsException;
import jemdros.SetOfMonads;
import junit.framework.TestCase;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.qwirx.db.Change;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.Lex;
import com.qwirx.lex.emdros.EmdrosChange;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.emdros.EmdrosChange.MonadSetEntry;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.lexicon.ThematicRelation;
import com.qwirx.lex.lexicon.Lexeme.Variable;
import com.qwirx.lex.translit.DatabaseTransliterator;

public final class LexemeTest extends TestCase
{
    private SqlDatabase sql;
    private EmdrosDatabase emdros;
    
    private List<Lexeme> addedLexemes = new ArrayList<Lexeme>();
    private List<EmdrosObject> addedObjects = new ArrayList<EmdrosObject>();
    
    public LexemeTest(String name) throws Exception
    {
        super(name);
        sql    = Lex.getSqlDatabase   ("test");
        emdros = Lex.getEmdrosDatabase("test", "localhost", sql);
    }
    
    class EmdrosObject 
    {
        String type;
        int id;
        public EmdrosObject(String type, int id)
        {
            this.type = type;
            this.id   = id;
        }
    }
    
    private static final int m_FirstMonad = 1000000;
    private static final int m_LastMonad  = 1000100;
    
    public void setUp() throws Exception
    {
        if (sql.getSingleInteger("SELECT COUNT(1) FROM user_text_access " +
            "WHERE User_Name = \"test\" AND Monad_First = " + m_FirstMonad +
            " AND Monad_Last = " + m_LastMonad + " AND Write_Access = 1") == 0)
        {
            Change ch = sql.createChange(SqlChange.INSERT,
                "user_text_access", null);
            ch.setString("User_Name", "test");
            ch.setInt("Monad_First", m_FirstMonad);
            ch.setInt("Monad_Last",  m_LastMonad);
            ch.setInt("Write_Access", 1);
            ch.execute();
        }
    }

    public void tearDown()
    {
        for (Iterator i = addedLexemes.iterator(); i.hasNext(); )
        {
            Lexeme lexeme = (Lexeme)( i.next() );
            try 
            {
                lexeme.delete();
            }
            catch (Exception e)
            {
                System.err.println("Failed to delete lexeme "+lexeme+": "+e);
            }
        }
        
        addedLexemes.clear();
        
        for (Iterator i = addedObjects.iterator(); i.hasNext(); )
        {
            EmdrosObject obj = (EmdrosObject)( i.next() );
            try
            {
                emdros.createChange(EmdrosChange.DELETE,
                    obj.type, new int[]{obj.id})
                    .execute();
            }
            catch (Exception e)
            {
                System.err.println("Failed to delete Emdros object "+obj.type+
                    " "+obj.id+": "+e);
            }
        }
        
        addedObjects.clear();
    }
    
    private void assertEquals(Lexeme a, Lexeme b)
    {
        assertEquals(a.getID(), b.getID());
        assertEquals(a.getLogicalStructure(), b.getLogicalStructure());
        assertEquals(a.isCaused(),            b.isCaused());
        assertEquals(a.isPunctual(),          b.isPunctual());
        assertEquals(a.hasResultState(),      b.hasResultState());
        assertEquals(a.isTelic(),             b.isTelic());
        assertEquals(a.getPredicate(),        b.getPredicate());
        assertEquals(a.getThematicRelation(), b.getThematicRelation());
        assertEquals(a.isDynamic(),           b.isDynamic());
        assertEquals(a.hasEndpoint(),         b.hasEndpoint());
        assertEquals(a.getResultPredicate(),  b.getResultPredicate());
    }

    private void checkCheckbox(WebForm form, String name, 
        boolean expectedValue)
    {
        assertTrue(form.hasParameterNamed(name));
        assertEquals(expectedValue ? "1" : null, 
            form.getParameterValue(name));
        
    }

    private void checkEditPageFormShowsCurrentValues(Lexeme expected) throws Exception
    {
        assertNotNull(expected);

        String url = "http://localhost:8080/lex/lexicon.jsp?lsid=" + 
            expected.getID();
        WebConversation conv = new WebConversation();
        conv.getClientProperties().setAcceptGzip(false);
        WebResponse resp = conv.getResponse(url);
        
        WebForm form = resp.getFormWithName("lsform");
        assertNotNull(form);

        assertEquals(expected.getID() == 0 ? "" : expected.getID() + "", 
            form.getParameterValue("lsid"));
        checkCheckbox(form, "ls_caused", expected.isCaused());
        checkCheckbox(form, "ls_punct",  expected.isPunctual());
        assertEquals(expected.hasResultState() ? "1" : "0", 
            form.getParameterValue("ls_punct_result"));
        checkCheckbox(form, "ls_telic",  expected.isTelic());
        assertEquals(expected.isDynamic() ? "1" : "0", 
            form.getParameterValue("ls_dynamic"));
        assertEquals(expected.hasEndpoint() ? "1" : "0", 
            form.getParameterValue("ls_endpoint"));

        assertEquals(expected.getPredicate() == null ? "" 
            : expected.getPredicate(), form.getParameterValue("ls_pred"));
        assertEquals(expected.getResultPredicate() == null ? "" 
            : expected.getResultPredicate(), form.getParameterValue("ls_pred_2"));

        if (expected.getThematicRelation() == null)
        {
            assertEquals("", form.getParameterValue("ls_trel"));
        }
        else
        {
            int trelIndex = Integer.parseInt(form.getParameterValue("ls_trel"));
            assertEquals(expected.getThematicRelation(),
                ThematicRelation.list()[trelIndex]);
        }
        
        assertEquals(expected.getLogicalStructure(), 
            form.getParameterValue("ls"));
    }
    
    private void doUseEditPageFormToSetValues(Lexeme lexeme) 
    throws Exception
    {
        assertNotNull(lexeme);
        
        String url = "http://localhost:8080/lex/lexicon.jsp?lsid=" + 
            lexeme.getID();
        WebConversation conv = new WebConversation();
        conv.getClientProperties().setAcceptGzip(false);
        WebResponse resp = conv.getResponse(url);
        WebForm form = resp.getFormWithName("lsform");
        assertNotNull(form);
        
        form.setCheckbox("ls_caused", lexeme.isCaused());
        form.setCheckbox("ls_punct",  lexeme.isPunctual());
        form.setParameter("ls_punct_result",  
            lexeme.hasResultState() ? "1" : "0");
        form.setCheckbox("ls_telic",  lexeme.isTelic());
        form.setParameter("ls_pred",   
            lexeme.getPredicate() == null ? "" : lexeme.getPredicate());
        form.setParameter("ls_pred_2",   
            lexeme.getResultPredicate() == null ? "" : 
            lexeme.getResultPredicate());
        
        if (lexeme.getThematicRelation() == null)
        {
            form.setParameter("ls_trel", "");
        }
        else
        {
            ThematicRelation[] rels = ThematicRelation.list();
            
            for (int i = 0; i < rels.length; i++)
            {
                if (lexeme.getThematicRelation() == rels[i])
                {
                    form.setParameter("ls_trel", i + "");
                    break;
                }
            }
        }        
        
        form.setParameter("ls_dynamic",  lexeme.isDynamic()   ? "1" : "0");
        form.setParameter("ls_endpoint", lexeme.hasEndpoint() ? "1" : "0");
        form.submit();
    }

    private Lexeme checkSaveAndLoad(Lexeme newValues) throws Exception
    {
        int id = newValues.getID();
        assertFalse("new values should belong to an existing lexeme", id == 0);
        
        Lexeme oldValues = Lexeme.load(sql, id);
        assertEquals(id, oldValues.getID());
        assertFalse("equals method did not detect change", 
            newValues.equals(oldValues));
        
        newValues.save();
        assertEquals(id, newValues.getID());
        
        Lexeme newCopy = Lexeme.load(sql, id);
        assertEquals(newValues, newCopy);
        assertTrue("equals method does not agree with assertEquals", 
            newCopy.equals(newValues));
        
        // revert to old version, and make the same changes using form
        oldValues.save();
        newCopy = Lexeme.load(sql, id);
        assertEquals(oldValues, newCopy);
        
        doUseEditPageFormToSetValues(newValues);
        newCopy = Lexeme.load(sql, id);
        assertEquals(newValues, newCopy);
        checkEditPageFormShowsCurrentValues(newValues);

        return newCopy;
    }
    
    public void testLexemeSaveAndLoadWorks() throws Exception
    {
        Lexeme t = new Lexeme(sql);
        assertEquals(0, t.getID());
        t.save();
        assertTrue(t.getID() != 0);
        addedLexemes.add(t);

        Lexeme t2 = Lexeme.load(sql, t.getID());
        assertEquals(t, t2);
        assertTrue(t2.equals(t));

        assertNull(t.getPredicate());
        doUseEditPageFormToSetValues(t);
        checkEditPageFormShowsCurrentValues(t);

        t.setPredicate("bar'");
        assertEquals("bar'", t.getPredicate());
        assertEquals("bar'", checkSaveAndLoad(t).getPredicate());
        assertEquals("bar'", t.getLogicalStructure());

        assertNull(t.getThematicRelation());
        t.setThematicRelation(ThematicRelation.list()[2]);
        assertEquals(ThematicRelation.list()[2], t.getThematicRelation());
        assertEquals(ThematicRelation.list()[2], 
            checkSaveAndLoad(t).getThematicRelation());
        assertEquals("bar'(<x>:LOCATION, <y>:THEME)", 
            t.getLogicalStructure());

        assertFalse(t.isCaused());
        t.setCaused(true);
        assertTrue(t.isCaused());
        assertTrue(checkSaveAndLoad(t).isCaused());
        assertEquals("do'(<x>, \u00D8) CAUSE bar'(<x>:LOCATION, <y>:THEME)", 
            t.getLogicalStructure());

        assertFalse(t.isPunctual());
        t.setPunctual(true);
        assertTrue(t.isPunctual());
        assertTrue(checkSaveAndLoad(t).isPunctual());
        assertEquals("do'(<x>, \u00D8) CAUSE SEMEL " +
                "bar'(<x>:LOCATION, <y>:THEME)", t.getLogicalStructure());

        assertFalse(t.hasResultState());
        t.setHasResultState(true);
        assertTrue(t.hasResultState());
        assertTrue(checkSaveAndLoad(t).hasResultState());
        assertEquals("do'(<x>, \u00D8) CAUSE INGR " +
                "bar'(<x>:LOCATION, <y>:THEME)", t.getLogicalStructure());

        assertFalse(t.isTelic());
        t.setTelic(true);
        assertTrue(t.isTelic());
        assertTrue(checkSaveAndLoad(t).isTelic());
        assertEquals("do'(<x>, \u00D8) CAUSE INGR BECOME " +
                "bar'(<x>:LOCATION, <y>:THEME)", t.getLogicalStructure());

        assertFalse(t.isDynamic());
        t.setDynamic(true);
        assertTrue(t.isDynamic());
        assertTrue(checkSaveAndLoad(t).isDynamic());
        assertEquals("do'(<x>, \u00D8) CAUSE INGR BECOME " +
            "do'(<x>, [bar'(<x>:LOCATION, <y>:THEME)])", 
            t.getLogicalStructure());

        assertFalse(t.hasEndpoint());
        t.setHasEndpoint(true);
        assertTrue(t.hasEndpoint());
        assertTrue(checkSaveAndLoad(t).hasEndpoint());        
        assertEquals("do'(<x>, \u00D8) CAUSE INGR BECOME " +
            "do'(<x>, [bar'(<x>:LOCATION, <y>:THEME)]) " +
            "& INGR (<x>)", t.getLogicalStructure());

        assertNull(t.getResultPredicate());
        t.setResultPredicate("baz'");
        assertEquals("baz'", t.getResultPredicate());
        assertEquals("baz'", checkSaveAndLoad(t).getResultPredicate());
        assertEquals("do'(<x>, \u00D8) CAUSE INGR BECOME " +
            "do'(<x>, [bar'(<x>:LOCATION, <y>:THEME)]) " +
            "& INGR baz'(<x>)", t.getLogicalStructure());
        
        Set<Variable> testVars = new TreeSet<Variable>();
        testVars.add(new Variable("x", "house'"));
        testVars.add(new Variable("y", "table'"));
        assertEquals("do'(house', \u00D8) CAUSE INGR BECOME " +
            "do'(house', [bar'(house':LOCATION, table':THEME)]) " +
            "& INGR baz'(house')", t.getEvaluatedLogicalStructure(testVars));
        
        EmdrosChange ch = (EmdrosChange)emdros.createChange(
            EmdrosChange.CREATE, "clause", null);
        ch.setMonads(new MonadSetEntry[]{
            new MonadSetEntry(1, 1)
        });
        ch.execute();
        int clauseAtom1 = ch.getInsertedRowId();
        addedObjects.add(new EmdrosObject("clause", clauseAtom1));
        
    }
    
    private Map makeMap(Object [] values)
    {
        Map<String, Object> result = new Hashtable<String, Object>();
        
        for (int i = 0; i < values.length; i += 2)
        {
            result.put((String)values[i], values[i+1]);
        }
        
        return result;
    }

    private int addEmdrosObject(String type, MonadSetEntry[] monads,
        String featureName, String featureValue)
    throws DatabaseException, BadMonadsException
    {
        EmdrosChange ch = (EmdrosChange)emdros.createChange(
            EmdrosChange.CREATE, type, null);
        ch.setMonads(monads);
        ch.setString(featureName, featureValue);
        ch.execute();
        int id = ch.getInsertedRowId();
        addedObjects.add(new EmdrosObject(type, id));
        return id;
    }

    private int addEmdrosObject(String type, int[] objectIds, 
        Map features)
    throws DatabaseException
    {
        EmdrosChange ch = (EmdrosChange)emdros.createChange(
            EmdrosChange.CREATE, type, objectIds);
       
        for (Iterator i = features.keySet().iterator(); i.hasNext(); )
        {
            String name  = (String)( i.next() );
            Object value = features.get(name);
            if (value instanceof Constant)
            {
                ch.setConstant(name, ((Constant)value).getName());
            }
            else
            {
                ch.setString(name, (String)value);
            }
        }
        
        ch.execute();
        
        int id = ch.getInsertedRowId();
        addedObjects.add(new EmdrosObject(type, id));
        return id;
    }
    
    class Constant
    {
        private String m_Name;
        public Constant(String name)
        {
            m_Name = name;
        }
        public Constant(int value)
        {
            m_Name = "" + value;
        }
        public String getName() { return m_Name; }
    }
    
    private int m_MonadCounter = 1000000;
    
    private int addClause(String [] words, Object [][] phrases,
        Integer logicalStructid)
    throws Exception
    {
        int [] phraseIds = new int [words.length];
        
        for (int i = 0; i < words.length; i++)
        {
            int wordId = addEmdrosObject("word", new MonadSetEntry[]{
                new MonadSetEntry(m_MonadCounter++)}, "lexeme", words[i]);        
            phraseIds[i] = addEmdrosObject("phrase", new int[]{wordId},
                makeMap(phrases[i]));
        }
        
        Map<String, Constant> clauseAttribs = new HashMap<String, Constant>();
        
        if (logicalStructid != null)
        {
            clauseAttribs.put("logical_struct_id", 
                new Constant(logicalStructid.intValue()));
        }
        
        return addEmdrosObject("clause", phraseIds, clauseAttribs);  
    }
    
    public void testEmdrosDatabaseUpdatedWithLogicalStructure()
    throws Exception
    {
        SetOfMonads monadsToClear = new SetOfMonads(m_FirstMonad, m_LastMonad); 
        
        EmdrosChange ch = (EmdrosChange)emdros.createChange(
            EmdrosChange.DELETE, "word", null);
        ch.setMonads(monadsToClear);
        ch.execute();
        
        ch = (EmdrosChange)emdros.createChange(EmdrosChange.DELETE, 
            "phrase", null);
        ch.setMonads(monadsToClear);
        ch.execute();

        ch = (EmdrosChange)emdros.createChange(EmdrosChange.DELETE, 
            "clause", null);
        ch.setMonads(monadsToClear);
        ch.execute();

        Lexeme be = new Lexeme(sql);
        be.save();
        addedLexemes.add(be);

        Lexeme want = new Lexeme(sql);
        want.save();
        addedLexemes.add(want);

        Lexeme drink = new Lexeme(sql);
        drink.save();
        addedLexemes.add(drink);

        Constant subj = new Constant("Subj");
        Constant objc = new Constant("Objc");
        Constant pred = new Constant("Pred");

        Object [] subjAttr = new Object []{
            "phrase_function", subj, "argument_name", "x"
        };
        Object [] predAttr = new Object []{"phrase_function", pred};
        Object [] objcAttr = new Object []{
            "phrase_function", objc, "argument_name", "y"
        };
        
        int clauseIdCatBlack = addClause(
            new String[]{"YAT","$LAYK"}, 
            new Object[][]{subjAttr, objcAttr}, null);
        int clauseIdCatIsBlack = addClause(
            new String[]{"YAT","IS","$LAYK"}, 
            new Object[][]{subjAttr, predAttr, objcAttr}, 
            new Integer(be.getID()));
        int clauseIdCatWantsMilk = addClause(
            new String[]{"YAT","WANTS","MILK"}, 
            new Object[][]{subjAttr, predAttr, objcAttr},
            new Integer(want.getID()));
        int clauseIdCatDrinksMilk = addClause(
            new String[]{"YAT","DRINKS","MILK"}, 
            new Object[][]{subjAttr, predAttr, objcAttr},
            new Integer(drink.getID()));
        
        DatabaseTransliterator transliterator = new DatabaseTransliterator(sql);
        
        assertEquals(null, Clause.find(emdros, sql, transliterator,
            clauseIdCatBlack).getEvaluatedLogicalStructure());
        assertEquals("", Clause.find(emdros, sql, transliterator,
            clauseIdCatIsBlack).getEvaluatedLogicalStructure());
        assertEquals("", Clause.find(emdros, sql, transliterator,
            clauseIdCatWantsMilk).getEvaluatedLogicalStructure());
        assertEquals("", Clause.find(emdros, sql, transliterator,
            clauseIdCatDrinksMilk).getEvaluatedLogicalStructure());
        
        be.setPredicate("<y>'");
        be.setDynamic(false);
        be.setHasEndpoint(false);
        be.setHasResultState(false);
        be.setPunctual(false);
        be.setTelic(false);
        be.setThematicRelation(ThematicRelation.get("STA-ind"));
        be.save();
        
        assertEquals(null,
            Clause.find(emdros, sql, transliterator,
                clauseIdCatBlack).getEvaluatedLogicalStructure());
        assertEquals("black'(cat:PATIENT)",
            Clause.find(emdros, sql,
                transliterator, clauseIdCatIsBlack).getEvaluatedLogicalStructure());
        assertEquals("",
            Clause.find(emdros, sql, transliterator,
                clauseIdCatWantsMilk).getEvaluatedLogicalStructure());
        assertEquals("",
            Clause.find(emdros, sql, transliterator,
                clauseIdCatDrinksMilk).getEvaluatedLogicalStructure());
        
        want.setPredicate("want'");
        want.setDynamic(false);
        want.setHasEndpoint(false);
        want.setHasResultState(false);
        want.setPunctual(false);
        want.setTelic(false);
        want.setThematicRelation(ThematicRelation.get("STA-des"));
        want.save();
        
        assertEquals(null,
            Clause.find(emdros, sql, transliterator,
                clauseIdCatBlack).getEvaluatedLogicalStructure());
        assertEquals("black'(cat:PATIENT)", 
            Clause.find(emdros, sql, transliterator,
                clauseIdCatIsBlack).getEvaluatedLogicalStructure());
        assertEquals("want'(cat:WANTER, milk:DESIRE)", 
            Clause.find(emdros, sql, transliterator,
                clauseIdCatWantsMilk).getEvaluatedLogicalStructure());
        assertEquals("",
            Clause.find(emdros, sql, transliterator,
                clauseIdCatDrinksMilk).getEvaluatedLogicalStructure());

        drink.setPredicate("drink'");
        drink.setDynamic(true);
        drink.setHasEndpoint(true);
        drink.setHasResultState(true);
        drink.setResultPredicate("drunk'");
        drink.setPunctual(false);
        drink.setTelic(false);
        drink.setResultPredicateArg("x");
        drink.setThematicRelation(ThematicRelation.get("ACT-cons"));
        drink.save();

        assertEquals(null,
            Clause.find(emdros, sql, transliterator,
                clauseIdCatBlack).getEvaluatedLogicalStructure());
        assertEquals("black'(cat:PATIENT)", 
            Clause.find(emdros, sql, transliterator,
                clauseIdCatIsBlack).getEvaluatedLogicalStructure());
        assertEquals("want'(cat:WANTER, milk:DESIRE)", 
            Clause.find(emdros, sql, transliterator,
                clauseIdCatWantsMilk).getEvaluatedLogicalStructure());
        assertEquals("do'(cat, [drink'(cat:CONSUMER, milk:CONSUMED)]) "+
            "& INGR drunk'(cat)",
            Clause.find(emdros, sql, transliterator,
                clauseIdCatDrinksMilk).getEvaluatedLogicalStructure());
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(LexemeTest.class);
    }
}

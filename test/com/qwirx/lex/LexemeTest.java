package com.qwirx.lex;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import junit.framework.TestCase;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosChange;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.emdros.EmdrosChange.MonadSetEntry;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.lexicon.ThematicRelation;
import com.qwirx.lex.lexicon.Lexeme.Variable;

public final class LexemeTest extends TestCase
{
    private SqlDatabase sql;
    private EmdrosDatabase emdros;
    
    private List addedLexemes = new Vector(), addedObjects = new Vector();
    
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
        assertEquals(a.id, b.id);
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

        String url = "http://localhost:8080/lex/lsedit.jsp?lsid=" + 
            expected.id;
        WebConversation conv = new WebConversation();
        conv.getClientProperties().setAcceptGzip(false);
        WebResponse resp = conv.getResponse(url);
        
        WebForm form = resp.getFormWithName("lsform");
        assertNotNull(form);

        assertEquals(expected.id == 0 ? "" : expected.id + "", 
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
        
        String url = "http://localhost:8080/lex/lsedit.jsp?lsid=" + 
            lexeme.id;
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
        int id = newValues.id;
        assertFalse("new values should belong to an existing lexeme", id == 0);
        
        Lexeme oldValues = Lexeme.load(sql, id);
        assertEquals(id, oldValues.id);
        assertFalse("equals method did not detect change", 
            newValues.equals(oldValues));
        
        newValues.save();
        assertEquals(id, newValues.id);
        
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
        assertEquals(0, t.id);
        t.save();
        assertTrue(t.id != 0);
        addedLexemes.add(t);

        Lexeme t2 = Lexeme.load(sql, t.id);
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
        
        Set testVars = new TreeSet();
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
    
    private Map makeMap(String [] values)
    {
        Map result = new Hashtable();
        
        for (int i = 0; i < values.length; i += 2)
        {
            result.put(values[i], values[i+1]);
        }
        
        return result;
    }

    private int addEmdrosObject(String type, MonadSetEntry[] monads,
        String featureName, String featureValue)
    throws DatabaseException
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
            EmdrosChange.CREATE, type, null);
        ch.setMonadsFromObjects(objectIds);
       
        for (Iterator i = features.keySet().iterator(); i.hasNext(); )
        {
            String name  = (String)( i.next() );
            String value = (String)( features.get(name) );
            ch.setString(name, value);
        }
        
        ch.execute();
        
        int id = ch.getInsertedRowId();
        addedObjects.add(new EmdrosObject(type, id));
        return id;
    }
    
    private String getEvaluatedLogicalStructure(int emdrosClauseId)
    throws DatabaseException, EmdrosException
    {
        Sheaf sheaf = emdros.getSheaf
            ("SELECT ALL OBJECTS IN { 1 - 5 } "+
             "WHERE [clause self = "+emdrosClauseId+
             "       GET evaluated_logical_struct]");

        SheafConstIterator sci = sheaf.const_iterator();
        assertTrue(sci.hasNext());
        
        Straw straw = sci.next();
        StrawConstIterator swci = straw.const_iterator();
        assertTrue(swci.hasNext());
        
        MatchedObject clause = swci.next();
        return clause.getEMdFValue("evaluated_logical_struct").getString();
    }
    
    public void testEmdrosDatabaseUpdatedWithLogicalStructure()
    throws Exception
    {
        Lexeme be = new Lexeme(sql);
        be.save();
        addedLexemes.add(be);

        Lexeme want = new Lexeme(sql);
        want.save();
        addedLexemes.add(want);

        Lexeme drink = new Lexeme(sql);
        drink.save();
        addedLexemes.add(drink);

        int wordIdCat = addEmdrosObject("word", new MonadSetEntry[]{
            new MonadSetEntry(1, 1)}, "lexeme", "cat");        
        int wordIdIs = addEmdrosObject("word", new MonadSetEntry[]{
            new MonadSetEntry(2, 2)}, "lexeme", "is");
        int wordIdDrinks = addEmdrosObject("word", new MonadSetEntry[]{
            new MonadSetEntry(3, 3)}, "lexeme", "drinks");
        int wordIdWants = addEmdrosObject("word", new MonadSetEntry[]{
            new MonadSetEntry(4, 4)}, "lexeme", "wants");
        int wordIdBlack = addEmdrosObject("word", new MonadSetEntry[]{
            new MonadSetEntry(5, 5)}, "lexeme", "black");
        int wordIdMilk = addEmdrosObject("word", new MonadSetEntry[]{
            new MonadSetEntry(6, 6)}, "lexeme", "milk");
  
        int phraseIdCat = addEmdrosObject("phrase", 
            new int[]{wordIdCat}, 
            makeMap(new String[]{"function", "Noun", "argument_name", "x"}));
        int phraseIdIs = addEmdrosObject("phrase", 
            new int[]{wordIdIs}, 
            makeMap(new String[]{"function", "Pred"}));
        int phraseIdWants = addEmdrosObject("phrase", 
            new int[]{wordIdWants}, 
            makeMap(new String[]{"function", "Pred"}));
        int phraseIdDrinks = addEmdrosObject("phrase", 
            new int[]{wordIdDrinks}, 
            makeMap(new String[]{"function", "Pred"}));
        int phraseIdBlack = addEmdrosObject("phrase", 
            new int[]{wordIdBlack}, 
            makeMap(new String[]{"function", "Noun", "argument_name", "y"}));
        int phraseIdMilk = addEmdrosObject("phrase", 
            new int[]{wordIdMilk}, 
            makeMap(new String[]{"function", "Noun", "argument_name", "y"}));
    
        int clauseIdCatBlack = addEmdrosObject("clause", 
            new int[]{phraseIdCat, phraseIdBlack}, 
            makeMap(new String[]{}));
        int clauseIdCatIsBlack = addEmdrosObject("clause", 
            new int[]{phraseIdCat, phraseIdIs, phraseIdBlack}, 
            makeMap(new String[]{"logical_struct_id", be.id+""}));
        int clauseIdCatWantsMilk = addEmdrosObject("clause", 
            new int[]{phraseIdCat, phraseIdWants, phraseIdMilk}, 
            makeMap(new String[]{"logical_struct_id", drink.id+""}));
        int clauseIdCatDrinksMilk = addEmdrosObject("clause", 
            new int[]{phraseIdCat, phraseIdDrinks, phraseIdMilk}, 
            makeMap(new String[]{"logical_struct_id", drink.id+""}));
        
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatBlack));
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatIsBlack));
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatWantsMilk));
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatDrinksMilk));
        
        be.setPredicate("<y>'");
        be.setDynamic(false);
        be.setHasEndpoint(false);
        be.setHasResultState(false);
        be.setPunctual(false);
        be.setTelic(false);
        be.setThematicRelation(ThematicRelation.list()[0]);
        be.save();
        
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatBlack));
        assertEquals("black'(cat':PATIENT)", 
            getEvaluatedLogicalStructure(clauseIdCatIsBlack));
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatWantsMilk));
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatDrinksMilk));
        
        want.setPredicate("want'");
        want.setDynamic(false);
        want.setHasEndpoint(false);
        want.setHasResultState(false);
        want.setPunctual(false);
        want.setTelic(false);
        want.setThematicRelation(ThematicRelation.list()[5]);
        want.save();
        
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatBlack));
        assertEquals("black'(cat':PATIENT)", 
            getEvaluatedLogicalStructure(clauseIdCatIsBlack));
        assertEquals("want'(cat':WANTER, milk':DESIRE)", 
            getEvaluatedLogicalStructure(clauseIdCatWantsMilk));
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatDrinksMilk));

        drink.setPredicate("drink'");
        drink.setDynamic(true);
        drink.setHasEndpoint(true);
        drink.setHasResultState(true);
        drink.setResultPredicate("drunk'");
        drink.setPunctual(false);
        drink.setTelic(false);
        drink.setThematicRelation(ThematicRelation.list()[18]);
        drink.save();
        
        assertEquals("", getEvaluatedLogicalStructure(clauseIdCatBlack));
        assertEquals("black'(cat':PATIENT)", 
            getEvaluatedLogicalStructure(clauseIdCatIsBlack));
        assertEquals("want'(cat':WANTER, milk':DESIRE)", 
            getEvaluatedLogicalStructure(clauseIdCatWantsMilk));
        assertEquals("do(cat', [drink'(cat':CONSUMER, milk':consumed)]) "+
            "& INGR drunk'(cat')", 
            getEvaluatedLogicalStructure(clauseIdCatDrinksMilk));
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(LexemeTest.class);
    }
}

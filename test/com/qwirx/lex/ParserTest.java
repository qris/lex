/*
 * Created on 21-Jan-2005
 *
 * Unit tests for the Lex parser code.
 */
package com.qwirx.lex;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.qwirx.lex.parser.Chart;
import com.qwirx.lex.parser.Edge;
import com.qwirx.lex.parser.Parser;
import com.qwirx.lex.parser.Rule;
import com.qwirx.lex.parser.RuleEdge;
import com.qwirx.lex.parser.RulePart;
import com.qwirx.lex.parser.WordEdge;
import com.qwirx.lex.parser.EdgeBase.AlreadyBoundException;
import com.qwirx.lex.parser.Rule.DuplicateNameException;

/**
 * @author chris
 *
 * Unit tests for the Lex parser code.
 */
public class ParserTest extends TestCase 
{
    public void testEdgePositioning()
    {
        Edge e = new WordEdge("foo", 0);
        assertTrue (e.isAt(0));
        assertFalse(e.isAt(-1));
        assertFalse(e.isAt(1));

        e = new WordEdge("bar", 1);
        assertFalse(e.isAt(0));
        assertTrue (e.isAt(1));
        assertFalse(e.isAt(2));
    }
    
    public void testAddToChart()
    {
        Chart chart = new Chart(2);
        List edges = chart.getEdges();
        assertEquals(0, edges.size());
        
        Edge e1 = new WordEdge("foo", 0);
        chart.add(e1);
        assertEquals(0, edges.size());
        edges = chart.getEdges();
        assertEquals(1, edges.size());
        assertEquals(e1, edges.get(0));

        Edge e2 = new WordEdge("bar", 1);
        chart.add(e2);
        assertEquals(1, edges.size());
        edges = chart.getEdges();
        assertEquals(2, edges.size());
        assertEquals(e1, edges.get(0));
        assertEquals(e2, edges.get(1));
        
        edges = chart.getEdgesAt(1);
        assertEquals(1,  edges.size());
        assertEquals(e2, edges.get(0));

        edges = chart.getEdgesAt(0);
        assertEquals(1,  edges.size());
        assertEquals(e1, edges.get(0));
    }
    
    public void testApplyRuleToChartReturnsEdge()
    {
        Chart chart = new Chart(2);
        chart.add(new WordEdge("the", 0));
        chart.add(new WordEdge("cat", 1));
        Rule r1 = Rule.makeFromString(1, "DET", "the");
        
        List newEdges = r1.applyTo(chart, 0);
        assertEquals(1, newEdges.size());
        
        newEdges = r1.applyTo(chart, 1);
        assertEquals(0, newEdges.size());
    }
    
    public void testRulePartRepeat()
    {
        String[] repetitions = new String[]{"","?","*","+"};
        
        for (int r = 0; r < repetitions.length; r++)
        {
            String repString = repetitions[r];
            
            String [] partDescs = new String[]{"the","{NP}"};
            for (int p = 0; p < partDescs.length; p++)
            {
                String partDesc = partDescs[p];
                RulePart part = RulePart.fromString(partDesc+repString);
                if (r == 0)
                {
                    assertFalse(part.canRepeat());
                    assertFalse(part.canSkip());
                }
                else if (r == 1)
                {
                    assertFalse(part.canRepeat());
                    assertTrue (part.canSkip());
                }
                else if (r == 2)
                {
                    assertTrue (part.canRepeat());
                    assertTrue (part.canSkip());
                }
                else if (r == 3)
                {
                    assertTrue (part.canRepeat());
                    assertFalse(part.canSkip());
                }
                else
                {
                    assertTrue(r >= 0 && r < 4);
                }
            }

            RulePart p = RulePart.fromString("the"+repString);
            assertFalse(p.matches(new WordEdge("", 0)));
            assertFalse(p.matches(new WordEdge("th", 0)));
            assertTrue (p.matches(new WordEdge("the", 0)));
            assertFalse(p.matches(new WordEdge("thet", 0)));
            assertFalse(p.matches(new WordEdge("thethe", 0)));
        }
        
        WordEdge[] none = new WordEdge[]{};
        WordEdge[] one  = new WordEdge[]{new WordEdge("the", 0)};
        WordEdge[] two  = new WordEdge[]{one[0], one[0]};
        WordEdge[] odd  = new WordEdge[]{one[0], new WordEdge("tea", 0)};
        
        RulePart p = RulePart.fromString("the");
        assertFalse(p.matches(none));
        assertTrue (p.matches(one));
        assertFalse(p.matches(two));
        assertFalse(p.matches(odd));
        
        p = RulePart.fromString("the?");
        assertTrue (p.matches(none));
        assertTrue (p.matches(one));
        assertFalse(p.matches(two));
        assertFalse(p.matches(odd));

        p = RulePart.fromString("the*");
        assertTrue (p.matches(none));
        assertTrue (p.matches(one));
        assertTrue (p.matches(two));
        assertFalse(p.matches(odd));

        p = RulePart.fromString("the+");
        assertFalse(p.matches(none));
        assertTrue (p.matches(one));
        assertTrue (p.matches(two));
        assertFalse(p.matches(odd));
    }
    
    public void testRuleFromStringSimple()
    {
        Rule rule = Rule.makeFromString(1, "DET",  "the");
        assertEquals(1, rule.id());
        assertEquals("DET", rule.symbol());
        
        RulePart[] parts = rule.parts();
        assertEquals(1, parts.length);
        
        RulePart part = parts[0];
        assertEquals("the", part.symbol());
        assertNull(part.name());
        assertTrue(part.terminal());
        
        Map conditions = part.conditions();
        assertEquals(0, conditions.size());
    }
    
    public void testRuleConstructionThrowsExceptionOnDuplicateName()
    {
        boolean thrown = false;
        try
        {
            Rule.makeFromString(7, "NP",          
                    "{NP:X.state=absolute} {conjunction} " +
                    "{NP:X.state=absolute}", new Hashtable(), new Hashtable());
        }
        catch (DuplicateNameException e)
        {
            thrown = true;
        }
        assertTrue("Expected DuplicateNameException was not thrown", thrown);
    }

    public void testRuleFromStringComplex()
    {
        Map fixNothing = new Hashtable();
        Map copyStateX = new Hashtable();
        copyStateX.put("state", "X");

        Rule rule = Rule.makeFromString(7, "NP",          
                "{NP:X.state=absolute} {conjunction} " +
                "{NP:Y.state=absolute}", fixNothing, copyStateX);

        assertEquals(7, rule.id());
        assertEquals("NP", rule.symbol());
        
        Map fixedAttrs = rule.fixedAttributes();
        assertEquals(0, fixedAttrs.size());
        
        Map copiedAttrs = rule.copiedAttributes();
        assertEquals(1, copiedAttrs.size());
        assertEquals("X", copiedAttrs.get("state"));
        
        RulePart[] parts = rule.parts();
        assertEquals(3, parts.length);

        RulePart part = parts[0];
        assertEquals("NP", part.symbol());
        assertEquals("X",  part.name());
        assertFalse (part.terminal());
        
        Map conditions = part.conditions();
        assertEquals(1, conditions.size());
        assertEquals("absolute", conditions.get("state"));
        
        part = parts[1];
        assertEquals("conjunction", part.symbol());
        assertNull  (part.name());
        assertFalse (part.terminal());
        
        part = parts[2];
        assertEquals("NP", part.symbol());
        assertEquals("Y",  part.name());
        assertFalse (part.terminal());
        
        conditions = part.conditions();
        assertEquals(1, conditions.size());
        assertEquals("absolute", conditions.get("state"));
    }

    public void testRuleFromStringWithPermutation()
    {
        Map fixNothing = new Hashtable();
        Map copyStateX = new Hashtable();
        copyStateX.put("state", "X");

        Rule rule = Rule.makeFromString(7, "NP",          
                "# {NP:X.state=absolute} {conjunction} " +
                "{NP:Y.state=absolute}", fixNothing, copyStateX);

        assertEquals(7, rule.id());
        assertEquals("NP", rule.symbol());
        
        assertTrue(rule.isPermutable());
        
        Map fixedAttrs = rule.fixedAttributes();
        assertEquals(0, fixedAttrs.size());
        
        Map copiedAttrs = rule.copiedAttributes();
        assertEquals(1, copiedAttrs.size());
        assertEquals("X", copiedAttrs.get("state"));
        
        RulePart[] parts = rule.parts();
        assertEquals(3, parts.length);

        RulePart part = parts[0];
        assertEquals("NP", part.symbol());
        assertEquals("X",  part.name());
        assertFalse (part.terminal());
        
        Map conditions = part.conditions();
        assertEquals(1, conditions.size());
        assertEquals("absolute", conditions.get("state"));
        
        part = parts[1];
        assertEquals("conjunction", part.symbol());
        assertNull  (part.name());
        assertFalse (part.terminal());
        
        part = parts[2];
        assertEquals("NP", part.symbol());
        assertEquals("Y",  part.name());
        assertFalse (part.terminal());
        
        conditions = part.conditions();
        assertEquals(1, conditions.size());
        assertEquals("absolute", conditions.get("state"));
    }

	public void testSimpleParse() throws Exception 
    {
		Rule[] rules = new Rule[]{
				Rule.makeFromString(1,  "DET",      "the"),
				Rule.makeFromString(2,  "NOUN",     "cat"),
				Rule.makeFromString(3,  "VERB",     "sat"),
				Rule.makeFromString(4,  "P",        "on"),
				Rule.makeFromString(5,  "NOUN",     "mat"),
                Rule.makeFromString(6,  "P",        "by"),
                Rule.makeFromString(7,  "NOUN",     "door"),
				Rule.makeFromString(8,  "NP",       "{DET} {NOUN}"),
				Rule.makeFromString(9,  "PP",       "{P} {NP}"),
				Rule.makeFromString(10, "VP",       "{VERB}"),
				Rule.makeFromString(11, "SENTENCE", "{NP} {VP} {PP}"),
                Rule.makeFromString(12, "SENTENCE", "{NP} {VP} {PP} {PP}")
		};
		
		Parser p = new Parser(rules);
		List results = p.parseFor("the cat sat on the mat by the door", "SENTENCE");
		
		assertEquals(1, results.size());
		
		Edge SENTENCE = (Edge)( results.get(0) );
		assertEquals("SENTENCE", SENTENCE.symbol());
		
		Edge NP1 = SENTENCE.part(0);
		assertEquals("NP", NP1.symbol());

		Edge DET = NP1.part(0);
		assertEquals("DET", DET.symbol());
		assertEquals("the", DET.part(0).toString());
		
		Edge NOUN = NP1.part(1);
		assertEquals("NOUN", NOUN.symbol());
		assertEquals("cat",  NOUN.part(0).toString());
		
		Edge VP = SENTENCE.part(1);
		assertEquals("VP", VP.symbol());
		
		Edge VERB = VP.part(0);
		assertEquals("VERB", VERB.symbol());
		assertEquals("sat", VERB.part(0).toString());
		
		Edge PP1 = SENTENCE.part(2);
		assertEquals("PP", PP1.symbol());
		
		Edge P1 = PP1.part(0);
		assertEquals("P",  P1.symbol());
		assertEquals("on", P1.part(0).symbol());
		
		Edge NP2 = PP1.part(1);
		assertEquals("NP", NP2.symbol());
		
		DET = NP2.part(0);
		assertEquals("DET", DET.symbol());
		assertEquals("the", DET.part(0).symbol());
		
		NOUN = NP2.part(1);
		assertEquals("NOUN", NOUN.symbol());
		assertEquals("mat",  NOUN.part(0).symbol());
        
        Edge PP2 = SENTENCE.part(3);
        assertEquals("PP", PP2.symbol());
        
        Edge P2 = PP2.part(0);
        assertEquals("P",  P2.symbol());
        assertEquals("by", P2.part(0).symbol());
        
        Edge NP3 = PP2.part(1);
        assertEquals("NP", NP3.symbol());
        
        DET = NP3.part(0);
        assertEquals("DET", DET.symbol());
        assertEquals("the", DET.part(0).symbol());
        
        NOUN = NP3.part(1);
        assertEquals("NOUN", NOUN.symbol());
        assertEquals("door",  NOUN.part(0).symbol());
	}

	public void testConstructParse() throws Exception 
    {
		Map absolute = new Hashtable();
		absolute.put("state", "absolute");

		Map construct = new Hashtable();
		construct.put("state", "construct");
		
		Map copyStateX = new Hashtable();
		copyStateX.put("state", "X");

		Map copyStateY = new Hashtable();
		copyStateY.put("state", "Y");
		
		Map fixNothing  = new Hashtable();
		Map copyNothing = new Hashtable();
		
		Rule[] rules = new Rule[]{
				Rule.makeFromString(1, "noun",        "TWLDWT/", 
						construct, copyNothing),
				Rule.makeFromString(2, "article",     "H"),
				Rule.makeFromString(3, "noun",        "CMJM/", 
						absolute, copyNothing),
				Rule.makeFromString(4, "conjunction", "W"),
				Rule.makeFromString(5, "noun",        ">RY/", 
						absolute, copyNothing),
				Rule.makeFromString(6, "NP",          
						"{article:X} {noun:Y}", fixNothing, copyStateY),
				Rule.makeFromString(7, "NP",          
						"{NP:X.state=absolute} {conjunction} " +
						"{NP:Y.state=absolute}", fixNothing, copyStateX),
				Rule.makeFromString(8, "NP",          
						"{noun:X}", fixNothing, copyStateX),
				Rule.makeFromString(9, "NP",
						"{NP:X.state=construct} {NP:Y.state=absolute}",
						absolute, copyNothing),
		};
		Parser p = new Parser(rules);
		
		List results = p.parseFor("TWLDWT/ H CMJM/ W H >RY/", "NP");
		assertEquals(2, results.size());
		
		for (int i = 0; i < results.size(); i++) {
			RuleEdge r = (RuleEdge)(results.get(i));
			System.out.println("Depth score of result "+i+": "+
					r.getDepthScore());	
		}
		
		Edge NP_all = (Edge)(results.get(1));
		assertEquals("NP",       NP_all.symbol());
		assertEquals("absolute", NP_all.attribute("state"));
		
		Edge NP_stories = NP_all.part(0);
		assertEquals("NP",        NP_stories.symbol());
		assertEquals("construct", NP_stories.attribute("state"));
		assertEquals("noun",      NP_stories.part(0).symbol());
		assertEquals("X",         NP_stories.partName(0));
		
		Edge NP_heavens_and_earth = NP_all.part(1);
		assertEquals("NP",       NP_heavens_and_earth.symbol());
		assertEquals("absolute", NP_heavens_and_earth.attribute("state"));
		
		assertEquals("X",     NP_heavens_and_earth.partName(0));
		Edge NP_heavens = NP_heavens_and_earth.part(0);
		assertEquals("NP",       NP_heavens.symbol());
		assertEquals("absolute", NP_heavens.attribute("state"));
		assertEquals("article",  NP_heavens.part(0).symbol());
		assertEquals("X",        NP_heavens.partName(0));
		assertEquals("noun",     NP_heavens.part(1).symbol());
		assertEquals("Y",        NP_heavens.partName(1));

		Edge CONJ_and = NP_heavens_and_earth.part(1);
		assertEquals("conjunction", CONJ_and.symbol());
		
		assertEquals("Y",   NP_heavens_and_earth.partName(2));
		Edge NP_earth = NP_heavens_and_earth.part(2);
		assertEquals("NP",       NP_earth.symbol());
		assertEquals("absolute", NP_earth.attribute("state"));
		assertEquals("article",  NP_earth.part(0).symbol());
		assertEquals("X",        NP_earth.partName(0));
		assertEquals("noun",     NP_earth.part(1).symbol());
		assertEquals("Y",        NP_earth.partName(1));
	}

    public void testRepetitionParse() throws Exception 
    {
        Rule[] rules = new Rule[] {
                Rule.makeFromString(1, "CAT", "cat"),
                Rule.makeFromString(2, "DOG", "dog"),
                Rule.makeFromString(3, "must_CAT_must_DOG",     "{CAT} {DOG}"),
                Rule.makeFromString(4, "maybe_CAT_must_DOG",    "{CAT}? {DOG}"),
                Rule.makeFromString(5, "maybe_CAT_maybe_DOG",   "{CAT}? {DOG}?"),
                Rule.makeFromString(6, "maybe_CATs_maybe_DOGs", "{CAT}* {DOG}*"),
                Rule.makeFromString(7, "must_CATs_must_DOGs",   "{CAT}+ {DOG}+"),
        };
        
        Parser p = new Parser(rules);
        
        List results = p.parseFor("cat dog", "must_CAT_must_DOG");
        assertEquals(1, results.size());
        
        Edge instance = (Edge)( results.get(0) );
        assertEquals("must_CAT_must_DOG", instance.symbol());
        
        Edge CAT = instance.part(0);
        assertEquals("CAT", CAT.symbol());

        Edge DOG = instance.part(1);
        assertEquals("DOG", DOG.symbol());
        
        String target = "must_CAT_must_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(0, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(0, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CAT_must_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(0, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());

        target = "maybe_CAT_maybe_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(1, p.parseFor("cat",             target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(0, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());

        target = "maybe_CATs_maybe_DOGs";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(1, p.parseFor("cat",             target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(0, p.parseFor("dog cat",         target).size());
        assertEquals(1, p.parseFor("cat cat dog",     target).size());
        assertEquals(1, p.parseFor("cat dog dog",     target).size());
        assertEquals(1, p.parseFor("cat cat dog dog", target).size());

        target = "must_CATs_must_DOGs";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(0, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(0, p.parseFor("dog cat",         target).size());
        assertEquals(1, p.parseFor("cat cat dog",     target).size());
        assertEquals(1, p.parseFor("cat dog dog",     target).size());
        assertEquals(1, p.parseFor("cat cat dog dog", target).size());
    }
    
    public void testRepetitionOrder() throws Exception 
    {
        Rule[] rules = new Rule[]{
                Rule.makeFromString(1, "N",  "door"),
                Rule.makeFromString(2, "N",  "mat"),
                Rule.makeFromString(3, "NP", "{N}+")
        };
        
        Parser p = new Parser(rules);
        List results = p.parseFor("door mat", "NP");
        
        assertEquals(1, results.size());
        
        Edge NP = (Edge)( results.get(0) );
        assertEquals("NP", NP.symbol());
        
        Edge N1 = NP.part(0);
        assertEquals("N", N1.symbol());

        Edge DOOR = N1.part(0);
        assertEquals("door", DOOR.symbol());

        Edge N2 = NP.part(1);
        assertEquals("N", N2.symbol());

        Edge MAT = N2.part(0);
        assertEquals("mat", MAT.symbol());
    }

    public void testPermutableParseSimple() throws Exception
    {
        Rule cat = Rule.makeFromString(1, "CAT", "cat");
        Rule dog = Rule.makeFromString(2, "DOG", "dog");
        Rule test = Rule.makeFromString(3, "maybe_CATs_maybe_DOGs", "# {CAT}* {DOG}*");
        
        Chart chart = new Chart(2);
        chart.add(new RuleEdge(cat, new Edge[]{
            new WordEdge("cat", 0, cat.part(0))
        }));
        chart.add(new RuleEdge(dog, new Edge[]{
            new WordEdge("dog", 1, dog.part(0))
        }));
        
        List edges = cat.applyTo(chart, 0);
        assertEquals(1, edges.size());
        chart.add(edges);

        edges = dog.applyTo(chart, 1);
        assertEquals(1, edges.size());
        chart.add(edges);

        edges = test.applyTo(chart, 1);
        assertEquals(1, edges.size());
        
        RuleEdge MCMD = (RuleEdge)( edges.get(0) );
        assertEquals("maybe_CATs_maybe_DOGs", MCMD.symbol());
        assertEquals(2,     MCMD.parts().length);

        RuleEdge CAT = (RuleEdge)( MCMD.parts()[0] );
        assertEquals("CAT", CAT.symbol());
        assertEquals(1,     CAT.parts().length);
        assertEquals("cat", CAT.parts()[0].symbol());

        RuleEdge DOG = (RuleEdge)( MCMD.parts()[1] );
        assertEquals("DOG", DOG.symbol());
        assertEquals(1,     DOG.parts().length);
        assertEquals("dog", DOG.parts()[0].symbol());
    }
    
    public void testPermutableParse() throws Exception 
    {
        Rule[] rules = new Rule[] {
                Rule.makeFromString(1, "CAT", "cat"),
                Rule.makeFromString(2, "DOG", "dog"),
                Rule.makeFromString(3, "must_CAT_must_DOG",     "# {CAT} {DOG}"),
                Rule.makeFromString(4, "maybe_CAT_must_DOG",    "# {CAT}? {DOG}"),
                Rule.makeFromString(5, "maybe_CAT_maybe_DOG",   "# {CAT}? {DOG}?"),
                Rule.makeFromString(6, "maybe_CATs_maybe_DOGs", "# {CAT}* {DOG}*"),
                Rule.makeFromString(7, "must_CATs_must_DOGs",   "# {CAT}+ {DOG}+"),
        };
        
        Parser p = new Parser(rules);
        
        List edges = p.parseFor("cat dog", "must_CAT_must_DOG");
        assertEquals(1, edges.size());
        
        Edge instance = (Edge)( edges.get(0) );
        assertEquals("must_CAT_must_DOG", instance.symbol());
        
        Edge CAT = instance.part(0);
        assertEquals("CAT", CAT.symbol());

        Edge DOG = instance.part(1);
        assertEquals("DOG", DOG.symbol());
        
        String target = "must_CAT_must_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(0, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CAT_must_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CAT_maybe_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(1, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CATs_maybe_DOGs";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(1, p.parseFor("cat",             target).size());
        assertEquals(1, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(1, p.parseFor("dog dog",         target).size());
        assertEquals(1, p.parseFor("cat cat cat",     target).size());
        assertEquals(1, p.parseFor("cat cat dog",     target).size());
        // assertEquals(1, p.parseFor("cat dog cat",     target).size());
        // assertEquals(1, p.parseFor("dog cat dog",     target).size());
        assertEquals(1, p.parseFor("dog dog cat",     target).size());
        assertEquals(1, p.parseFor("dog dog dog",     target).size());
        assertEquals(1, p.parseFor("cat cat dog dog", target).size());
        //assertEquals(1, p.parseFor("cat dog cat dog", target).size());
        //assertEquals(1, p.parseFor("cat dog dog cat", target).size());
        //assertEquals(1, p.parseFor("dog cat dog cat", target).size());

        target = "must_CATs_must_DOGs";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(0, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(1, p.parseFor("cat cat dog",     target).size());
        //assertEquals(1, p.parseFor("cat dog cat",     target).size());
        //assertEquals(1, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(1, p.parseFor("cat cat dog dog", target).size());
        //assertEquals(1, p.parseFor("cat dog cat dog", target).size());
        //assertEquals(1, p.parseFor("cat dog dog cat", target).size());
        //assertEquals(1, p.parseFor("dog cat dog cat", target).size());
    }

    public void testSearchingParse() throws Exception 
    {
        Rule[] rules = new Rule[] {
                Rule.makeFromString(1, "CAT", "cat"),
                Rule.makeFromString(2, "DOG", "dog"),
                Rule.makeFromString(3, "must_CAT_must_DOG",     "* {CAT} {DOG}"),
                Rule.makeFromString(4, "maybe_CAT_must_DOG",    "* {CAT}? {DOG}"),
                Rule.makeFromString(5, "maybe_CAT_maybe_DOG",   "* {CAT}? {DOG}?"),
                Rule.makeFromString(6, "maybe_CATs_maybe_DOGs", "* {CAT}* {DOG}*"),
                Rule.makeFromString(7, "must_CATs_must_DOGs",   "* {CAT}+ {DOG}+"),
        };
        
        Parser p = new Parser(rules);
        
        List edges = p.parseFor("cat dog", "must_CAT_must_DOG");
        assertEquals(1, edges.size());
        
        Edge instance = (Edge)( edges.get(0) );
        assertEquals("must_CAT_must_DOG", instance.symbol());
        
        Edge CAT = instance.part(0);
        assertEquals("CAT", CAT.symbol());

        Edge DOG = instance.part(1);
        assertEquals("DOG", DOG.symbol());

        String target = "must_CAT_must_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(0, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CAT_must_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CAT_maybe_DOG";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(1, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(0, p.parseFor("cat cat dog",     target).size());
        assertEquals(0, p.parseFor("cat dog cat",     target).size());
        assertEquals(0, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(0, p.parseFor("cat cat dog dog", target).size());
        assertEquals(0, p.parseFor("cat dog cat dog", target).size());
        assertEquals(0, p.parseFor("cat dog dog cat", target).size());
        assertEquals(0, p.parseFor("dog cat dog cat", target).size());

        target = "maybe_CATs_maybe_DOGs";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(1, p.parseFor("dog",             target).size());
        assertEquals(1, p.parseFor("cat",             target).size());
        assertEquals(1, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(1, p.parseFor("dog dog",         target).size());
        assertEquals(1, p.parseFor("cat cat cat",     target).size());
        assertEquals(1, p.parseFor("cat cat dog",     target).size());
        assertEquals(1, p.parseFor("cat dog cat",     target).size());
        assertEquals(1, p.parseFor("dog cat dog",     target).size());
        assertEquals(1, p.parseFor("dog dog cat",     target).size());
        assertEquals(1, p.parseFor("dog dog dog",     target).size());
        assertEquals(1, p.parseFor("cat cat dog dog", target).size());
        assertEquals(1, p.parseFor("cat dog cat dog", target).size());
        assertEquals(1, p.parseFor("cat dog dog cat", target).size());
        assertEquals(1, p.parseFor("dog cat dog cat", target).size());

        target = "must_CATs_must_DOGs";
        assertEquals(0, p.parseFor("",                target).size());
        assertEquals(0, p.parseFor("dog",             target).size());
        assertEquals(0, p.parseFor("cat",             target).size());
        assertEquals(0, p.parseFor("cat cat",         target).size());
        assertEquals(1, p.parseFor("cat dog",         target).size());
        assertEquals(1, p.parseFor("dog cat",         target).size());
        assertEquals(0, p.parseFor("dog dog",         target).size());
        assertEquals(0, p.parseFor("cat cat cat",     target).size());
        assertEquals(1, p.parseFor("cat cat dog",     target).size());
        assertEquals(1, p.parseFor("cat dog cat",     target).size());
        assertEquals(1, p.parseFor("dog cat dog",     target).size());
        assertEquals(0, p.parseFor("dog dog dog",     target).size());
        assertEquals(1, p.parseFor("cat cat dog dog", target).size());
        assertEquals(1, p.parseFor("cat dog cat dog", target).size());
        assertEquals(1, p.parseFor("cat dog dog cat", target).size());
        assertEquals(1, p.parseFor("dog cat dog cat", target).size());
    }

    /*
    private static Map map(String key, String value)
    {
        Map result = new Hashtable();
        result.put(key, value);
        return result;
    }
    */
    
    Rule[] english = new Rule[] {
            Rule.makeFromString(1,  "DET",   "the"),
            Rule.makeFromString(2,  "NOUN",  "man"),
            Rule.makeFromString(3,  "NOUN",  "woman"),
            Rule.makeFromString(4,  "VERB",  "saw"),
            Rule.makeFromString(5,  "NP",    "{DET} {NOUN}"),
            Rule.makeFromString(6,  "ACTOR", "{NP}"),
            Rule.makeFromString(7,  "UNDERGOER", "{NP}"),
            Rule.makeFromString(8,  "CORE",      "{ACTOR} {VERB} {UNDERGOER}"),
            Rule.makeFromString(9,  "CLAUSE",    "{CORE}"),
            Rule.makeFromString(10, "SENTENCE",  "{CLAUSE}")
    };

    /**
     * Asserts equality (equivalence) between two Instances, 
     * either Rules or Words
     * @param a The first  Instance (expected)
     * @param b The second Instance (actual)
     */
    void assertEquals(Edge a, Edge b)
    {
        assertEquals("'"+b+"' has wrong symbol", a.symbol(), b.symbol());

        if (a instanceof RuleEdge)
        {
            assertEquals(RuleEdge.class, b.getClass());
            RuleEdge ra = (RuleEdge)a;
            RuleEdge rb = (RuleEdge)b;
            assertEquals("'"+b+"' has wrong rule", ra.rule(), rb.rule());
        }
        else if (a instanceof WordEdge)
        {
            assertEquals(WordEdge.class, b.getClass());
            WordEdge wa = (WordEdge)a;
            WordEdge wb = (WordEdge)b;
            assertEquals("'"+b+"' has wrong surface text", 
                    wa.toString(), wb.toString());
        }

        Map bCopy = new Hashtable(b.attributes());
        for (Iterator key = a.attributes().keySet().iterator(); key.hasNext(); )
        {
            Object thisKey = key.next();
            assertEquals("'"+b+"' has mismatched attribute "+thisKey,
                    a.attributes().get(thisKey), 
                    b.attributes().get(thisKey));
            bCopy.remove(thisKey);
        }
        
        if (bCopy.size() > 0)
        {
            assertTrue
            (
                    "'"+b+"' has extra attribute '"+
                    bCopy.keySet().iterator().next()+
                    "'",
                    false
            );
        }
        
        int min = a.parts().length;
        if (min > b.parts().length)
            min = b.parts().length;
        
        for (int i = 0; i < min; i++)
        {
            assertEquals(a.parts()[i], b.parts()[i]);
        }
        
        if (a.parts().length > min)
        {
            assertTrue
            (
                    "'"+b+"' lacks instance '"+a.parts()[min]+"'",
                    false
            );
        }

        if (b.parts().length > min)
        {
            assertTrue
            (
                    "'"+b+"' has extra instance '"+b.parts()[min]+"'",
                    false
            );
        }
        
        if (a.getBoundInstance() != null)
        {
            assertNotNull(b.getBoundInstance());
            assertEquals
            (
                    "'"+b+"' is bound to the wrong rule",
                    a.getBoundInstance().rule(),
                    b.getBoundInstance().rule()
            );
            assertEquals
            (
                    "'"+b+"' is bound to the wrong location",
                    a.getBoundLocation(),
                    b.getBoundLocation()
            );
        }
    }
    
    /**
     * See Van Valin's Syntax (example 1.1a):
     * "the man saw the woman"
     */
    public void testParseEnglishExample_1_1_a()  
    {        
        Parser p = new Parser(english);
        List edges = p.parseFor("the man saw the woman", "SENTENCE");
        assertEquals(1, edges.size());
        
        Edge SENTENCE = (Edge)( edges.get(0) );
        assertEquals("SENTENCE", SENTENCE.symbol());
        
        Edge CLAUSE = SENTENCE.part(0);
        assertEquals("CLAUSE", CLAUSE.symbol());
        
        Edge CORE = CLAUSE.part(0);
        assertEquals("CORE", CORE.symbol());
        
        Edge ACTOR = CORE.part(0);
        assertEquals("ACTOR", ACTOR.symbol());

        Edge NP = ACTOR.part(0);
        assertEquals("NP", NP.symbol());

        Edge DET = NP.part(0);
        assertEquals("DET", DET.symbol());
        assertEquals("the", DET.part(0).toString());
        
        Edge NOUN = NP.part(1);
        assertEquals("NOUN", NOUN.symbol());
        assertEquals("man",  NOUN.part(0).toString());
        
        Edge VERB = CORE.part(1);
        assertEquals("VERB", VERB.symbol());
        assertEquals("saw", VERB.part(0).toString());

        Edge UNDERGOER = CORE.part(2);
        assertEquals("UNDERGOER", UNDERGOER.symbol());

        NP = UNDERGOER.part(0);
        assertEquals("NP", NP.symbol());

        DET = NP.part(0);
        assertEquals("DET", DET.symbol());
        assertEquals("the", DET.part(0).toString());
        
        NOUN = NP.part(1);
        assertEquals("NOUN", NOUN.symbol());
        assertEquals("woman",  NOUN.part(0).toString());

        // test the same thing another way, and test the bindings too
        
        RuleEdge actor = 
            new RuleEdge(english[5], new RuleEdge[]{
                    new RuleEdge(english[4], new RuleEdge[]{
                            new RuleEdge(english[0], new WordEdge[]{
                                    new WordEdge("the", 0, english[0].part(0))
                            }, english[4].part(0)),
                            new RuleEdge(english[1], new WordEdge[]{
                                    new WordEdge("man", 1, english[1].part(0))
                            }, english[4].part(1))
                    }, english[5].part(0))
        }, english[7].part(0));

        RuleEdge undergoer = 
            new RuleEdge(english[6], new RuleEdge[]{
                    new RuleEdge(english[4], new RuleEdge[]{
                            new RuleEdge(english[0], new WordEdge[]{
                                    new WordEdge("the", 3, english[0].part(0))
                            }, english[4].part(0)),
                            new RuleEdge(english[2], new WordEdge[]{
                                    new WordEdge("woman", 4, english[2].part(0))
                            }, english[4].part(1))
                    }, english[6].part(0))
        }, english[7].part(2));

        assertEquals
        (
                new RuleEdge(english[9], new RuleEdge[]{
                        new RuleEdge(english[8], new RuleEdge[]{
                                new RuleEdge(english[7], new RuleEdge[]{
                                        actor,
                                        new RuleEdge(english[3], 
                                                new WordEdge[]{
                                                new WordEdge("saw", 2,
                                                        english[3].part(0))
                                        }, english[7].part(1)),
                                        undergoer
                                }, english[8].part(0))
                        }, english[9].part(0))
                }),
                (Edge)( edges.get(0) )
        );
    }
    
    private RuleEdge newRuleInstance(Rule rule, RuleEdge i1)
    {
        try 
        {
            i1.bindTo(null, rule.part(0));
        }
        catch (AlreadyBoundException e)
        {
            throw new RuntimeException(e);
        }
        return new RuleEdge(rule, new Edge[]{i1});
    }

    private RuleEdge newRuleInstance(Rule rule, RuleEdge i1, 
        RuleEdge i2)
    {
        try 
        {
            i1.bindTo(null, rule.part(0));
            i2.bindTo(null, rule.part(1));
        }
        catch (AlreadyBoundException e)
        {
            throw new RuntimeException(e);
        }
        return new RuleEdge(rule, new Edge[]{i1, i2});
    }

    private RuleEdge newRuleInstance(Rule rule, RuleEdge i1, 
        RuleEdge i2, RuleEdge i3)
    throws AlreadyBoundException
    {
        i1.bindTo(null, rule.part(0));
        i2.bindTo(null, rule.part(1));
        i3.bindTo(null, rule.part(2));
        return new RuleEdge(rule, new Edge[]{i1, i2, i3});
    }

    private RuleEdge newRuleAutoBind(Rule rule, RuleEdge i1, 
        RuleEdge i2, RuleEdge i3)
    throws AlreadyBoundException
    {
        i1.bindTo(null, rule.part(i1.symbol()));
        i2.bindTo(null, rule.part(i2.symbol()));
        i3.bindTo(null, rule.part(i3.symbol()));
        return new RuleEdge(rule, new Edge[]{i1, i2, i3});
    }

    private RuleEdge newRuleInstance(Rule rule, int pos, String s1)
    {
        assertEquals(s1, rule.part(0).symbol());
        Edge i1 = new WordEdge(s1, pos, rule.part(0));
        return new RuleEdge(rule, new Edge[]{i1});
    }

    /*
    private RuleInstance newRuleInstance(Rule rule, String s1, String s2)
    {
     
        Instance i1 = new WordInstance(s1, rule.part(0));
        Instance i2 = new WordInstance(s2, rule.part(1));
        return new RuleInstance(rule, new Instance[]{i1, i2});
    }

    private RuleInstance newRuleInstance(Rule rule, String s1, String s2, 
        String s3)
    {
        Instance i1 = new WordInstance(s1, rule.part(0));
        Instance i2 = new WordInstance(s2, rule.part(1));
        Instance i3 = new WordInstance(s2, rule.part(2));
        return new RuleInstance(rule, new Instance[]{i1, i2, i3});
    }
    */

    /**
     * See Van Valin's Syntax (example 1.1b):
     * "the woman saw the man"
     */
    public void testParseEnglishExample_1_1_b()  
    throws AlreadyBoundException
    {        
        Parser p = new Parser(english);
        List edges = p.parseFor("the woman saw the man", "SENTENCE");
        
        RuleEdge actor = newRuleInstance
        (
                english[5], 
                newRuleInstance
                (
                        english[4], 
                        newRuleInstance(english[0], 0, "the"),
                        newRuleInstance(english[2], 1, "woman")
                )
        );

        RuleEdge undergoer = newRuleInstance
        (
                english[6], 
                newRuleInstance
                (
                        english[4], 
                        newRuleInstance(english[0], 3, "the"),
                        newRuleInstance(english[1], 4, "man")
                )
        );

        assertEquals
        (
                newRuleInstance
                (
                        english[9], 
                        newRuleInstance(
                                english[8], 
                                newRuleInstance
                                (
                                        english[7],
                                        actor,
                                        newRuleInstance(english[3], 2, "saw"), 
                                        undergoer
                                )
                        )
                ),
                (Edge)( edges.get(0) )
        );
    }

    Rule dyirbal_CORE = Rule.makeFromString(16, 
            "CORE", "# {ACTOR} {VERB} {UNDERGOER}");

    Rule [] dyirbal = new Rule[] {
            Rule.makeFromString(1,  "DET_fu",  "balan"),      // female undergoer
            Rule.makeFromString(2,  "DET_ma",  "bangul"),     // male actor
            Rule.makeFromString(3,  "DET_fa",  "bangun"),     // female actor
            Rule.makeFromString(4,  "DET_mu",  "bayi"),       // male undergoer
            Rule.makeFromString(5,  "NOUN_mu", "yara"),       // man, undergoer
            Rule.makeFromString(6,  "NOUN_ma", "yara-ngu"),   // man, actor
            Rule.makeFromString(7,  "NOUN_fu", "dugumbil"),   // woman, undergoer
            Rule.makeFromString(8,  "NOUN_fa", "dugumbi-ru"), // woman, actor
            Rule.makeFromString(9,  "VERB",    "buran"),      // see
            Rule.makeFromString(10, "NP_a",    "* {DET_ma} {NOUN_ma}"),
            Rule.makeFromString(11, "NP_a",    "* {DET_fa} {NOUN_fa}"),
            Rule.makeFromString(12, "NP_u",    "* {DET_mu} {NOUN_mu}"),
            Rule.makeFromString(13, "NP_u",    "* {DET_fu} {NOUN_fu}"),
            Rule.makeFromString(14, "ACTOR",     "{NP_a}"),
            Rule.makeFromString(15, "UNDERGOER", "{NP_u}"),
            dyirbal_CORE,
            Rule.makeFromString(17, "CLAUSE",    "{CORE}"),
            Rule.makeFromString(18, "SENTENCE",  "{CLAUSE}"),
    };

    RuleEdge dyirbal_man_actor = newRuleInstance
    (
            dyirbal[13],
            newRuleInstance
            (
                    dyirbal[9], 
                    newRuleInstance(dyirbal[1], 0, "bangul"),
                    newRuleInstance(dyirbal[5], 0, "yara-ngu")
            )
    );

    RuleEdge dyirbal_woman_undergoer = newRuleInstance
    (
            dyirbal[14],
            newRuleInstance
            (
                    dyirbal[12], 
                    newRuleInstance(dyirbal[0], 0, "balan"),
                    newRuleInstance(dyirbal[6], 0, "dugumbil")
            )
    );

    RuleEdge dyirbal_man_undergoer = newRuleInstance
    (
            dyirbal[14],
            newRuleInstance
            (
                    dyirbal[11], 
                    newRuleInstance(dyirbal[3], 0, "bayi"),
                    newRuleInstance(dyirbal[4], 0, "yara")
            )
    );

    RuleEdge dyirbal_woman_actor = newRuleInstance
    (
            dyirbal[13],
            newRuleInstance
            (
                    dyirbal[10], 
                    newRuleInstance(dyirbal[2], 0, "bangun"),
                    newRuleInstance(dyirbal[7], 0, "dugumbi-ru")
            )
    );

    RuleEdge dyirbal_verb_see = newRuleInstance(dyirbal[8], 0, "buran");

    RuleEdge dyirbal_SENTENCE_CORE(RuleEdge core)
    {
        return newRuleInstance
        (
                dyirbal[17],
                newRuleInstance
                (
                        dyirbal[16],
                        core
                )
        );
    }
    
    private void doParseDyirbalExample(String input, RuleEdge example)
    {
        Parser p = new Parser(dyirbal);
        List edges = p.parseFor(input, "SENTENCE");
        assertEquals(input, 1, edges.size());

        assertEquals
        (
                dyirbal_SENTENCE_CORE(example),
                (Edge)( edges.get(0) )
        );
    }
    
    /**
     * dyirbal "balan dugumbil bangul yara-ngu buran" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 1.2a)
     */
    public void testParseDyirbalExample_1_2_a() throws AlreadyBoundException  
    {        
        doParseDyirbalExample("balan dugumbil bangul yara-ngu buran", 
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_woman_undergoer,
                        dyirbal_man_actor,
                        dyirbal_verb_see
                )
        );
    }

    /**
     * dyirbal "bangul yara-ngu balan dugumbil buran" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 1.2b)
     */
    public void testParseDyirbalExample_1_2_b() throws AlreadyBoundException  
    {
        doParseDyirbalExample("bangul yara-ngu balan dugumbil buran",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_man_actor,
                        dyirbal_woman_undergoer,
                        dyirbal_verb_see
                )
        );
    }

    /**
     * dyirbal "bayi yara bangun dugumbi-ru buran" 
     * (the woman saw the man)
     * @see Van Valin's Syntax (example 1.2c)
     */
    public void testParseDyirbalExample_1_2_c() throws AlreadyBoundException  
    {
        doParseDyirbalExample("bayi yara bangun dugumbi-ru buran",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_man_undergoer,
                        dyirbal_woman_actor,
                        dyirbal_verb_see
                )
        );
    }

    /**
     * dyirbal "bangun dugumbi-ru bayi yara buran" 
     * (the woman saw the man)
     * @see Van Valin's Syntax (example 1.2d)
     */
    public void testParseDyirbalExample_1_2_d() throws AlreadyBoundException  
    {
        doParseDyirbalExample("bangun dugumbi-ru bayi yara buran",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_woman_actor,
                        dyirbal_man_undergoer,
                        dyirbal_verb_see
                )
        );
    }

    /**
     * dyirbal "bangul yara-ngu buran balan dugumbil" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 2.2b)
     */
    public void testParseDyirbalExample_2_2_b() throws AlreadyBoundException  
    {   
        doParseDyirbalExample("bangul yara-ngu buran balan dugumbil",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_man_actor,
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer
                )
        );
    }

    /**
     * dyirbal "buran balan dugumbil bangul yara-ngu" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 2.2c)
     */
    public void testParseDyirbalExample_2_2_c() throws AlreadyBoundException  
    {      
        doParseDyirbalExample("buran balan dugumbil bangul yara-ngu",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer,
                        dyirbal_man_actor
                )
        );
    }

    /**
     * dyirbal "buran bangul yara-ngu balan dugumbil" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 2.2e)
     */
    public void testParseDyirbalExample_2_2_e() throws AlreadyBoundException  
    {      
        doParseDyirbalExample("buran bangul yara-ngu balan dugumbil",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_verb_see,
                        dyirbal_man_actor,
                        dyirbal_woman_undergoer
                )
        );
    }

    /**
     * dyirbal "balan dugumbil buran bangul yara-ngu" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 2.2f)
     */
    public void testParseDyirbalExample_2_2_f() throws AlreadyBoundException  
    {      
        doParseDyirbalExample("balan dugumbil buran bangul yara-ngu",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_woman_undergoer,
                        dyirbal_verb_see,
                        dyirbal_man_actor
                )
        );
    }

    /**
     * dyirbal "bangul balan yara-ngu buran dugumbil" 
     * (the man saw the woman)
     * @see Van Valin's Syntax (example 2.6a)
     */
    public void testParseDyirbalExample_2_6_a() throws AlreadyBoundException  
    {      
        doParseDyirbalExample("bangul balan yara-ngu buran dugumbil",
                newRuleAutoBind
                (
                        dyirbal_CORE,
                        dyirbal_man_actor,
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer
                )
        );
    }

    // Gen 02,08
    // W   NV<[   JHWH/   >LHJM/   GN/    B  <DN=/   MN   QDM/
    // and planted yahweh god      garden in eden    from east
    // and he planted Yahweh God a garden in Eden from [the] east
    
    Rule [] hebrew = new Rule[] {
        Rule.makeFromString(1,  "CONJ",    "W"),
        Rule.makeFromString(2,  "V",       "NV<["),
        Rule.makeFromString(3,  "Nprop",   "JHWH/"),
        Rule.makeFromString(4,  "Nprop",   ">LHJM/"),
        Rule.makeFromString(5,  "Ncom",    "GN/"),
        Rule.makeFromString(6,  "P",       "B"),
        Rule.makeFromString(7,  "Nprop",   "<DN=/"),
        Rule.makeFromString(8,  "P",       "MN"),
        Rule.makeFromString(9,  "Nprop",   "QDM/"),
        Rule.makeFromString(10, "NP",      "{Nprop}+"),
        Rule.makeFromString(11, "NP",      "{Ncom}"),
        Rule.makeFromString(12, "PRED",    "{P}"),
        Rule.makeFromString(13, "PRED",    "{V}"),
        Rule.makeFromString(14, "NUC",     "{PRED}"),
        Rule.makeFromString(15, "PP",      "{NUC} {NP}"),
        Rule.makeFromString(16, "ARG",     "{NP}"),
        Rule.makeFromString(17, "CORE",    "{NUC} {ARG} {ARG}"),
        Rule.makeFromString(18, "CLM",     "{CONJ}"),
        Rule.makeFromString(19, "PERIPH",  "{PP}+"),
        Rule.makeFromString(20, "CLAUSE",  "{CLM} {CORE} {PERIPH}"),
    };

    public void testParseHebrewGenesis2_8()  
    {        
        Parser p = new Parser(hebrew);
        List edges = p.parseFor(
            "W NV<[ JHWH/ >LHJM/ GN/ B <DN=/ MN QDM/", "CLAUSE", false);
        assertEquals(1, edges.size());

        Edge CLAUSE = (Edge)( edges.get(0) );
        
        assertEquals("{CLAUSE {CLM {CONJ \"W\"}} " +
                "{CORE " +
                "{NUC {PRED {V \"NV<[\"}}} " +
                "{ARG {NP {Nprop \"JHWH/\"} {Nprop \">LHJM/\"}}} " +
                "{ARG {NP {Ncom \"GN/\"}}}" +
                "} " +
                "{PERIPH " +
                "{PP {NUC {PRED {P \"B\"}}} {NP {Nprop \"<DN=/\"}}} " +
                "{PP {NUC {PRED {P \"MN\"}}} {NP {Nprop \"QDM/\"}}}" +
                "}}", CLAUSE.toString());
    }

	public static void main(String[] args) 
    {
		junit.textui.TestRunner.run(ParserTest.class);
	}
}

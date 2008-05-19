/*
 * Created on 21-Jan-2005
 *
 * Unit tests for the Lex parser code.
 */
package com.qwirx.lex;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import com.qwirx.lex.parser.Chart;
import com.qwirx.lex.parser.Edge;
import com.qwirx.lex.parser.MorphEdge;
import com.qwirx.lex.parser.MorphRule;
import com.qwirx.lex.parser.Parser;
import com.qwirx.lex.parser.Rule;
import com.qwirx.lex.parser.RuleEdge;
import com.qwirx.lex.parser.RulePart;
import com.qwirx.lex.parser.WordEdge;
import com.qwirx.lex.parser.EdgeBase.AlreadyBoundException;
import com.qwirx.lex.parser.Rule.Attribute;
import com.qwirx.lex.parser.Rule.ConstAttribute;
import com.qwirx.lex.parser.Rule.CopiedAttribute;
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
        Chart chart = new Chart();
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
        Chart chart = new Chart();
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
                    "{NP:X.state=absolute}");
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

        Rule rule = Rule.makeFromString2(7, "NP.state=@X",          
                "{NP:X.state=absolute} {conjunction} " +
                "{NP:Y.state=absolute}");

        assertEquals(7, rule.id());
        assertEquals("NP", rule.symbol());
        
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
        Rule rule = Rule.makeFromString2(7, "NP.state=@X",          
                "# {NP:X.state=absolute} {conjunction} " +
                "{NP:Y.state=absolute}");

        assertEquals(7, rule.id());
        assertEquals("NP", rule.symbol());
        
        assertTrue(rule.isPermutable());
        
        List attrs = rule.attributes();
        assertEquals(1, attrs.size());
        CopiedAttribute attr = (CopiedAttribute)attrs.get(0);
        assertEquals("state", attr.getName());
        assertEquals("X", attr.getSource());
        
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
        p.setVerbose(true);
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
		Rule[] rules = new Rule[]{
				Rule.makeFromString2(1, "noun.state=:construct", "TWLDWT/"),
				Rule.makeFromString2(2, "article",     "H"),
				Rule.makeFromString2(3, "noun.state=:absolute", "CMJM/"),
				Rule.makeFromString2(4, "conjunction", "W"),
				Rule.makeFromString2(5, "noun.state=:absolute", ">RY/"), 
				Rule.makeFromString2(6, "NP.state=@Y", "{article:X} {noun:Y}"),
				Rule.makeFromString2(7, "NP.state=@X",          
						"{NP:X.state=absolute} {conjunction} " +
						"{NP:Y.state=absolute}"),
				Rule.makeFromString2(8, "NP.state=@X", "{noun:X}"),
				Rule.makeFromString2(9, "NP.state=:absolute",
						"{NP:X.state=construct} {NP:Y.state=absolute}")
		};
        
		Parser p = new Parser(rules);
		List results = p.parseFor("TWLDWT/ H CMJM/ W H >RY/", "NP");
		assertEquals(2, results.size());
		
		for (int i = 0; i < results.size(); i++) {
			RuleEdge r = (RuleEdge)(results.get(i));
			System.out.println("Depth score of result "+i+": "+
					r.getDepthScore());	
		}
		
		Edge NP_all = (Edge)(results.get(0));
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
		assertEquals("X",        NP_heavens_and_earth.partName(0));
        
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
        
        Chart chart = new Chart();
        
        /*
        chart.add(new RuleEdge(cat, new Edge[]{
            new WordEdge("cat", 0, cat.part(0))
        }));
        chart.add(new RuleEdge(dog, new Edge[]{
            new WordEdge("dog", 1, dog.part(0))
        }));
        */

        chart.add(new WordEdge("cat", 0, cat.part(0)));
        chart.add(new WordEdge("dog", 1, dog.part(0)));

        List edges = cat.applyTo(chart, 0);
        assertEquals(1, edges.size());
        chart.add(edges);

        edges = dog.applyTo(chart, 1);
        assertEquals(1, edges.size());
        chart.add(edges);

        edges = test.applyTo(chart, 3);
        assertEquals(2, edges.size());
        
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
            "CORE", "* {ACTOR} {VERB} {UNDERGOER}");

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
                        dyirbal_man_actor,
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer
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
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer
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
                        dyirbal_woman_actor,
                        dyirbal_verb_see,
                        dyirbal_man_undergoer
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
                        dyirbal_verb_see,
                        dyirbal_man_undergoer
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
                        dyirbal_man_actor,
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer
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
                        dyirbal_man_actor,
                        dyirbal_verb_see,
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
                        dyirbal_man_actor,
                        dyirbal_verb_see,
                        dyirbal_woman_undergoer
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
    
    Rule [] hebrewCons = new Rule[] {
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

    public void testParseHebrewConsGenesis2_8()  
    {        
        Parser p = new Parser(hebrewCons);
        List edges = p.parseFor(
            "W NV<[ JHWH/ >LHJM/ GN/ B <DN=/ MN QDM/", "CLAUSE");
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

    Rule [] hebrewQuest = new Rule[] {
        Rule.makeFromString(0,  "CLM",     "{CONJ}"),
        Rule.makeFromString(1,  "OPT",     "{V/TNS}"),
        Rule.makeFromString(2,  "IGN",     "{V/STM}"),
        Rule.makeFromString(3,  "NUC/V",   "{V/LEX}"),
        Rule.makeFromString(4,  "ARG/V",   "{V/PGN}"),
        Rule.makeFromString(5,  "CORE/V",  "{OPT} {IGN} {NUC/V} {ARG/V}"),
        Rule.makeFromString(6,  "N/PROP",  "{HEAD/NPROP} {MARK/N}"),
        Rule.makeFromString(7,  "N/COM",   "{HEAD/NCOM} {MARK/N}"),
        Rule.makeFromString(8,  "NP",      "{P} {DET} {N/COM}"),
        Rule.makeFromString(9,  "NP",      "{N/PROP}+"),
        Rule.makeFromString(10, "NP",      "{N/COM}"),
        Rule.makeFromString(11, "NP",      "{NP} {CONJ} {NP}"),
        Rule.makeFromString(12, "PRED/P",  "{P}"),
        Rule.makeFromString(13, "NUC/P",   "{PRED/P}"),
        Rule.makeFromString(14, "NUC",     "{CORE/V}"),
        Rule.makeFromString(15, "ARG",     "{NP}"),
        Rule.makeFromString(16, "CORE/P",  "{NUC/P} {NP}"),
        Rule.makeFromString(17, "PP",      "{CORE/P}"),
        Rule.makeFromString(18, "PreCS",   "{PP}"),
        Rule.makeFromString(19, "CORE",    "{NUC} {ARG} {ARG}"),
        Rule.makeFromString(20, "PERIPH",  "{PP}*"),
        Rule.makeFromString(21, "CLAUSE",  "{CLM}? {PreCS}? {CORE} {PERIPH}?"),
        Rule.makeFromString(22, "SENTENCE","{CLAUSE}"),
    };

    // Gen 02,08
    //
    // WA-  | J.I-  | Ø-    | V.A62<- | Ø     | J:HW@94H- | Ø         
    // CONJ | V/TNS | V/STM | V/LEX   | V/PGN | N/PROP    | N/GNS  
    //
    // >:ELOH- | I91Jm | G.An&- | Ø     | B.:- | <;73DEn- | Ø     
    // N/PROP  | N/GNS | N/COM  | N/GNS | P    | N/PROP   | N/GNS 
    //
    // MI- | Q.E92DEm- | Ø
    // P   | N/PROP    | N/GNS
    //
    // Expected output:
    // | SENTENCE                                                                                                |
    // | CLAUSE                                                                                                  |
    // | CLM  | CORE                                                       | PERIPH                              |
    // |      | NUC                       | ARG            | ARG           | PP               | PP               |
    // |      | CORE/V                    | NP             | NP            | NUC/P   | NP     | NUC/P   | NP     |
    // |      | OPT | IGN | NUC/V | ARG/V |                |               | PRED/P  |        | PRED/P  |        |
    // | CONJ | TNS | STM | LEX   | PGN   | N/PROP | N/GNS | N/COM | N/GNS | P       | N/PROP | P       | N/PROP |
    
    public void testParseHebrewQuestGenesis2_8()  
    {        
        Parser p = new Parser(hebrewQuest);
        
        List input = new ArrayList();
        
        input.add(new MorphEdge("CONJ",       "WA-",      0)); 
        input.add(new MorphEdge("V/TNS",      "J.I",      1)); 
        input.add(new MorphEdge("V/STM",      "",         2)); 
        input.add(new MorphEdge("V/LEX",      "V.A62<",   3)); 
        input.add(new MorphEdge("V/PGN",      "",         4)); 
        input.add(new MorphEdge("HEAD/NPROP", "J:HW@94H", 5)); 
        input.add(new MorphEdge("MARK/N",     "",         6)); 
        input.add(new MorphEdge("HEAD/NPROP", ">:ELOH",   7)); 
        input.add(new MorphEdge("MARK/N",     "I91Jm",    8)); 
        input.add(new MorphEdge("HEAD/NCOM",  "G.An&",    9)); 
        input.add(new MorphEdge("MARK/N",     "",         10)); 
        input.add(new MorphEdge("P",          "B.:-",     11)); 
        input.add(new MorphEdge("HEAD/NPROP", "<;73DEn",  12)); 
        input.add(new MorphEdge("MARK/N",     "",         13)); 
        input.add(new MorphEdge("P",          "MI-",      14)); 
        input.add(new MorphEdge("HEAD/NPROP", "Q.E92DEm", 15)); 
        input.add(new MorphEdge("MARK/N",     "",         16)); 
        
        p.setVerbose(true);
        List edges = p.parseFor(input, "SENTENCE");
        assertEquals(1, edges.size());

        Edge CLAUSE = (Edge)( edges.get(0) );
        
        assertEquals("{SENTENCE " +
            "{CLAUSE {CLM {CONJ \"WA-\"}} " +
            "{CORE " +
            "{NUC {CORE/V " +
            "{OPT {V/TNS \"J.I\"}} " +
            "{IGN {V/STM \"\"}} " +
            "{NUC/V {V/LEX \"V.A62<\"}} " +
            "{ARG/V {V/PGN \"\"}}" +
            "}} " +
            "{ARG {NP " +
            "{N/PROP {HEAD/NPROP \"J:HW@94H\"} {MARK/N \"\"}} " +
            "{N/PROP {HEAD/NPROP \">:ELOH\"} {MARK/N \"I91Jm\"}}" +
            "}} " +
            "{ARG {NP " +
            "{N/COM {HEAD/NCOM \"G.An&\"} {MARK/N \"\"}}" +
            "}}} " +
            "{PERIPH " +
            "{PP {CORE/P " +
            "{NUC/P {PRED/P {P \"B.:-\"}}} " +
            "{NP {N/PROP {HEAD/NPROP \"<;73DEn\"} {MARK/N \"\"}}}" +
            "}} " +
            "{PP {CORE/P " +
            "{NUC/P {PRED/P {P \"MI-\"}}} " +
            "{NP {N/PROP {HEAD/NPROP \"Q.E92DEm\"} {MARK/N \"\"}}}" +
            "}}" +
            "}}}", CLAUSE.toString());
    }

    // Gen 02,08
    //
    // Input:
    //
    // B.:- | R;>CI73J- | T      | Ø-    | Ø-    | B.@R@74>- | Ø
    // P    | HEAD/NCOM | MARK/N | V/TNS | V/STM | V/LEX     | V/PGN
    //
    // >:ELOH-    | I92Jm  | >;71T | HA- | C.@M-     | A73JIm | W:-
    // HEAD/NPROP | MARK/N | P     | DET | HEAD/NCOM | MARK/N | CONJ
    //
    // >;71T | H@- | >@75REy-  | Ø
    // P     | DET | HEAD/NCOM | MARK/N
    //
    // Expected output:
    //
    // | SENTENCE                                                                                                                        |
    // | CLAUSE                                                                                                                          |
    // | PreCS                       | CORE                                                                                              |
    // | PP                          | NUC                           | ARG                | ARG                                          |
    // | CORE/P                      | CORE/V                        | NP                 | NP                                           |
    // | NUC/P  | NP                 | OPT   | IGN   | NUC/V | ARG/V |                    | NP                |      | NP                |
    // | P      | HEAD/NCOM | MARK/N | V/TNS | V/STM | V/LEX | V/PGN | HEAD/NPROP | N/GNS | P | N/COM | N/GNS | CONJ | P | N/COM | N/GNS |
    
    public void testParseHebrewMorphGenesis1_1()  
    {        
        Parser p = new Parser(hebrewQuest);
        
        List input = new ArrayList();
        
        input.add(new MorphEdge("P",          "B.:-",     0)); 
        input.add(new MorphEdge("HEAD/NCOM",  "R;>CI73J", 1)); 
        input.add(new MorphEdge("MARK/N",     "T",        2)); 
        input.add(new MorphEdge("V/TNS",      "",         3)); 
        input.add(new MorphEdge("V/STM",      "",         4)); 
        input.add(new MorphEdge("V/LEX",      "B.@R@74>", 5)); 
        input.add(new MorphEdge("V/PGN",      "",         6)); 
        input.add(new MorphEdge("HEAD/NPROP", ">:ELOH",   7)); 
        input.add(new MorphEdge("MARK/N",     "I92Jm",    8)); 
        input.add(new MorphEdge("P",          ">;71T",    9)); 
        input.add(new MorphEdge("DET",        "HA-",      10)); 
        input.add(new MorphEdge("HEAD/NCOM",  "C.@M",     11)); 
        input.add(new MorphEdge("MARK/N",     "A73JIm",   12)); 
        input.add(new MorphEdge("CONJ",       "W:-",      13)); 
        input.add(new MorphEdge("P",          ">;71T",    14)); 
        input.add(new MorphEdge("DET",        "H@-",      15)); 
        input.add(new MorphEdge("HEAD/NCOM",  ">@75REy",  16)); 
        input.add(new MorphEdge("MARK/N",     "",         17)); 
        
        p.setVerbose(true);
        List edges = p.parseFor(input, "SENTENCE");
        assertEquals(1, edges.size());

        Edge CLAUSE = (Edge)( edges.get(0) );
        
        assertEquals("{SENTENCE " +
            "{CLAUSE {PreCS {PP {CORE/P " +
            "{NUC/P {PRED/P {P \"B.:-\"}}} " +
            "{NP {N/COM {HEAD/NCOM \"R;>CI73J\"} {MARK/N \"T\"}}}}}} " +
            "{CORE " +
            "{NUC {CORE/V " +
            "{OPT {V/TNS \"\"}} " +
            "{IGN {V/STM \"\"}} " +
            "{NUC/V {V/LEX \"B.@R@74>\"}} " +
            "{ARG/V {V/PGN \"\"}}" +
            "}} " +
            "{ARG {NP " +
            "{N/PROP {HEAD/NPROP \">:ELOH\"} {MARK/N \"I92Jm\"}}" +
            "}} " +
            "{ARG {NP " +
            "{NP {P \">;71T\"} {DET \"HA-\"} {N/COM {HEAD/NCOM \"C.@M\"} {MARK/N \"A73JIm\"}}} "+
            "{CONJ \"W:-\"} " +
            "{NP {P \">;71T\"} {DET \"H@-\"} {N/COM {HEAD/NCOM \">@75REy\"} {MARK/N \"\"}}}"+
            "}}}}}", 
            CLAUSE.toString());
    }

    public void testConstructParse2() throws Exception 
    {
        Rule[] rules = new Rule[]{
                Rule.makeFromString2(1, "noun.state=:construct", "TWLDWT/"),
                Rule.makeFromString2(2, "article",     "H"),
                Rule.makeFromString2(3, "noun.state=:absolute",  "CMJM/"),
                Rule.makeFromString2(4, "conjunction", "W"),
                Rule.makeFromString2(5, "noun.state=:absolute",  ">RY/"),
                Rule.makeFromString2(6, "NP.state=@Y", 
                        "{article:X} {noun:Y}"),
                Rule.makeFromString2(7, "NP.state=@X",          
                        "{NP:X.state=absolute} {conjunction} " +
                        "{NP:Y.state=absolute}"),
                Rule.makeFromString2(8, "NP.state=@X",          
                        "{noun:X}"),
                Rule.makeFromString2(9, "NP.state=:absolute",
                        "{NP:X.state=construct} {NP:Y.state=absolute}")
        };
        
        Parser p = new Parser(rules);
        List results = p.parseFor("TWLDWT/ H CMJM/ W H >RY/", "NP");
        assertEquals(2, results.size());
        
        for (int i = 0; i < results.size(); i++) {
            RuleEdge r = (RuleEdge)(results.get(i));
            System.out.println("Depth score of result "+i+": "+
                    r.getDepthScore()); 
        }
        
        Edge NP_all = (Edge)(results.get(0));
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
        assertEquals("X",        NP_heavens_and_earth.partName(0));
        
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

    public void testMorphologicalRules() throws Exception 
    {
        MorphRule [] morph = new MorphRule [] {
                new MorphRule(1, "ba-la-n",    "balan"),
                new MorphRule(2, "dugumbil-0", "dugumbil"),
                new MorphRule(3, "ba-ngu-l",   "bangul"),
                new MorphRule(4, "yara-ngu",   "yarangu"),
                new MorphRule(5, "bura-n",     "buran")
        };
        
        Rule [] rules = new Rule [] {
            Rule.makeFromString2(1, "DEIC", "ba-"),
            Rule.makeFromString2(2, "DCAS.case=:ABS",  "-la-"),
            Rule.makeFromString2(3, "DCLS.class=:II",   "-n"),
            Rule.makeFromString2(4, "NOUN.gloss=:woman,class=:II", "dugumbil-"),
            Rule.makeFromString2(5, "NCAS.case=:ABS",  "-0"),
            Rule.makeFromString2(6, "DCAS.case=:ERG",  "-ngu-"),
            Rule.makeFromString2(7, "NCAS.case=:ERG",  "-ngu"),
            Rule.makeFromString2(8, "DCLS.class=:I",    "-l"),
            Rule.makeFromString2(9, "NOUN.gloss=:man,class=:I",  "yara-"),
            Rule.makeFromString2(10, "VERB.gloss=:see",  "bura-"),
            Rule.makeFromString2(11, "TNS",  "-n"),
            Rule.makeFromString2(12, 
                "NP.class=@dcl,class=@n,case=@dca,case=@nca",
                "{DEIC} {DCAS:dca} {DCLS:dcl} {NOUN:n} {NCAS:nca}"),
            Rule.makeFromString2(13, "CORE", 
                "* {NP.case=ERG} {VERB} {TNS} {NP.case=ABS}"),
        };
        
        Parser p = new Parser(rules, morph);
        p.setVerbose(true);

        List results = p.parseFor("balan dugumbil bangul yarangu buran", "CORE");
        assertEquals(1, results.size());
        
        Edge core = (Edge)(results.get(0));
        assertEquals(
            "{CORE " +
            "{NP case=ERG,class=I " +
            "{DEIC {ba \"ba\"}} " +
            "{DCAS case=ERG {ngu \"ngu\"}}:dca " +
            "{DCLS class=I {l \"l\"}}:dcl " +
            "{NOUN class=I,gloss=man {yara \"yara\"}}:n " +
            "{NCAS case=ERG {ngu \"ngu\"}}:nca" +
            "} " +
            "{VERB gloss=see {bura \"bura\"}} " +
            "{TNS {n \"n\"}} " +
            "{NP case=ABS,class=II " +
            "{DEIC {ba \"ba\"}} " +
            "{DCAS case=ABS {la \"la\"}}:dca " +
            "{DCLS class=II {n \"n\"}}:dcl " +
            "{NOUN class=II,gloss=woman {dugumbil \"dugumbil\"}}:n " +
            "{NCAS case=ABS {0 \"0\"}}:nca}" +
            "}", core.toString()
        );
    }
    
    public void testUnificationSimple() throws Exception 
    {
        Rule[] rules = new Rule[]{
                Rule.makeFromString2(1, "C.gender=@X,gender=@Y", "{A:X} {B:Y}"),
                Rule.makeFromString2(2, "A.gender=:M", "a_m"),
                Rule.makeFromString2(3, "B.gender=:M", "b_m"),
                Rule.makeFromString2(4, "A.gender=:F", "a_f"),
                Rule.makeFromString2(5, "B.gender=:F", "b_f"),
                Rule.makeFromString2(6, "A.gender", "a_u"),
                Rule.makeFromString2(7, "B.gender", "b_u")
        };
        
        Parser p = new Parser(rules);
        p.setVerbose(true);

        List results = p.parseFor("a_m b_m", "C");
        assertEquals(1, results.size());
        Edge C = (Edge)(results.get(0));
        assertEquals("C", C.symbol());
        assertEquals("M", C.attribute("gender"));

        results = p.parseFor("a_f b_f", "C");
        assertEquals(1, results.size());
        C = (Edge)(results.get(0));
        assertEquals("C", C.symbol());
        assertEquals("F", C.attribute("gender"));

        results = p.parseFor("a_m b_f", "C");
        assertEquals(0, results.size());

        results = p.parseFor("a_m b_u", "C");
        assertEquals(1, results.size());
        C = (Edge)(results.get(0));
        assertEquals("C", C.symbol());
        assertEquals("M", C.attribute("gender"));
        Edge A = C.part(0);
        assertEquals("A", A.symbol());
        assertEquals("M", A.attribute("gender"));
        Edge B = C.part(1);
        assertEquals("B", B.symbol());
        assertEquals("M", B.attribute("gender"));

        results = p.parseFor("a_u b_f", "C");
        assertEquals(1, results.size());
        C = (Edge)(results.get(0));
        assertEquals("C", C.symbol());
        assertEquals("F", C.attribute("gender"));
        A = C.part(0);
        assertEquals("A", A.symbol());
        assertEquals("F", A.attribute("gender"));
        B = C.part(1);
        assertEquals("B", B.symbol());
        assertEquals("F", B.attribute("gender"));
    }

    private String findGroup(String regex, String input)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find())
        {
            return null;
        }
        return matcher.group(1);
    }
    
    class MultiMatcher
    {
        private final String  m_Input;
        private Matcher m_Matcher = null;
        public MultiMatcher(String input)
        {
            m_Input = input;
        }
        public boolean find(String regex)
        {
            Pattern pattern = Pattern.compile(regex);
            m_Matcher = pattern.matcher(m_Input);
            return m_Matcher.find();
        }
        public String group(int index)
        {
            return m_Matcher.group(index);
        }
    }
    
    private void assignArgumentRoles(Edge [] args, String ls)
    {
        Edge subject = null, directObject = null;
        
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].attribute("argtype").equals("S"))
            {
                assertNull(subject);
                subject = args[i];
            }
            else if (args[i].attribute("argtype").equals("DO"))
            {
                assertNull(directObject);
                directObject = args[i];
            }
        }
        
        assertNotNull(subject);
        // assumed for now, fix later:
        // assertEquals("1", subject.attribute("psa"));
        
        MultiMatcher matcher = new MultiMatcher(ls);
        Map assignments = new Hashtable();
        
        if (matcher.find("do'\\(<(\\w+)>,") ||
            matcher.find("'\\(<(\\w+)>,<\\w+>\\)") ||
            matcher.find("'\\(<(\\w+)>\\)"))
        {
            subject.addAttribute(new ConstAttribute("arg", matcher.group(1)));
            assignments.put(matcher.group(1), subject);
        }
        
        if ((matcher.find("'\\(<(\\w+)>\\)") &&
            assignments.get(matcher.group(1)) == null) ||
            (matcher.find("'\\(<\\w+>,<(\\w+)>\\)") &&
            assignments.get(matcher.group(1)) == null) ||
            (matcher.find("'\\(<(\\w+)>,<\\w+>\\)") &&
            assignments.get(matcher.group(1)) == null) ||
            (matcher.find("do'\\(<(\\w+)>,") &&
            assignments.get(matcher.group(1)) == null))
        {
            directObject.addAttribute(new ConstAttribute("arg", 
                matcher.group(1)));
            assignments.put(matcher.group(1), directObject);
        }
        
        assertNotNull(subject.attribute("arg"));
        assertFalse(directObject.attribute("arg") != null && 
            subject.attribute("arg") == null);
    }
    
    
    public void testTranslation()
    {
        Rule [] english = new Rule [] {
            Rule.makeFromString2(1,  "N/PROP", "John"),
            Rule.makeFromString2(2,  "NP", "{N/PROP}"),
            Rule.makeFromString2(3,  "V.ls=\":do'(<x>, run'(<x>)) " +
                    "CAUSE BECOME be-at'(<y>,<x>)\"", "ran"),
            Rule.makeFromString2(4,  "PREP", "to"),
            Rule.makeFromString2(5,  "DET", "the"),
            Rule.makeFromString2(6,  "N/COM", "park"),
            Rule.makeFromString2(7,  "NP", "{DET} {N/COM}"),
            Rule.makeFromString2(8,  "PP", "{PREP} {NP}"),
            Rule.makeFromString2(9,  "NUC", "{V}"),
            Rule.makeFromString2(10, "ARG.argtype", "{NP}"),
            Rule.makeFromString2(11, "ARG.argtype", "{PP}"),
            Rule.makeFromString2(12, "CORE", "{ARG.argtype=S} {NUC} " +
                    "{ARG.argtype=DO}"),
            Rule.makeFromString2(13, "CLAUSE", "{CORE}")
        };

        Parser p = new Parser(english);
        // p.setVerbose(true);

        // VVLP 97, p.152, example 4.12
        List results = p.parseFor("John ran to the park", "CLAUSE");
        assertEquals(1, results.size());
        Edge CLAUSE = (Edge)(results.get(0));
        assertEquals(
            "{CLAUSE {CORE " +
            "{ARG argtype=S {NP {N/PROP \"John\"}}} " +
            "{NUC {V ls=\"do'(<x>, run'(<x>)) " +
            "CAUSE BECOME be-at'(<y>,<x>)\" \"ran\"}} " +
            "{ARG argtype=DO {PP {PREP \"to\"} " +
            "{NP {DET \"the\"} {N/COM \"park\"}}}}}" +
            "}",
            CLAUSE.toString());
        Edge CORE = CLAUSE.part("CORE", 0);
        Edge NUC  = CORE  .part("NUC",  0);
        Edge V    = NUC   .part("V",    0);
        String ls = V.attribute("ls");
        assertEquals("do'(<x>, run'(<x>)) CAUSE BECOME be-at'(<y>,<x>)", ls);
        
        Edge [] args = CORE.parts("ARG");
        assertEquals("S",  args[0].attribute("argtype"));
        assertEquals("DO", args[1].attribute("argtype"));
        
        assignArgumentRoles(args, ls);
        assertEquals("x", args[0].attribute("arg"));
        assertEquals("y", args[1].attribute("arg"));
        
        Rule [] spanish = new Rule [] {
            Rule.makeFromString2(1,  "N/PROP", "Juan"),
            Rule.makeFromString2(2,  "NP", "{N/PROP}"),
            Rule.makeFromString2(3,  "V.ls=\":do'(<x>, run'(<x>)) " +
                    "CAUSE BECOME be-at'(<y>,<x>)\"", "corro"),
            Rule.makeFromString2(4,  "PREP", "a"),
            Rule.makeFromString2(5,  "DET", "el"),
            Rule.makeFromString2(6,  "N/COM", "parque"),
            Rule.makeFromString2(7,  "NP", "{DET} {N/COM}"),
            Rule.makeFromString2(8,  "PP", "{PREP} {NP}"),
            Rule.makeFromString2(9,  "NUC", "{V}"),
            Rule.makeFromString2(10, "ARG.argtype", "{NP}"),
            Rule.makeFromString2(11, "ARG.argtype", "{PP}"),
            Rule.makeFromString2(12, "CORE", "{ARG.argtype=S} {NUC} " +
                    "{ARG.argtype=DO}"),
            Rule.makeFromString2(13, "CLAUSE", "{CORE}")
        };

        Rule verb = null;
        
        for (int i = 0; i < spanish.length; i++)
        {
            List attributes = spanish[i].attributes();
            
            for (Iterator a = attributes.iterator(); a.hasNext();)
            {
                Attribute attr = (Attribute)a.next();
                if (attr.getName().equals("ls"))
                {
                    String value = attr.getValue(null);
                    if (value.equals(ls))
                    {
                        assertNull(verb);
                        verb = spanish[i];
                    }
                }
            }
        }
        
        assertNotNull(verb);
        
        
    }

    public void testInfiniteLoop() throws Exception 
    {
        Rule[] rules = new Rule[]{
                Rule.makeFromString2(1, "C", "{B}"),
                Rule.makeFromString2(2, "B", "x"),
                Rule.makeFromString2(3, "B", "{A}"),
                Rule.makeFromString2(4, "A", "{B}"),
        };
        
        Parser p = new Parser(rules);
        p.setVerbose(true);

        List results = p.parseFor("x", "C");
        assertEquals(2, results.size());

        Edge C = (Edge)(results.get(1));
        assertEquals("C", C.symbol());
        assertEquals("B", C.part(0).symbol());
        assertEquals("x", C.part(0).part(0).symbol());

        C = (Edge)(results.get(0));
        assertEquals("C", C.symbol());
        assertEquals("B", C.part(0).symbol());
        assertEquals("A", C.part(0).part(0).symbol());
        assertEquals("B", C.part(0).part(0).part(0).symbol());
        assertEquals("x", C.part(0).part(0).part(0).part(0).symbol());
    }

	public static void main(String[] args) 
    {
		junit.textui.TestRunner.run(ParserTest.class);
	}
}

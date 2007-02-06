package com.qwirx.lex;

import junit.framework.TestCase;

import com.qwirx.lex.lexicon.ThematicRelation;
import com.qwirx.lex.lexicon.ThematicRelation.Arguments;
import com.qwirx.lex.lexicon.ThematicRelation.Theme;

public class ThematicRelationTest extends TestCase
{
    private void assertEquals(ThematicRelation expected, 
        ThematicRelation actual)
    {
        assertEquals(expected.getLabel(), 
            actual.getLabel());
        assertEquals(expected.getPrompt(),
            actual.getPrompt());
        assertEquals(expected.getExample(),
            actual.getExample());
        assertEquals(expected.getArgs().toString(), 
            actual.getArgs().toString());
        assertEquals(expected.getThemes().length, 
            actual.getThemes().length);
        
        for (int i = 0; i < expected.getThemes().length; i++)
        {
            assertEquals(expected.getThemes()[i], 
                actual.getThemes()[i]);
        }        
    }
    
    public void testThematicRelationList()
    {
        ThematicRelation[] rels = ThematicRelation.list();
        
        assertEquals(new ThematicRelation("STA-ind-x", 
            "<x> is", "broken", Arguments.X, Theme.PATIENT), rels[0]);

        assertEquals(new ThematicRelation("STA-ind-y", 
            "<y> is", "broken", Arguments.Y, Theme.PATIENT), rels[1]);

        assertEquals(new ThematicRelation("STA-loc-xy", "<x> is at <y>", 
            "be-LOC", Arguments.XY, Theme.LOCATION, Theme.THEME), rels[4]);
        
        assertEquals(new ThematicRelation("STA-loc-yx", "<y> is at <x>", 
            "be-LOC", Arguments.YX, Theme.LOCATION, Theme.THEME), rels[5]);

        /*
        new ThematicRelation("STA-xst", "<x> exists",        "exist",  
            Arguments.X, Theme.ENTITY),
        new ThematicRelation("STA-loc", "<x> is at <y>",       "be-LOC", 
            Arguments.XY, Theme.LOCATION, Theme.THEME),
        new ThematicRelation("STA-per", "<x> perceives <y>",   "hear",   
            Arguments.XY, Theme.PERCEIVER, Theme.STIMULUS),
        new ThematicRelation("STA-cog", "<x> cognizes <y>",    "know",   
            Arguments.XY, Theme.COGNIZER, Theme.CONTENT),
        new ThematicRelation("STA-des", "<x> desires <y>",     "want",   
            Arguments.XY, Theme.WANTER, Theme.DESIRE),
        new ThematicRelation("STA-con", "<x> considers <y>",   "consider", 
            Arguments.XY, Theme.JUDGER, Theme.JUDGMENT),
        new ThematicRelation("STA-pos", "<x> possesses <y>",   "have",   
            Arguments.XY, Theme.POSSESSOR, Theme.POSSESSED),
        new ThematicRelation("STA-exp", "<x> experiences <y>", "feel",   
            Arguments.XY, Theme.EXPERIENCER, Theme.SENSATION),
        new ThematicRelation("STA-emo", "<x> feels for <y>",   "love",   
            Arguments.XY, Theme.EMOTER, Theme.TARGET),
        new ThematicRelation("STA-att", "<x> has attribute <y>", "be-ATT", 
            Arguments.XY, Theme.ATTRIBUTANT, Theme.ATTRIBUTE),
        new ThematicRelation("STA-idn", "<x> has identity <y>", "be-ID", 
            Arguments.XY, Theme.IDENTIFIED, Theme.IDENTITY),
        new ThematicRelation("STA-val", "<x> has value <y>",   "be-SPEC", 
            Arguments.XY, Theme.VARIABLE, Theme.VALUE),
        new ThematicRelation("STA-equ", "<x> is identical to <y>", "equate", 
            Arguments.XY, Theme.REFERENT, Theme.REFERENT),
        new ThematicRelation("ACT-unsp", "<x> does something unspecified", 
            "&Oslash;", Arguments.X, Theme.EFFECTOR),
        new ThematicRelation("ACT-move", "<x> moves",        "walk",   
            Arguments.X, Theme.MOVER),
        new ThematicRelation("ACT-emit", "<x> emits something", "shine", 
            Arguments.X, Theme.EMITTER),
        new ThematicRelation("ACT-perf", "<x> performs <y>",   "sing",   
            Arguments.XY, Theme.PERFORMER, Theme.PERFORMANCE),
        new ThematicRelation("ACT-cons", "<x> consumes <y>",   "eat",    
            Arguments.XY, Theme.CONSUMER, Theme.CONSUMED),
        new ThematicRelation("ACT-crea", "<x> creates <y>",    "write",  
            Arguments.XY, Theme.CREATOR, Theme.CREATION),
        new ThematicRelation("ACT-dirp", "<x> perceives <y>",  "hear",   
            Arguments.XY, Theme.OBSERVER, Theme.STIMULUS),
        new ThematicRelation("ACT-uses", "<x> uses <y>",       "use",    
            Arguments.XY, Theme.USER, Theme.IMPLEMENT),
            */
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ThematicRelationTest.class);
    }
}

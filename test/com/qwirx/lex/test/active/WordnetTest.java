package com.qwirx.lex.test.active;

import junit.framework.TestCase;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;

import com.qwirx.lex.wordnet.Wordnet;

public class WordnetTest extends TestCase
{
    public void testCanGetInstance() throws Exception
    {
        Wordnet.getInstance();
    }
    
    public void testCanGetSynsets() throws Exception
    {
        Wordnet wn = Wordnet.getInstance();
        Synset [] synsets = wn.getSenses(POS.NOUN, "land");

        assertEquals(11, synsets.length);
        assertEquals(11176958, ((Long)synsets[0].getKey()).longValue());
        assertEquals(7667751,  ((Long)synsets[1].getKey()).longValue());
        assertEquals(7667106,  ((Long)synsets[2].getKey()).longValue());
        assertEquals(7043405,  ((Long)synsets[3].getKey()).longValue());
        assertEquals(7034213,  ((Long)synsets[4].getKey()).longValue());
        assertEquals(12257615, ((Long)synsets[5].getKey()).longValue());
        assertEquals(11173919, ((Long)synsets[6].getKey()).longValue());
        assertEquals(6771217,  ((Long)synsets[7].getKey()).longValue());
        assertEquals(6772247,  ((Long)synsets[8].getKey()).longValue());
        assertEquals(9114381,  ((Long)synsets[9].getKey()).longValue());
        assertEquals(339271,   ((Long)synsets[10].getKey()).longValue());
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(WordnetTest.class);
    }
}

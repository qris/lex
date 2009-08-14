package com.qwirx.lex.test.active;

import com.qwirx.lex.hebrew.WivuLexicon;
import com.qwirx.lex.hebrew.WivuLexicon.Entry;

import junit.framework.TestCase;

public class WivuLexiconTest extends TestCase
{
    private WivuLexicon m_Lexicon;
    
    public void setUp() throws Exception
    {
        m_Lexicon = new WivuLexicon();
    }
    
    public void testWivuLexicon() throws Exception
    {
        assertEntry("<B/", "", "cloud, cloud-mass");
        assertEntry("<B=/", "", "<architectural>");
        assertEntry("<B==/", "", "thicket");
        assertEntry("<BD[", "qal", "work, till, serve");
        assertEntry("<BD[", "ni", "be cultivated");
        assertEntry("<BD[", "pu", "it is worked with");
        assertEntry("<BD[", "hi", "let work, serve");
        assertEntry("<BD[", "ho", "be led to serve");
        
        Entry [] entries = m_Lexicon.getEntry("<L", "");
        assertEquals(5, entries.length);
        assertEntry(entries[0], "upon, over; on account of; " +
                "against, opposite to");
        assertEntry(entries[1], "down from");
        assertEntry(entries[2], "up over");
        assertEntry(entries[3], "therefore");
        assertEntry(entries[4], "upon, over, against, concerning");
    }
    
    private void assertEntry(String lexeme, String form, String gloss)
    {
        Entry [] entries = m_Lexicon.getEntry(lexeme, form);
        assertNotNull(entries);
        assertEquals(1, entries.length);
        assertEquals(gloss, entries[0].getGloss());
    }

    private void assertEntry(Entry entry, String gloss)
    {
        assertEquals(gloss, entry.getGloss());
    }
}

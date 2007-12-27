package com.qwirx.lex;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import junit.framework.TestCase;

import org.crosswire.jsword.book.BookData;

import com.qwirx.crosswire.kjv.KJV;
import com.qwirx.lex.emdros.EmdrosDatabase;

public class GemExporterTest extends TestCase
{
    public void testGemExport() throws Exception
    {
        EmdrosDatabase emdros = Lex.getEmdrosDatabase("test", "localhost");
        
        Sheaf sheaf = emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + emdros.getMinM() + "-" + emdros.getMaxM() + "} " +
            "WHERE " +
            "[clause self = 1324989 " +
            " [phrase "+
            "  [word GET text, graphical_word, lexeme]"+
            " ]"+
            "]"
        );

        MatchedObject clause = null;
        SheafConstIterator sci = sheaf.const_iterator();
        assertTrue(sci.hasNext());
        Straw straw = sci.next();
        StrawConstIterator swci = straw.const_iterator();
        assertTrue(swci.hasNext()); 
        clause = swci.next();
        
        BookData verse = KJV.getVerse(emdros, "Nehemiah", 2, 11); 

        assertEquals(
            "\\wordfield word\n" +
            "\\glossfield gloss\n" +
            "\\tagfield tag\n" +
            "\\transliterationfield trans\n" +
            "\\lemmafield lemma\n" +
            "\\recordend re\n" +
            "\\foreignfont SILIPAManuscriptL\n" +
            "\\glossfont Times New Roman\n" +
            "\\transliterationfont SILIPADoulosL\n" +
            "\\righttoleft\n" +
            "\\wordfieldisUTF8\n" +
            "\\transliterationfieldisUTF8\n" +
            "\n" +
            "\\word וָ\n" +
            "\\trans wā-\n" +
            "\\tag tag\n" +
            "\\gloss \n" +
            "\\lemma lemma\n" +
            "\\re\n" +
            "\n" +
            "\\word אֱהִי\n" +
            "\\trans ?ĕhî\n" +
            "\\tag tag\n" +
            "\\gloss (no exact matches)\n" +
            "\\lemma lemma\n" +
            "\\re\n" +
            "\n" +
            "\\word שָׁ֖ם\n" +
            "\\trans šām\n" +
            "\\tag tag\n" +
            "\\gloss (no exact matches)\n" +
            "\\lemma lemma\n" +
            "\\re\n" +
            "\n" +
            "\\word יָמִ֥ים\n" +
            "\\trans jāmîm\n" +
            "\\tag tag\n" +
            "\\gloss days\n" +
            "\\lemma lemma\n" +
            "\\re\n" +
            "\n" +
            "\\word שְׁלֹשָֽׁה\n" +
            "\\trans šəlōšāh\n" +
            "\\tag tag\n" +
            "\\gloss (no exact matches)\n" +
            "\\lemma lemma\n" +
            "\\re\n\n",
            GemExporter.export(clause, verse));
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(GemExporterTest.class);
    }
}

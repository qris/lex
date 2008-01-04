package com.qwirx.lex;

import java.util.Map;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import junit.framework.TestCase;

import org.crosswire.jsword.book.BookData;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.qwirx.crosswire.kjv.KJV;
import com.qwirx.lex.emdros.EmdrosDatabase;

public class GenExporterTest extends TestCase
{
    private static final String NEHEMIAH_2_11b_EXPORT =
        "\\wordfield morpheme\n" +
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
        "\\morpheme וָ\n" +
        "\\trans wā-\n" +
        "\\tag tag\n" +
        "\\gloss CONJ\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme אֱ\n" +
        "\\trans ?ĕ-\n" +
        "\\tag tag\n" +
        "\\gloss NARR\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme \n" +
        "\\trans Ø-\n" +
        "\\tag tag\n" +
        "\\gloss Qa\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme הִי־\n" +
        "\\trans hî-\n" +
        "\\tag tag\n" +
        "\\gloss be\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme \n" +
        "\\trans Ø-\n" +
        "\\tag tag\n" +
        "\\gloss 1sg\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme \n" + 
        "\\trans Ø\n" +
        "\\tag tag\n" +
        "\\gloss SUFF\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme שָׁם\n" +
        "\\trans šām\n" +
        "\\tag tag\n" +
        "\\gloss ADV\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme יָמ\n" +
        "\\trans jām-\n" +
        "\\tag tag\n" +
        "\\gloss day\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme ִים\n" +
        "\\trans îm-\n" +
        "\\tag tag\n" +
        "\\gloss MplAB\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme \n" + 
        "\\trans Ø\n" +
        "\\tag tag\n" +
        "\\gloss SUFF\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme שְׁלֹשׁ\n" + 
        "\\trans šəlōš-\n" +
        "\\tag tag\n" +
        "\\gloss null\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme ָה\n" +
        "\\trans āh-\n" +
        "\\tag tag\n" +
        "\\gloss FsgAB\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n" +
        "\\morpheme \n" + 
        "\\trans Ø\n" +
        "\\tag tag\n" +
        "\\gloss SUFF\n" +
        "\\lemma lemma\n" +
        "\\re\n" +
        "\n";
    
    public void testGenExportCode() throws Exception
    {
        EmdrosDatabase emdros = Lex.getEmdrosDatabase("test", "localhost");
        
        Sheaf sheaf = emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + emdros.getMinM() + "-" + emdros.getMaxM() + "} " +
            "WHERE " +
            "[clause self = 1324989 " +
            " [phrase "+
            "  [word GET phrase_dependent_part_of_speech, person, gender, " +
            "            number, state, wordnet_gloss, lexeme, tense, stem, " +
            "            graphical_preformative, graphical_root_formation, " +
            "            graphical_lexeme, graphical_verbal_ending, " +
            "            graphical_nominal_ending, graphical_pron_suffix]" +
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

        assertEquals(NEHEMIAH_2_11b_EXPORT, 
            new GenExporter(emdros).export(clause, verse));
    }
    
    public void testGenExportJsp() throws Exception
    {
        WebConversation conv = new WebConversation();
        WebResponse response = conv.getResponse("http://localhost:8080/lex" +
                "/gen-export.jsp?clause=1324989");
        assertEquals("text/x-gen", response.getContentType());
        assertEquals("UTF-8", response.getCharacterSet());
        assertEquals("attachment; filename=export.gen", 
            response.getHeaderField("Content-disposition"));
        assertEquals(NEHEMIAH_2_11b_EXPORT, response.getText());
    }

    public void testCrashJavaWithGetStringOnFeature2() throws Exception
    {
        EmdrosDatabase emdros = Lex.getEmdrosDatabase("test", "localhost");
        
        Sheaf sheaf = emdros.getSheaf
        ( 
            "SELECT ALL OBJECTS IN " +
            "{1-1000000} " +
            "WHERE " +
            "[verse GET book, chapter, verse " +
            " [clause self = 1324989 " +
            "  [word GET phrase_dependent_part_of_speech, person, gender, " +
            "            number, state, wordnet_gloss, lexeme, tense, " +
            "            graphical_preformative, graphical_root_formation, " +
            "            graphical_lexeme, graphical_verbal_ending, " +
            "            graphical_nominal_ending]"+
            " ]"+
            "]"
        );

        SheafConstIterator sci = sheaf.const_iterator();
        assertTrue(sci.hasNext());
        Straw straw = sci.next();
        StrawConstIterator swci = straw.const_iterator();
        assertTrue(swci.hasNext());
        MatchedObject verse = swci.next();
        
        Map bookNumToNameMap = emdros.getEnumerationConstants("book_name_e", 
            false);
        
        assertEquals("verse", verse.getObjectTypeName());
        
        String bookName = (String)bookNumToNameMap.get(
            "" + verse.getEMdFValue("book").getEnum());
        assertEquals("Nehemiah", bookName);

        BookData verseData = KJV.getVerse(emdros, bookName,
            verse.getEMdFValue("chapter").getInt(),
            verse.getEMdFValue("verse").getInt()); 

        sci = verse.getSheaf().const_iterator();
        assertTrue(sci.hasNext());
        straw = sci.next();
        swci = straw.const_iterator();
        assertTrue(swci.hasNext());
        MatchedObject clause = swci.next();

        /*
        verse.getEMdFValue("chapter").getInt();
        verse.getEMdFValue("verse").getInt(); 

        sci = verse.getSheaf().const_iterator();
        straw = sci.next();
        swci = straw.const_iterator();
        MatchedObject clause = swci.next();
        */
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(GenExporterTest.class);
    }
}

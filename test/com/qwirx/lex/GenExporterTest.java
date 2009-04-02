package com.qwirx.lex;

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
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class GenExporterTest extends TestCase
{
    private SqlDatabase m_SQL;

    public GenExporterTest() throws Exception
    {
        m_SQL = Lex.getSqlDatabase("test");
    }
    
    private String getGloss(String predicate) throws Exception
    {
        Lexeme lexeme = Lexeme.load(m_SQL, predicate);
        if (lexeme == null)
        {
            return null;
        }
        else
        {
            return lexeme.getGloss();
        }
    }
    
    private String getNehemiah2_11bExportHebrew()
    throws Exception
    {
        return
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
        "\\gloss " + getGloss("HJH[") + "\n" +
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
        "\\trans yām-\n" +
        "\\tag tag\n" +
        "\\gloss " + getGloss("JM") + "\n" +
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
    }

    private String getNehemiah2_11bExportHebrewTransliterated()
    throws Exception
    {
        return getNehemiah2_11bExportHebrew()
        .replaceAll("\\\\morpheme .*\n", "")
        .replaceAll("\\\\trans (.*)\n", "\\\\morpheme $1\n")
        .replaceAll("\\\\transliterationfield trans\n", "")
        .replaceAll("\\\\righttoleft\n", "\\\\lefttoright\n");
    }
        
    public void testGenExportCode() throws Exception
    {
        SqlDatabase sql = Lex.getSqlDatabase("test");
        EmdrosDatabase emdros = Lex.getEmdrosDatabase("test", "localhost", sql);
        DatabaseTransliterator transliterator = new DatabaseTransliterator(sql);
        
        Sheaf sheaf = emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + emdros.getMinM() + "-" + emdros.getMaxM() + "} " +
            "WHERE " +
            "[clause self = 1323065 " +
            " [phrase "+
            "  [word GET phrase_dependent_part_of_speech, person, gender, " +
            "            number, state, wordnet_gloss, lexeme_wit, tense, stem, " +
            "            graphical_preformative_utf8, " +
            "            graphical_root_formation_utf8, " +
            "            graphical_lexeme_utf8, " +
            "            graphical_verbal_ending_utf8, " +
            "            graphical_nominal_ending_utf8, " +
            "            graphical_pron_suffix_utf8, " +
            "            suffix_gender, suffix_number, suffix_person]"+
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

        assertEquals(getNehemiah2_11bExportHebrew(), 
            new GenExporter().export(clause, verse,
                Lex.getSqlDatabase("test"), transliterator, true));

        assertEquals(getNehemiah2_11bExportHebrewTransliterated(),
            new GenExporter().export(clause, verse,
                Lex.getSqlDatabase("test"), transliterator, false));
    }
    
    public void testGenExportJsp() throws Exception
    {
        WebConversation conv = new WebConversation();
        WebResponse response = conv.getResponse("http://localhost:8080/lex" +
                "/gen-export.jsp?clause=1323065&hebrew=y");
        assertEquals("text/x-gen", response.getContentType());
        assertEquals("UTF-8", response.getCharacterSet());
        assertEquals("attachment; filename=export.gen", 
            response.getHeaderField("Content-disposition"));
        assertEquals(getNehemiah2_11bExportHebrew(), response.getText());

        response = conv.getResponse("http://localhost:8080/lex" +
            "/gen-export.jsp?clause=1323065&hebrew=n");
        assertEquals("text/x-gen", response.getContentType());
        assertEquals("UTF-8", response.getCharacterSet());
        assertEquals("attachment; filename=export.gen", 
            response.getHeaderField("Content-disposition"));
        assertEquals(getNehemiah2_11bExportHebrewTransliterated(),
            response.getText());
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(GenExporterTest.class);
    }
}

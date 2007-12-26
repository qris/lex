package com.qwirx.lex;

import java.io.InputStream;

import junit.framework.TestCase;

import com.qwirx.csv.CommaSeparatedValueParser;
import com.qwirx.lex.hebrew.HebrewEnglishDatabase;

public class HebrewEnglishDatabaseTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HebrewEnglishDatabaseTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    public void test() throws Exception
    {
        // test data/hebrew dictionary from 
        // http://crosswire.org/~scribe/greekheb/hebrewDiB.xls
        
        HebrewEnglishDatabase dict = HebrewEnglishDatabase.getInstance();

        assertNull(dict.get(0));
        assertNotNull(dict.get(1));
        assertNotNull(dict.get(8674));
        assertNull(dict.get(8675));

        HebrewEnglishDatabase.Entry entry = dict.get(1); 
        assertEquals(1, entry.getStrongsNum());
        assertEquals("Hebrew", entry.getLanguage());
        assertEquals("TWOT-4a ", entry.getTWOT());
        assertEquals("Noun Masculine", entry.getForm());
        assertEquals("G1118 G2730 G3390 G3507 G3509 G3962 G3965 G3966 " +
                "G3967 G3971", entry.getGkRelated());
        assertEquals("1) father of an individual <BR> " +
                "2) of God as father of his people <BR> " +
                "3) head or founder of a household, group, family, or clan " +
                "<BR> " +
                "4) ancestor <BR> " +
                "4a) grandfather, forefathers -- of person <BR> " +
                "4b) of people <BR> " +
                "5) originator or patron of a class, " +
                "profession, or art <BR> " +
                "6) of producer, generator (fig.) <BR> " +
                "7) of benevolence and protection (fig.) <BR> " +
                "8) term of respect and honour <BR> " +
                "9) ruler or chief (spec.)", entry.getFullerMeaning());
        assertEquals("אב", entry.getUnpointedHeb());
        assertEquals(")b", entry.getCALUnpointedAscii());
        assertEquals("AB", entry.getTABSUnpointedAscii());
        assertEquals("אָב", entry.getPointedHeb());
        assertEquals("ab", entry.getTransliteration());
        assertEquals("awb", entry.getPhonetic());
        assertEquals("a primitive word;", entry.getNotes());
        assertEquals("father", entry.getMeaning());
        assertEquals("<b>father</b>, in a literal and immediate, or " +
                "figurative and remote application", entry.getFullMeaning());
        assertEquals("chief, (fore-) father(-less), [idiom] patrimony, " +
                "principal. Compare names in 'Abi-'.", 
                entry.getTranslationInAV());
                                
        entry = dict.get(2); 
        assertEquals(2, entry.getStrongsNum());
        assertEquals("Aramaic", entry.getLanguage());
        assertEquals("TWOT-2553 ", entry.getTWOT());
        assertEquals("Noun Masculine", entry.getForm());
        assertEquals("G5 G912", entry.getGkRelated());
        assertEquals("1) father", entry.getFullerMeaning());
        assertEquals("אב", entry.getUnpointedHeb());
        assertEquals(")b", entry.getCALUnpointedAscii());
        assertEquals("AB", entry.getTABSUnpointedAscii());
        assertEquals("אַב", entry.getPointedHeb());
        assertEquals("ab", entry.getTransliteration());
        assertEquals("ab", entry.getPhonetic());
        assertEquals("(Aramaic) corresponding to " +
            "<span class='StNo'>#1</span> " +
            "<span class='Heb'>אָב</span> [<span class='Trans'>'ab</span> " +
            "<span class='Phon'>, awb</span>]", entry.getNotes());
        assertEquals("father", entry.getMeaning());
        assertEquals("{<b>father</b>}", entry.getFullMeaning());
        assertEquals("father.", entry.getTranslationInAV());
                          
        entry = dict.get(3); 
        assertEquals(3, entry.getStrongsNum());
        assertEquals("Hebrew", entry.getLanguage());
        assertEquals("TWOT-1a ", entry.getTWOT());
        assertEquals("Noun Masculine", entry.getForm());
        assertEquals("G1080 G2590 G4491", entry.getGkRelated());
        assertEquals("1) freshness, fresh green, green shoots, or greenery",
            entry.getFullerMeaning());
        assertEquals("אב", entry.getUnpointedHeb());
        assertEquals(")b", entry.getCALUnpointedAscii());
        assertEquals("AB", entry.getTABSUnpointedAscii());
        assertEquals("אֵב", entry.getPointedHeb());
        assertEquals("eb", entry.getTransliteration());
        assertEquals("abe", entry.getPhonetic());
        assertEquals("from the same as <span class='StNo'>#24</span> " +
            "<span class='Heb'>אָבִיב</span> " +
            "[<span class='Trans'>'abiyb</span> " +
            "<span class='Phon'>, aw-beeb'</span>];", entry.getNotes());
        assertEquals("a green plant", entry.getMeaning());
        assertEquals("<b>a green plant</b>", entry.getFullMeaning());
        assertEquals("greenness, fruit.", entry.getTranslationInAV());
        
        entry = dict.get(4); 
        assertEquals(4, entry.getStrongsNum());
        assertEquals("Aramaic", entry.getLanguage());
        assertEquals("TWOT-2554 ", entry.getTWOT());
        assertEquals("Noun Masculine", entry.getForm());
        assertEquals("", entry.getGkRelated());
        assertEquals("1) fruit, fresh, young, greening",
            entry.getFullerMeaning());
        assertEquals("אב", entry.getUnpointedHeb());
        assertEquals(")b", entry.getCALUnpointedAscii());
        assertEquals("AB", entry.getTABSUnpointedAscii());
        assertEquals("אֵב", entry.getPointedHeb());
        assertEquals("eb", entry.getTransliteration());
        assertEquals("abe", entry.getPhonetic());
        assertEquals("(Aramaic) corresponding to " +    
            "<span class='StNo'>#3</span> " +
            "<span class='Heb'>אֵב</span> [<span class='Trans'>'eb</span> " +
            "<span class='Phon'>, abe</span>]", entry.getNotes());
        assertEquals("a green plant", entry.getMeaning());
        assertEquals("{<b>a green plant</b> }", entry.getFullMeaning());
        assertEquals("fruit.", entry.getTranslationInAV());

        for (int i = 1; i < 8675; i++)
        {
            assertEquals(i, dict.get(i).getStrongsNum());
        }
    }

}

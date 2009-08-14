package com.qwirx.lex.test.active;

import java.io.InputStream;

import junit.framework.TestCase;

import com.qwirx.csv.CommaSeparatedValueParser;

public class CommaSeparatedValueParserTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(CommaSeparatedValueParserTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }
    
    /*
     * Test method for 'com.qwirx.csv.CommaSeparatedValueParser.parse(InputStream)'
     */
    public void testParse() throws Exception
    {
        // test data/hebrew dictionary from 
        // http://crosswire.org/~scribe/greekheb/hebrewDiB.xls
        
        InputStream fis = getClass().getResourceAsStream(
            "/com/qwirx/lex/hebrew/crosswire_hebrew_english_dict.csv");
        assertNotNull(fis);
        String [][] parse = CommaSeparatedValueParser.parse(fis);

        assertEquals(8675, parse.length);
        assertEquals(16, parse[0].length);
        assertEquals("StrongsNo", parse[0][0]);
        assertEquals("Language", parse[0][1]);
        assertEquals("TWOT", parse[0][2]);
        assertEquals("Form",parse[0][3]);
        assertEquals("GkRelated", parse[0][4]);
        assertEquals("FullerMeaning", parse[0][5]);
        assertEquals("UnpointedHeb", parse[0][6]);
        assertEquals("CALUnpointedAscii", parse[0][7]);
        assertEquals("TABSUnpointedAscii", parse[0][8]);
        assertEquals("PointedHeb", parse[0][9]);
        assertEquals("Transliteration", parse[0][10]);
        assertEquals("Phonetic",parse[0][11]);
        assertEquals("Notes", parse[0][12]);
        assertEquals("Meaning", parse[0][13]);
        assertEquals("FullMeaning", parse[0][14]);
        assertEquals("TranslationInAV", parse[0][15]);

        assertEquals(16, parse[1].length);
        assertEquals("1", parse[1][0]);
        assertEquals("Hebrew", parse[1][1]);
        assertEquals("TWOT-4a ", parse[1][2]);
        assertEquals("Noun Masculine", parse[1][3]);
        assertEquals("G1118 G2730 G3390 G3507 G3509 G3962 G3965 G3966 " +
                "G3967 G3971", parse[1][4]);
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
                "9) ruler or chief (spec.)", parse[1][5]);
        assertEquals("אב", parse[1][6]);
        assertEquals(")b", parse[1][7]);
        assertEquals("AB", parse[1][8]);
        assertEquals("אָב", parse[1][9]);
        assertEquals("ab", parse[1][10]);
        assertEquals("awb", parse[1][11]);
        assertEquals("a primitive word;", parse[1][12]);
        assertEquals("father", parse[1][13]);
        assertEquals("<b>father</b>, in a literal and immediate, or " +
                "figurative and remote application", parse[1][14]);
        assertEquals("chief, (fore-) father(-less), [idiom] patrimony, " +
                "principal. Compare names in 'Abi-'.", parse[1][15]);
                                
        assertEquals(16, parse[2].length);
        assertEquals("2", parse[2][0]);
        assertEquals("Aramaic", parse[2][1]);
        assertEquals("TWOT-2553 ", parse[2][2]);
        assertEquals("Noun Masculine", parse[2][3]);
        assertEquals("G5 G912", parse[2][4]);
        assertEquals("1) father", parse[2][5]);
        assertEquals("אב", parse[2][6]);
        assertEquals(")b", parse[2][7]);
        assertEquals("AB", parse[2][8]);
        assertEquals("אַב", parse[2][9]);
        assertEquals("ab", parse[2][10]);
        assertEquals("ab", parse[2][11]);
        assertEquals("(Aramaic) corresponding to <span class='StNo'>#1</span> " +
                "<span class='Heb'>אָב</span> [<span class='Trans'>'ab</span> " +
                "<span class='Phon'>, awb</span>]", parse[2][12]);
        assertEquals("father", parse[2][13]);
        assertEquals("{<b>father</b>}", parse[2][14]);
        assertEquals("father.", parse[2][15]);
                          
        assertEquals(16, parse[3].length);
        assertEquals("3", parse[3][0]);
        assertEquals("Hebrew", parse[3][1]);
        assertEquals("TWOT-1a ", parse[3][2]);
        assertEquals("Noun Masculine", parse[3][3]);
        assertEquals("G1080 G2590 G4491", parse[3][4]);
        assertEquals("1) freshness, fresh green, green shoots, or greenery",
            parse[3][5]);
        assertEquals("אב", parse[3][6]);
        assertEquals(")b", parse[3][7]);
        assertEquals("AB", parse[3][8]);
        assertEquals("אֵב", parse[3][9]);
        assertEquals("eb", parse[3][10]);
        assertEquals("abe", parse[3][11]);
        assertEquals("from the same as <span class='StNo'>#24</span> " +
                "<span class='Heb'>אָבִיב</span> " +
                "[<span class='Trans'>'abiyb</span> " +
                "<span class='Phon'>, aw-beeb'</span>];", parse[3][12]);
        assertEquals("a green plant", parse[3][13]);
        assertEquals("<b>a green plant</b>", parse[3][14]);
        assertEquals("greenness, fruit.", parse[3][15]);
        
        assertEquals(16, parse[4].length);
        assertEquals("4", parse[4][0]);
        assertEquals("Aramaic", parse[4][1]);
        assertEquals("TWOT-2554 ", parse[4][2]);
        assertEquals("Noun Masculine", parse[4][3]);
        assertEquals("", parse[4][4]);
        assertEquals("1) fruit, fresh, young, greening", parse[4][5]);
        assertEquals("אב", parse[4][6]);
        assertEquals(")b", parse[4][7]);
        assertEquals("AB", parse[4][8]);
        assertEquals("אֵב", parse[4][9]);
        assertEquals("eb", parse[4][10]);
        assertEquals("abe", parse[4][11]);
        assertEquals("(Aramaic) corresponding to <span class='StNo'>#3</span> " +
                "<span class='Heb'>אֵב</span> [<span class='Trans'>'eb</span> " +
                "<span class='Phon'>, abe</span>]", parse[4][12]);
        assertEquals("a green plant", parse[4][13]);
        assertEquals("{<b>a green plant</b> }", parse[4][14]);
        assertEquals("fruit.", parse[4][15]);

        for (int i = 0; i < parse.length; i++)
        {
            assertEquals("Mismatched length on row " + i, 16, parse[i].length);
        }
    }

}

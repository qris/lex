package com.qwirx.lex.hebrew;

import java.io.IOException;

import org.xml.sax.SAXException;

import jemdros.MatchedObject;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.MorphemeHandler;


/*
 * Based on:
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * File    : QuestFontmap.dat
 * Format  : *char~sequence* *unicode~points* // Comments
 * Creator : Eli Evans
 * Created : 6/11/2002
 * Purpose : Provides a mapping between Dr. Bader's Quest Hebrew trans-
 *         : literation (sortof like Michegan/Claremont + Beta code + some
 *         : changes to make sense in German + some extensions) and Unicode
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * NOTES:
 * (1) Sequences on the left are literal and should be applied to the data in
 *     longest-sequence-first order. Therefore, the ordering of mappings in
 *     this file is not significant.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

public class HebrewConverter
{
    public static String toHtml(String input)
    {
        StringBuffer output = new StringBuffer();
        
        for (int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if (c == '<')
            {
                output.append("&lt;");
            }
            else if (c == '>')
            {
                output.append("&gt;");
            }
            else if (c == '&')
            {
                output.append("&amp;");
            }
            else
            {
                output.append(c);
            }
        }
        
        return output.toString();
    }
    
    public static String toHebrew(String input)
    {
        StringBuffer output = new StringBuffer();
        
        for (int i = 0; i < input.length(); i++)
        {
            String substr = input.substring(i);

            char c = input.charAt(i);
            char c2 = 0xffff;
            if (i < input.length() - 1)
            {
                c2 = input.charAt(i + 1);
            }

            if      (c == '.') { output.append("\u05bc"); } // Dagesh 
            else if (c == ',') { output.append("\u05bf"); } // Rafe
            
            else if (c == ':') 
            {
                i++;
                if      (c2 == '@') { output.append("\u05b3"); } // Hataph Qamets
                else if (c2 == 'A') { output.append("\u05b2"); } // Hataph Patah
                else if (c2 == 'E') { output.append("\u05b1"); } // Hataph Segol
                else    { i--;        output.append("\u05b0"); } // Sheva
            }
            
            else if (c == ';') { output.append("\u05b5"); } // Tsere
            else if (c == '<') { output.append("\u05e2"); } // Ayin
            else if (c == '>') { output.append("\u05d0"); } // Aleph
            else if (c == '@') { output.append("\u05b8"); } // Qamets
            else if (c == 'A') { output.append("\u05b7"); } // Patah
            else if (c == 'B') { output.append("\u05d1"); } // Bet
            else if (c == 'C') { output.append("\u05e9\u05c1"); } // Shin, Shin Dot
            else if (c == '#') { output.append("\u05e9"); } // S/hin with no dot
            else if (c == 'D') { output.append("\u05d3"); } // Dalet
            else if (c == 'E') { output.append("\u05b6"); } // Segol
            else if (c == 'F') { output.append("\u05e9\u05c2"); } // Shin, Sin Dot
            else if (c == 'G') { output.append("\u05d2"); } // Gimel
            else if (c == 'H') { output.append("\u05d4"); } // He
            else if (c == 'I') { output.append("\u05b4"); } // Hiriq
            else if (c == 'J') { output.append("\u05d9"); } // Yod
            else if (c == 'K') { output.append("\u05db"); } // Kaph
            else if (c == 'L') { output.append("\u05dc"); } // Lamed
            else if (c == 'M') { output.append("\u05de"); } // Mem
            else if (c == 'N') { output.append("\u05e0"); } // Nun
            else if (c == 'O') { output.append("\u05b9"); } // Holam
            else if (c == 'P') { output.append("\u05e4"); } // Pe
            else if (c == 'Q') { output.append("\u05e7"); } // Qoph
            else if (c == 'R') { output.append("\u05e8"); } // Resh
            else if (c == 'S') { output.append("\u05e1"); } // Samek
            else if (c == 'T') { output.append("\u05ea"); } // Tav
            else if (c == 'U') { output.append("\u05bb"); } // Qibbuts
            else if (c == 'V') { output.append("\u05d8"); } // Tet
            else if (c == 'W') { output.append("\u05d5"); } // Waw
            else if (c == 'X') { output.append("\u05d7"); } // Khet
            else if (c == 'Y') { output.append("\u05e6"); } // Tsade
            else if (c == 'Z') { output.append("\u05d6"); } // Zayin
            else if (c == 'k') { output.append("\u05da"); } // Final Kaph
            else if (c == 'm') { output.append("\u05dd"); } // Final Mem
            else if (c == 'n') { output.append("\u05df"); } // Final Nun
            else if (c == 'p') { output.append("\u05e3"); } // Final Pe
            else if (c == 'y') { output.append("\u05e5"); } // Final Tsade

            // Cantillation Marks/Punctuation ala MC
            // removed at Nicolai's request 2/1/08
            /*
            else if (c == '0')
            {
                i++;
                if      (c2 == '0') { output.append("\u05c3"); } // sof pasuq (handled in script)
                else if (c2 == '1') { output.append("\u0592"); } // segol
                else if (c2 == '2') { output.append("\u05ae"); } // zarqa; tsinnor
                else if (c2 == '3') { output.append("\u0599"); } // pashta
                else if (c2 == '4') { output.append("\u05a9"); } // tlisha qtana
                else if (c2 == '5') { output.append("\u05c0"); } // legarmeh; paseq
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '1')
            {
                i++;
                if      (c2 == '0') { output.append("\u059a"); } // yetiv
                else if (c2 == '1') { output.append("\u059d"); } // geresh muqdam
                else if (c2 == '3') { output.append("\u05ad"); } // dechi
                else if (c2 == '4') { output.append("\u05a0"); } // tlisha gdola
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '2')
            {
                i++;
                if (c2 == '4') { output.append("\u05a9"); } // tlisha qtana
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '3')
            {
                i++;
                if      (c2 == '3') { output.append("\u05a8"); } // pashta
                else if (c2 == '5') { output.append("\u05bd"); } // meteg (between)
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '4')
            {
                i++;
                if (c2 == '4') { output.append("\u05a0"); } // tlisha gdola
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '5')
            {
                i++;
                if      (c2 == '2') { output.append("\u05c4"); } // Puncta Extraordinaria above (not a revia!)
                else if (c2 == '3') { output.append("\u0323"); } // Puncta Extraordinaria below; Ps 27:13 only
                else { throw new IllegalArgumentException(substr); }
            }

            else if (c == '6')
            {
                i++;
                if      (c2 == '0') { output.append("\u05ab"); } // ole
                else if (c2 == '1') { output.append("\u059c"); } // geresh
                else if (c2 == '2') { output.append("\u059e"); } // gershayim
                else if (c2 == '3') { output.append("\u05a8"); } // qadma; azla
                else if (c2 == '4') { output.append("\u05ac"); } // illuy
                else if (c2 == '5') { output.append("\u0593"); } // shalshelet
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '7')
            {
                i++;
                if      (c2 == '0') { output.append("\u05a4"); } // mahpakh
                else if (c2 == '1') { output.append("\u05a5"); } // merkha; yored
                else if (c2 == '2') { output.append("\u05a6"); } // merkha khfula
                else if (c2 == '3') { output.append("\u0596"); } // tipcha; m�ayla; tarcha
                else if (c2 == '4') { output.append("\u05a3"); } // munnach
                else if (c2 == '5') { output.append("\u05bd"); } // meteg, silluq (standard=left)
                else { throw new IllegalArgumentException(c + c2 + " / " + substr); }
            }
            
            else if (c == '8')
            {
                i++;
                if      (c2 == '0') { output.append("\u0594"); } // zaqef qatan
                else if (c2 == '1') { output.append("\u0597"); } // revia
                else if (c2 == '2') { output.append("\u0598"); } // tsinnorit
                else if (c2 == '3') { output.append("\u05a1"); } // pazer
                else if (c2 == '4') { output.append("\u059f"); } // qarney para
                else if (c2 == '5') { output.append("\u0595"); } // zaqef gadol
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '9')
            {
                i++;
                if      (c2 == '1') { output.append("\u059b"); } // tvir
                else if (c2 == '2') { output.append("\u0591"); } // atnach
                else if (c2 == '3') { output.append("\u05aa"); } // galgal; atnach hafukh
                else if (c2 == '4') { output.append("\u05a7"); } // darga
                else if (c2 == '5') { output.append("\u05bd"); } // meteg, silluq (right)
                else { throw new IllegalArgumentException(substr); }
            }*/
            
            else if (c >= '0' && c <= '9' && c2 >= '0' && c2 <= '9')
            {
                i++; // skip both
            }
            
            // Extended Punctuation

            else if (c == '&') { output.append("\u05be"); } // Maqaf
            else if (c == '-') { /* output.append("\\u0"); */ } // Word continues
            else if (c == '_') { output.append("\u0020"); } // space
            else if (c == '|') { output.append("\u05c6"); } // Nun Inversum 
            // (note that this is artificially produced from N by 
            // wit2utf8.py:get_suffix_and_form_stripped_of_suffix

            // Other

            else if (c == '*') { /* 0 */ } // asterisk indicates Qere/Ketiv

            else if (c == '[') { /* 0 */ } // Don't know function
            else if (c == '!') { /* 0 */ } // Don't know function
            else if (c == '+') { /* 0 */ } // Don't know function
            else if (c == '=') { /* 0 */ } // Don't know function
            else if (c == ']') { /* 0 */ } // Don't know function
            else if (c == '~') { /* 0 */ } // Don't know function
            else if (c == '2') { /* 0 */ } // Don't know function
            else if (c == '/') { output.append("\u059c"); } // Geresh? (That's a guess)
            else
            {
                throw new IllegalArgumentException(substr);
            }
        }
        
        return output.toString();
    }
    
    public static String toTranslit(String input)
    {
        input = input.toUpperCase().replaceAll("\\d\\d", "");
        StringBuffer output = new StringBuffer();

        for (int i = 0; i < input.length(); i++)
        {
            String substr = input.substring(i);
            char c = input.charAt(i);
            char c2 = 0xffff;
            if (i < input.length() - 1)
            {
                c2 = input.charAt(i + 1);
            }

            if (substr.matches("[BGDKPT]\\.[@AEIOUW;:].*"))
            {
                output.append(substr.substring(0, 1).toLowerCase());
                i++; // skip the dagesh (.)
            }
            else if (substr.matches("[BCDFGJKLMNPQSTVWYZ#]\\..*"))
            {
                if      (c == 'C') { output.append("šš"); }
                else if (c == 'F') { output.append("śś"); }
                else if (c == '#') { output.append("ss"); } // ?? guess ??
                else if (c == 'J') { output.append("yy"); }
                else if (c == 'V') { output.append("ťť"); }
                else if (c == 'W') { output.append("ū");  }
                else if (c == 'Y') { output.append("tsts"); }
                else
                {
                    String s = "" + c + c;
                    output.append(s.toLowerCase());
                }
                
                i++; // skip the dagesh (.)
            }
            else if (c == '>') { output.append("?"); }
            else if (c == 'B') { output.append("v"); }
            else if (c == 'X') { output.append("x"); }
            else if (c == 'V') { output.append("ť"); }
            else if (c == '<') { output.append("¿"); }
            else if (c == 'P') { output.append("f"); }
            else if (c == 'Y') { output.append("c"); }
            else if (c == 'F') { output.append("ś"); }
            else if (c == 'C') { output.append("š"); }
            else if (c == '#') { output.append("s"); } // ?? guess ??
            else if (c == '@') { output.append("ā"); }
            else if (c == 'D' || c == 'G' || c == 'H' || 
                c == 'J' || c == 'K' || c == 'L' || c == 'M' ||
                c == 'N' || c == 'Q' || c == 'R' || c == 'S' || 
                c == 'T' || c == 'W' || c == 'Z')
            {
                output.append(("" + c).toLowerCase());
            }
            else if (substr.matches(":A.*"))
            {
                output.append("ă");
                i++;
            }
            else if (c == 'A') { output.append("a"); }
            else if (substr.matches(":O.*"))
            {
                output.append("ŏ");
                i++;
            }
            else if (substr.matches("OW.*"))
            {
                output.append("ô");
                i++;
            }
            else if (c == 'O') { output.append("ō"); }
            else if (c == ';') { output.append("ē"); }
            else if (substr.matches(":E.*"))
            {
                output.append("ĕ");
                i++;
            }
            else if (c == 'E') { output.append("e"); }
            else if (c == 'U') { output.append("u"); }
            else if (substr.matches("IJ.*"))
            {
                output.append("î");
                i++;
            }
            else if (c == 'I') { output.append("i"); }
            else if (c == ':') { output.append("ə"); }
            
            /* not sure about these */
            else if (c == '&') { } // Maqaf
            else if (c == '-') { output.append("-"); } // Word continues
            else if (c == '_') { } // space
            else if (c == '|') { } // Nun Inversum 
            // (note that this is artificially produced from N by 
            // wit2utf8.py:get_suffix_and_form_stripped_of_suffix
            else if (c == '*') { } // asterisk indicates Qere/Ketiv
            else if (c == '[') { } // Don't know function
            else if (c == '!') { } // Don't know function
            else if (c == '+') { } // Don't know function
            else if (c == '=') { } // Don't know function
            else if (c == ']') { } // Don't know function
            else if (c == '~') { } // Don't know function
            else if (c == '2') { } // Don't know function
            else if (c == '/') { } // Geresh? (That's a guess)
            else if (c == '.')
            {
                // dagesh, double the last letter
                output.append(output.charAt(output.length() - 1));
            }

            else
            {
                throw new AssertionError("Unknown code: "+substr + 
                    " (in " + input + ")");
            }
        }
        
        return output.toString();
    }
    
    static class Transliterator implements MorphemeHandler
    {
        private MatchedObject m_Word;
        private StringBuffer  m_Output;
        
        public Transliterator(MatchedObject word, StringBuffer output)
        {
            m_Word   = word;
            m_Output = output;
        }
        
        public void convert(String surface, 
            boolean lastMorpheme, String desc,
            String morphNode)
        {
            String raw = m_Word.getEMdFValue(surface).getString();
            String translit = HebrewConverter.toTranslit(raw);
            m_Output.append(translit);
        }
    }

    static class Hebrewator implements MorphemeHandler
    {
        private MatchedObject m_Word;
        private StringBuffer  m_Output;
        
        public Hebrewator(MatchedObject word, StringBuffer output)
        {
            m_Word   = word;
            m_Output = output;
        }
        
        public void convert(String surface, 
            boolean lastMorpheme, String desc,
            String morphNode)
        {
            String raw = m_Word.getEMdFValue(surface).getString();
            String hebrew = HebrewConverter.toHebrew(raw);
            m_Output.append(hebrew);
        }
    }

    public static String wordToHtml(MatchedObject word, EmdrosDatabase emdros)
    throws IOException, DatabaseException, SAXException
    {
        return wordTranslitToHtml(word, new HebrewMorphemeGenerator(emdros));
    }

    public static String wordTranslitToHtml(MatchedObject word, 
        HebrewMorphemeGenerator generator)
    {
        StringBuffer out = new StringBuffer();
        Transliterator xlit = new Transliterator(word, out);
        generator.parse(word, xlit, false);
        return toHtml(out.toString());
    }

    public static String wordHebrewToHtml(MatchedObject word, 
        HebrewMorphemeGenerator generator)
    {
        StringBuffer out = new StringBuffer();
        Hebrewator xlit = new Hebrewator(word, out);
        generator.parse(word, xlit, false);
        return toHtml(out.toString());
    }
}
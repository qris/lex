package com.qwirx.lex;


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
    public static String convert(String input)
    {
        StringBuffer output = new StringBuffer();
        
        for (int i = 0; i < output.length();)
        {
            char c  = input.charAt(i++);
            char c2 = input.charAt(i);
            String substr = input.substring(i, i + 2);
            
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
            else if (c == 'C') { output.append("\u05e9"); } // 05c1 // Shin, Shin Dot
            else if (c == '#') { output.append("\u05e9"); } // S/hin with no dot
            else if (c == 'D') { output.append("\u05d3"); } // Dalet
            else if (c == 'E') { output.append("\u05b6"); } // Segol
            else if (c == 'F') { output.append("\u05e9"); } // 05c2 // Shin, Sin Dotelse if (c == 'G') { output.append("\u05d2"); } // Gimel
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
            
            else if (c == '2' && c2 == '4')
            { i++; output.append("\u05a9"); } // tlisha qtana
            
            else if (c == '3')
            {
                i++;
                if      (c2 == '3') { output.append("\u05a8"); } // pashta
                else if (c2 == '5') { output.append("\u05bd"); } // meteg (between)
                else { throw new IllegalArgumentException(substr); }
            }
            
            else if (c == '4' && c2 == '4') 
            { i++; output.append("\u05a0"); } // tlisha gdola
            
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
                else { throw new IllegalArgumentException(substr); }
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
                if      (c2 == '1') { output.append("\u059b"); } // tvir
                else if (c2 == '2') { output.append("\u0591"); } // atnach
                else if (c2 == '3') { output.append("\u05aa"); } // galgal; atnach hafukh
                else if (c2 == '4') { output.append("\u05a7"); } // darga
                else if (c2 == '5') { output.append("\u05bd"); } // meteg, silluq (right)
                else { throw new IllegalArgumentException(substr); }
            }
            
            // Extended Punctuation

            else if (c == '&') { output.append("\u05be"); } // Maqaf
            else if (c == '-') { /* output.append("\\u0"); */ } // Word continues
            else if (c == '_') { output.append("\u0020"); } // space
            else if (c == '|') { output.append("\u05c6"); } // Nun Inversum 
            // (note that this is artificually produced from N by 
            // wit2utf8.py:get_suffix_and_form_stripped_of_suffix

            // Other

            else if (c == '5')
            {
                i++;
                if      (c2 == '2') { output.append("\u05c4"); } // Puncta Extraordinaria above (not a revia!)
                else if (c2 == '3') { output.append("\u0323"); } // Puncta Extraordinaria below; Ps 27:13 only
                else { throw new IllegalArgumentException(substr); }
            }

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
}

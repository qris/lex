package com.qwirx.lex;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;
import com.qwirx.lex.translit.DatabaseTransliterator;
import com.qwirx.lex.translit.DatabaseTransliterator.Rule;

public class TransliteratorTest extends TestCase
{
    SqlDatabase m_sql;
    
    public TransliteratorTest() throws Exception
    {
        m_sql = Lex.getSqlDatabase("test", "test");
    }
    
    private Rule addRule(String precedent, String original, String succeedent,
        String replacement)
    throws Exception
    {
        /* pattern syntax check */
        Rule r = new DatabaseTransliterator.Rule(precedent, original,
            succeedent, replacement);
        PreparedStatement stmt = m_sql.prepareSelect("INSERT INTO " +
                "translit_rules SET Precedent = ?, Original = ?, " +
                "Succeedent = ?, Replacement = ?");
        stmt.setString(1, precedent);
        stmt.setString(2, original);
        stmt.setString(3, succeedent);
        stmt.setString(4, replacement);
        stmt.execute();
        m_sql.finish();
        
        return r;
    }
    
    private DatabaseTransliterator m_trans;
    
    public void testDatabaseTransliterator() throws Exception
    {
        m_sql.beginTransaction();
        m_sql.executeDirect("DELETE FROM translit_rules");

        /* characters and symbols */
        final String ALEPH = "\u05d0";
        final String AYIN = "\u05e2";
        final String PATAH = "\u05b7";
        final String BET = "\u05d1";
        final String DAGESH = "\u05bc";
        final String SHEVA = "\u05b0";
        final String TSERE = "\u05b5";
        final String QAMETS = "\u05b8";
        final String HATAPH_QAMETS = "\u05b3";
        final String HATAPH_PATAH = "\u05b2";
        final String HATAPH_SEGOL = "\u05b1";
        final String SOF_PASUQ = "\u05c3";
        final String HE = "\u05d4";
        final String YOD = "\u05d9";
        final String WAW = "\u05d5";
        final String KHET = "\u05d7";
        final String SHIN = "\u05e9\u05c1";
        final String DALET = "\u05d3";
        final String SEGOL = "\u05b6";
        final String SIN = "\u05e9\u05c2";
        final String GIMEL = "\u05d2";
        final String HIRIQ = "\u05b4";
        final String KAPH = "\u05db";
        final String FINAL_KAPH = "\u05da";
        final String LAMED ="\u05dc";
        final String MEM = "\u05de";
        final String FINAL_MEM = "\u05dd";
        final String NUN = "\u05e0";
        final String FINAL_NUN = "\u05df";
        final String HOLAM = "\u05b9";
        final String PE = "\u05e4";
        final String FINAL_PE = "\u05e3";
        final String QOPH = "\u05e7";
        final String RESH = "\u05e8";
        final String SAMEK = "\u05e1";
        final String TAV = "\u05ea";
        final String QIBBUTS = "\u05bb";
        final String TET = "\u05d8";
        final String TSADE = "\u05e6";
        final String FINAL_TSADE = "\u05e5";
        final String ZAYIN = "\u05d6";
        
        /* pattern matching rules (regular expressions) */
        final String CONSONANT = "(" +
                SHIN + "|" + 
                SIN + "|" +
                "[" +
                ALEPH +
                AYIN +
                BET +
                DALET +
                GIMEL +
                HE +
                YOD +
                KAPH +
                LAMED +
                MEM +
                NUN +
                PE +
                QOPH +
                RESH +
                SAMEK +
                TAV +
                TET +
                WAW +
                KHET +
                TSADE + /* Y */
                ZAYIN + /* Z */
                "])";
        final String CANTILLATION = "[" +
            "\u05c3" +
            "\u0592" +
            "\u05ae" +
            "\u0599" +
            "\u05a9" +
            "\u05c0" +
            "\u059a" +
            "\u059d" +
            "\u05ad" +
            "\u05a0" +
            "\u05a8" +
            "\u05c4" +
            "\u0323" +
            "\u05ab" +
            "\u059c" +
            "\u059e" +
            "\u05a8" +
            "\u05ac" +
            "\u0593" +
            "\u05a4" +
            "\u05a5" +
            "\u05a6" +
            "\u0596" +
            "\u05a3" +
            "\u05bd" +
            "\u0594" +
            "\u0597" +
            "\u0598" +
            "\u05a1" +
            "\u059f" +
            "\u0595" +
            "\u059b" +
            "\u0591" +
            "\u05aa" +
            "\u05a7" +
            "\u05bd" +
            "]";
        final String OPTIONAL_CANTILLATION = CANTILLATION + "?";
        final String OPTIONAL_DAGESH = DAGESH + "?";
        final String CONSONANT_DAGESH_CANTILLATION = CONSONANT +
            OPTIONAL_DAGESH + OPTIONAL_CANTILLATION;
        final String CONSONANT_NO_DAGESH_CANTILLATION = CONSONANT +
            OPTIONAL_CANTILLATION;
        final String LONG_VOWEL = "(\u05b5|\u05b9|" + WAW + DAGESH + ")"; /* ;|O|W. */
        final String VOWEL = "([" + TSERE + QAMETS +
            PATAH + SEGOL + HIRIQ + "\u05b9" /* holam */ +
            "\u05bb" /* qibbuts */ + "]|" + WAW + DAGESH + ")";
        final String NOT_VOWEL_BEFORE = "(" +
                "^" +
                "|[^" + TSERE + QAMETS + 
                "\u05b7" /* patah */ + "\u05b6" /* segol */ +
                "\u05b4" /* hiriq */ + "\u05b9" /* holam */ +
                "\u05bb" /* qibbuts */ + WAW + "]" +
                "|(" + WAW + ")" +
                "|(" + WAW + "[^" + DAGESH + "]))";
        final String NOT_VOWEL_AFTER = "($" +
                "|[^" + TSERE + QAMETS + 
                "\u05b7" /* patah */ + "\u05b6" /* segol */ +
                "\u05b4" /* hiriq */ + "\u05b9" /* holam */ +
                "\u05bb" /* qibbuts */ + WAW + "]" +
                "|(" + WAW + "$)" +
                "|(" + WAW + "[^" + DAGESH + "]))";

        /* output symbols (transliterations) */
        final String SUPERSCRIPT_BACKWARDS_E = "\u1d4a";
        final String SUPERSCRIPT_A = "\u1d43";
        final String SUPERSCRIPT_E = "\u1d49";
        final String SUPERSCRIPT_H = "\u02b0";
        final String SUPERSCRIPT_O = "\u1d52";
        final String SUPERSCRIPT_Y = "\u02b8";
        final String SUPERSCRIPT_QUERY = "\u02c0";
        final String SUBSCRIPT_A = "\u2090";
        final String A_BAR = "\u0101";
        final String E_BAR = "\u0113";
        final String O_BAR = "\u014d";
        final String E_CIRCUMFLEX = "\u00ea";
        final String HOOK_RIGHT = "\u0295";
        final String HOOK_LEFT = "\u0294";
        final String H_DOT = "\u1e25";
        final String S_DOT = "\u1e63";
        final String T_DOT = "\u1e6d";
        final String S_CARON = "\u0161";
        final String S_ACUTE = "\u015b";
        final String I_CARET = "\u00ee";
        final String O_CARET = "\u00f4";
        final String U_CARET = "\u00fb";

        addRule("", "\u05bf", "", "");
        addRule("", "\u05bc", "", "");
        addRule("", "\u05be", "", "-");
        addRule("", "/", "", "");
        Rule sheva1 = addRule("^" + CONSONANT_DAGESH_CANTILLATION, SHEVA, "",
            SUPERSCRIPT_BACKWARDS_E);
        Rule sheva2 = addRule("", SHEVA, "$", "");
        Rule sheva3 = addRule(CONSONANT_DAGESH_CANTILLATION, SHEVA,
            CONSONANT_DAGESH_CANTILLATION + SHEVA + "$", "");
        Rule sheva4 = addRule(LONG_VOWEL + CONSONANT_DAGESH_CANTILLATION,
            SHEVA, "", SUPERSCRIPT_BACKWARDS_E);
        Rule sheva5 = addRule(QAMETS + CANTILLATION +
            CONSONANT_DAGESH_CANTILLATION, SHEVA, "", SUPERSCRIPT_BACKWARDS_E);
        Rule sheva6 = addRule(CONSONANT + DAGESH + OPTIONAL_CANTILLATION,
            SHEVA, "", SUPERSCRIPT_BACKWARDS_E);
        Rule sheva7 = addRule(CONSONANT + OPTIONAL_CANTILLATION,
            SHEVA, "", "");
        addRule("", HATAPH_QAMETS, "", SUPERSCRIPT_O);
        addRule("", HATAPH_PATAH, "", SUPERSCRIPT_A);
        addRule("", HATAPH_SEGOL, "", SUPERSCRIPT_E);
        addRule("", TSERE + OPTIONAL_CANTILLATION + HE, "$",
            E_BAR + SUPERSCRIPT_H);
        addRule("", TSERE + OPTIONAL_CANTILLATION + HE + DAGESH, "$",
            E_BAR + "h");
        addRule("", TSERE + OPTIONAL_CANTILLATION + YOD, "",
            E_CIRCUMFLEX);
        addRule("", TSERE, "", E_BAR);
        addRule("", QAMETS + OPTIONAL_CANTILLATION + HE, "$",
            A_BAR + SUPERSCRIPT_H);
        addRule("", QAMETS + OPTIONAL_CANTILLATION + HE + DAGESH, "$",
            A_BAR + "h");
        addRule("", QAMETS + OPTIONAL_CANTILLATION + YOD, WAW,
            A_BAR + SUPERSCRIPT_Y);
        addRule("", QAMETS + OPTIONAL_CANTILLATION + YOD, "$",
            A_BAR + SUPERSCRIPT_Y);
        addRule("", QAMETS, CONSONANT_DAGESH_CANTILLATION + SHEVA, "o");
        addRule("", QAMETS, CONSONANT + OPTIONAL_DAGESH + "$", "o");
        addRule("", QAMETS, "", A_BAR);
        addRule("", "\u05c6", "", ""); /* Nun Inversum */
        addRule("", SOF_PASUQ, "", ":");
        addRule("", "\u0592", "", ""); /* segol */
        addRule("", "\u05ae", "", ""); /* zarqa/tsinnor */
        addRule("", "\u0599", "", ""); /* pashta */
        addRule("", "\u05a9", "", ""); /* tlisha qtana */ 
        addRule("", "\u05c0", "", ""); /* legarmeh, paseq */
        addRule("", "\u059a", "", ""); /* yetiv */ 
        addRule("", "\u059d", "", ""); /* geresh muqdam */  
        addRule("", "\u05ad", "", ""); /* dechi */
        addRule("", "\u05a0", "", ""); /* tlisha gdola */ 
        addRule("", "\u05a8", "", ""); /* pashta */
        addRule("", "\u05c4", "", ""); /* Puncta Extraordinaria above */ 
        addRule("", "\u0323", "", ""); /* Puncta Extraordinaria below */
        addRule("", "\u05ab", "", ""); /* ole */
        addRule("", "\u059c", "", ""); /* geresh */ 
        addRule("", "\u059e", "", ""); /* gershayim */
        addRule("", "\u05a8", "", ""); /* qadma, azla (compare "33") */
        addRule("", "\u05ac", "", ""); /* illuy */
        addRule("", "\u0593", "", ""); /* shalshelet */ 
        addRule("", "\u05a4", "", ""); /* mahpakh */
        addRule("", "\u05a5", "", ""); /* merkha, yored */ 
        addRule("", "\u05a6", "", ""); /* merkha khfula */
        addRule("", "\u0596", "", ""); /* tipcha, mëayla, tarcha */ 
        addRule("", "\u05a3", "", ""); /* munnach */
        addRule("", "\u05bd", "", ""); /* meteg, silluq (standard=left) */ 
        addRule("", "\u0594", "", ""); /* zaqef qatan */
        addRule("", "\u0597", "", ""); /* revia */
        addRule("", "\u0598", "", ""); /* tsinnorit */ 
        addRule("", "\u05a1", "", ""); /* pazer */
        addRule("", "\u059f", "", ""); /* qarney para */ 
        addRule("", "\u0595", "", ""); /* zaqef gadol */
        addRule("", "\u059b", "", ""); /* tvir */
        addRule("", "\u0591", "", ""); /* atnach */
        addRule("", "\u05aa", "", ""); /* galgal, atnach hafukh */ 
        addRule("", "\u05a7", "", ""); /* darga */
        addRule("", "\u05bd", "", ""); /* meteg, silluq (right) (compare "75") */
        addRule("", AYIN + PATAH, "$", SUBSCRIPT_A + HOOK_RIGHT);
        addRule("", "\u05e2", "", HOOK_RIGHT); /* ayin */
        Rule aleph1 = addRule(VOWEL + OPTIONAL_CANTILLATION, ALEPH,
            NOT_VOWEL_AFTER, SUPERSCRIPT_QUERY);
        addRule("", ALEPH, "", HOOK_LEFT); /* aleph */
        addRule("", KHET + PATAH, "$", SUBSCRIPT_A + H_DOT);
        addRule("", PATAH, "", "a");
        addRule(VOWEL + OPTIONAL_CANTILLATION, BET + DAGESH, "", "bb");
        addRule(NOT_VOWEL_BEFORE, BET + DAGESH, "", "b");
        addRule("", BET, "", "v");
        addRule("", SHIN + DAGESH, "", S_CARON + S_CARON);
        addRule("", SHIN, "", S_CARON);
        addRule(VOWEL, DALET + DAGESH, "", "dd");
        // addRule(NOT_VOWEL_BEFORE, DALET + DAGESH, "", "d");
        addRule("", DALET, "", "d");
        addRule("", SEGOL + OPTIONAL_CANTILLATION + HE, "$",
            "e" + SUPERSCRIPT_H);
        addRule("", SEGOL + OPTIONAL_CANTILLATION + HE + DAGESH, "$",
            "eh");
        addRule("", SEGOL + OPTIONAL_CANTILLATION + YOD, "",
            "e" + SUPERSCRIPT_Y);
        addRule("", SEGOL, "", "e");
        addRule("", SIN + DAGESH, "", S_ACUTE + S_ACUTE);
        addRule("", SIN, "", S_ACUTE);
        addRule(VOWEL, GIMEL + DAGESH, "", "gg");
        // addRule(NOT_VOWEL_BEFORE, GIMEL + DAGESH, "", "g");
        addRule("", GIMEL, "", "g");
        addRule("", HE, "", "h");
        addRule("", HIRIQ + OPTIONAL_CANTILLATION + YOD, "", I_CARET);
        addRule("", HIRIQ, "", "i");
        addRule("", YOD + DAGESH, "", "yy");
        addRule("", YOD, "", "y");
        addRule(VOWEL + OPTIONAL_CANTILLATION, KAPH + DAGESH, "", "kk");
        addRule(NOT_VOWEL_BEFORE, KAPH + DAGESH, "", "k");
        addRule("", KAPH, "", "x");
        addRule("", FINAL_KAPH, "", "x");
        addRule("", LAMED + DAGESH, "", "ll");
        addRule("", LAMED, "", "l");
        addRule("", MEM + DAGESH, "", "mm");
        addRule("", MEM, "", "m");
        addRule("", FINAL_MEM, "", "m");
        addRule("", NUN + DAGESH, "", "nn");
        addRule("", NUN, "", "n");
        addRule("", FINAL_NUN, "", "n");
        addRule("", HOLAM + OPTIONAL_CANTILLATION + WAW, "",
            O_CARET);
        addRule("", HOLAM + OPTIONAL_CANTILLATION + HE, "$",
            O_BAR + SUPERSCRIPT_H);
        // addRule("", HOLAM + HE + DAGESH, "$", O_BAR + "h");
        addRule("", HOLAM, "", O_BAR);
        addRule(VOWEL + OPTIONAL_CANTILLATION, PE + DAGESH, "", "pp");
        addRule(NOT_VOWEL_BEFORE, PE + DAGESH, "", "p");
        addRule("", PE, "", "f");
        addRule("", FINAL_PE, "", "f");
        addRule("", QOPH + DAGESH, "", "qq");
        addRule("", QOPH, "", "q");
        addRule("", RESH, "", "r");
        addRule("", SAMEK + DAGESH, "", "ss");
        addRule("", SAMEK, "", "s");
        addRule(VOWEL + OPTIONAL_CANTILLATION, TAV + DAGESH, "", "tt");
        addRule("", TAV, "", "t");
        addRule("", QIBBUTS, "", "u");
        addRule("", TET + DAGESH, "", T_DOT + T_DOT);
        addRule("", TET, "", T_DOT);
        addRule("", WAW + DAGESH, "", U_CARET);
        addRule("", WAW, "", "w");
        addRule("", KHET, "", H_DOT);
        addRule("", TSADE + DAGESH, "", S_DOT + S_DOT);
        addRule("", TSADE, "", S_DOT);
        addRule("", FINAL_TSADE, "", S_DOT);
        addRule("", ZAYIN + DAGESH, "", "zz");
        addRule("", ZAYIN, "", "z");
        
        m_trans = new DatabaseTransliterator(m_sql);
        
        addTest("\u05bf", "");
        addTest("\u05bc", "");
        addTest(ALEPH + SHEVA, HOOK_LEFT + SUPERSCRIPT_BACKWARDS_E);
        addTest(ALEPH + DAGESH + SHEVA, HOOK_LEFT + SUPERSCRIPT_BACKWARDS_E);
        addTest(ALEPH + DAGESH + SOF_PASUQ + SHEVA, HOOK_LEFT + ":" + SUPERSCRIPT_BACKWARDS_E);
        addTest(SHEVA + ALEPH, SHEVA + HOOK_LEFT);
        addTest(SHEVA, "");
        addTest(ALEPH + ALEPH + SHEVA, HOOK_LEFT + HOOK_LEFT);
        addTest(SHEVA + ALEPH, SHEVA + HOOK_LEFT);
        addTest(ALEPH + SOF_PASUQ + SHEVA + ALEPH + SOF_PASUQ +
                SHEVA, HOOK_LEFT + ":" + SUPERSCRIPT_BACKWARDS_E + HOOK_LEFT + ":");
        addTest(ALEPH + ALEPH + SOF_PASUQ + SHEVA +
                ALEPH + SOF_PASUQ + SHEVA, HOOK_LEFT + HOOK_LEFT + ":" + HOOK_LEFT + ":");
        addTest(TSERE + ALEPH + SHEVA + ALEPH, E_BAR + SUPERSCRIPT_QUERY + SUPERSCRIPT_BACKWARDS_E + HOOK_LEFT);
        addTest(QAMETS + SOF_PASUQ + ALEPH + SHEVA + ALEPH, A_BAR + ":" + SUPERSCRIPT_QUERY + SUPERSCRIPT_BACKWARDS_E +
            HOOK_LEFT);
        addTest(ALEPH + DAGESH + SHEVA + ALEPH, HOOK_LEFT + SUPERSCRIPT_BACKWARDS_E + HOOK_LEFT);
        
        assertFalse(sheva1.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        assertFalse(sheva2.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        assertFalse(sheva3.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        assertFalse(sheva4.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        assertFalse(sheva5.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        assertFalse(sheva6.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        assertTrue(sheva7.matches(ALEPH + ALEPH, SHEVA + ALEPH));
        addTest(ALEPH + ALEPH + SHEVA + ALEPH, HOOK_LEFT + HOOK_LEFT + HOOK_LEFT);
        
        addTest(HATAPH_QAMETS, SUPERSCRIPT_O);
        addTest(HATAPH_PATAH, SUPERSCRIPT_A);
        addTest(TSERE + HE, E_BAR + SUPERSCRIPT_H);
        addTest(TSERE + HE + ALEPH, E_BAR + "h" + HOOK_LEFT);
        addTest(TSERE + HE + DAGESH, E_BAR + "h");
        addTest(TSERE + HE + DAGESH + ALEPH, E_BAR + "h" + HOOK_LEFT);
        addTest(TSERE + YOD, E_CIRCUMFLEX);
        addTest(ALEPH + TSERE + YOD + ALEPH, HOOK_LEFT + E_CIRCUMFLEX + HOOK_LEFT);
        addTest(TSERE, E_BAR);
        addTest(ALEPH + TSERE + ALEPH, HOOK_LEFT + E_BAR + SUPERSCRIPT_QUERY);
        addTest(QAMETS + ALEPH + DAGESH + SOF_PASUQ + SHEVA,
            "o" + SUPERSCRIPT_QUERY + ":");
        
        assertFalse(sheva1.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        assertFalse(sheva2.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        assertFalse(sheva3.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        assertFalse(sheva4.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        assertFalse(sheva5.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        assertTrue(sheva6.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        assertFalse(sheva7.matches(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ,
            SHEVA + ALEPH));
        addTest(ALEPH + QAMETS + ALEPH + DAGESH + SOF_PASUQ + SHEVA + ALEPH,
            HOOK_LEFT + "o" + SUPERSCRIPT_QUERY + ":" +
            SUPERSCRIPT_BACKWARDS_E + HOOK_LEFT);
        addTest(ALEPH + QAMETS + ALEPH + SOF_PASUQ + SHEVA + ALEPH,
            HOOK_LEFT + "o" + SUPERSCRIPT_QUERY + ":" + HOOK_LEFT);
        
        addTest(QAMETS + ALEPH, "o" + SUPERSCRIPT_QUERY);
        addTest(QAMETS + ALEPH + DAGESH, "o" + SUPERSCRIPT_QUERY);
        addTest(QAMETS + SOF_PASUQ + ALEPH, A_BAR + ":" + SUPERSCRIPT_QUERY);
        addTest(QAMETS + ALEPH + SOF_PASUQ, A_BAR + SUPERSCRIPT_QUERY + ":");
        addTest(QAMETS + ALEPH + ALEPH, A_BAR + SUPERSCRIPT_QUERY + HOOK_LEFT);
        addTest(QAMETS + HE, A_BAR + SUPERSCRIPT_H);
        addTest(QAMETS + HE + HE, A_BAR + "hh");
        addTest(QAMETS + HE + DAGESH, A_BAR + "h");
        addTest(QAMETS + HE + DAGESH + HE, A_BAR + "hh");
        addTest(QAMETS + YOD, A_BAR + SUPERSCRIPT_Y);
        addTest(QAMETS + YOD + DAGESH + HE, A_BAR + "yyh");
        addTest(QAMETS, A_BAR);
        addTest("\u05c6", "");
        addTest(SOF_PASUQ, ":");
        addTest("\u0592", "");
        addTest("\u0599", "");
        addTest("\u05a9", ""); /* tlisha qtana  */
        addTest("\u05c0", ""); /* legarmeh, paseq  */
        addTest("\u059a", ""); /* yetiv* */
        addTest("\u059d", ""); /* geresh muqdam  */
        addTest("\u05ad", ""); /* dechi  */
        addTest("\u05a0", ""); /* tlisha gdola* */
        addTest("\u05a8", ""); /* pashta */
        addTest("\u05c4", ""); /* Puncta Extraordinaria above */
        addTest("\u0323", ""); /* Puncta Extraordinaria below */
        addTest("\u05ab", ""); /* ole  */
        addTest("\u059c", ""); /* geresh  */
        addTest("\u059e", ""); /* gershayim  */
        addTest("\u05a8", ""); /* qadma, azla (compare "33") */
        addTest("\u05ac", ""); /* illuy  */
        addTest("\u0593", ""); /* shalshelet  */
        addTest("\u05a4", ""); /* mahpakh  */
        addTest("\u05a5", ""); /* merkha, yored  */
        addTest("\u05a6", ""); /* merkha khfula  */
        addTest("\u0596", ""); /* tipcha, mëayla, tarcha  */
        addTest("\u05a3", ""); /* munnach  */
        addTest("\u05bd", ""); /* meteg, silluq (standard=left)  */
        addTest("\u0594", ""); /* zaqef qatan  */
        addTest("\u0597", ""); /* revia  */
        addTest("\u0598", ""); /* tsinnorit  */
        addTest("\u05a1", ""); /* pazer  */
        addTest("\u059f", ""); /* qarney para  */
        addTest("\u0595", ""); /* zaqef gadol  */
        addTest("\u059b", ""); /* tvir  */
        addTest("\u0591", ""); /* atnach  */
        addTest("\u05aa", ""); /* galgal, atnach hafukh  */
        addTest("\u05a7", ""); /* darga  */
        addTest("\u05bd", ""); /* meteg, silluq (right) (compare "75") */
        addTest("\u05e2", "\u0295"); /* ayin */
        addTest(PATAH + ALEPH + ALEPH, "a" + SUPERSCRIPT_QUERY + HOOK_LEFT);
        addTest(PATAH + ALEPH + WAW, "a" + SUPERSCRIPT_QUERY + "w");
        addTest(PATAH + ALEPH + WAW + WAW, "a" + SUPERSCRIPT_QUERY + "ww");
        
        assertTrue(PATAH.matches(VOWEL));
        assertFalse(PATAH.matches(NOT_VOWEL_AFTER));
        assertTrue((WAW + DAGESH).matches(VOWEL));
        assertFalse((WAW + DAGESH).matches(NOT_VOWEL_AFTER));
        assertFalse(aleph1.matches(PATAH, ALEPH + WAW + DAGESH));
        addTest(PATAH + ALEPH + WAW + DAGESH, "a" + HOOK_LEFT + U_CARET);
        addTest(PATAH + ALEPH, "a" + SUPERSCRIPT_QUERY);
        addTest(PATAH + ALEPH + TSERE, "a" + HOOK_LEFT + E_BAR);
        
        addTest(AYIN + PATAH, SUBSCRIPT_A + HOOK_RIGHT);
        addTest(AYIN + PATAH + ALEPH, HOOK_RIGHT + "a" + SUPERSCRIPT_QUERY);
        addTest(KHET + PATAH, SUBSCRIPT_A + H_DOT);
        addTest(KHET + PATAH + ALEPH, H_DOT + "a" + SUPERSCRIPT_QUERY);
        
        addTest(PATAH + SOF_PASUQ + BET + DAGESH, "a:bb");
        addTest(ALEPH + SOF_PASUQ + BET + DAGESH, HOOK_LEFT + ":b");
        addTest(ALEPH + BET, HOOK_LEFT + "v");
        
        addTest(SHIN + DAGESH, S_CARON + S_CARON);
        addTest(SHIN + ALEPH, S_CARON + HOOK_LEFT);
        
        addTest(PATAH + DALET + DAGESH, "add");
        addTest(ALEPH + DALET + DAGESH, HOOK_LEFT + "d");
        addTest(DALET, "d");

        addTest(SEGOL + HE, "e" + SUPERSCRIPT_H);
        addTest(SEGOL + HE + DAGESH, "eh");
        addTest(SEGOL + YOD + ALEPH, "e" + SUPERSCRIPT_Y + HOOK_LEFT);
        addTest(SEGOL + ALEPH, "e" + SUPERSCRIPT_QUERY);

        addTest(SIN + DAGESH, S_ACUTE + S_ACUTE);
        addTest(SIN, S_ACUTE);
        addTest(PATAH + GIMEL + DAGESH, "agg");
        addTest(ALEPH + GIMEL + DAGESH, HOOK_LEFT + "g");
        addTest(GIMEL, "g");
        addTest(ALEPH + GIMEL, HOOK_LEFT + "g");
        addTest(HE, "h");
        addTest(HIRIQ + YOD, I_CARET);
        addTest(HIRIQ, "i");
        addTest(YOD + DAGESH, "yy");
        addTest(YOD, "y");
        addTest(PATAH + SOF_PASUQ + KAPH + DAGESH, "a:kk");
        addTest(ALEPH + KAPH + DAGESH, HOOK_LEFT + "k");
        addTest(KAPH + DAGESH, "k");
        addTest(ALEPH + KAPH, HOOK_LEFT + "x");
        addTest(ALEPH + FINAL_KAPH, HOOK_LEFT + "x");
        addTest(LAMED + DAGESH, "ll");
        addTest(LAMED, "l");
        addTest(MEM + DAGESH, "mm");
        addTest(MEM, "m");
        addTest(FINAL_MEM, "m");
        addTest(NUN + DAGESH, "nn");
        addTest(NUN, "n");
        addTest(FINAL_NUN, "n");
        addTest(HOLAM + WAW, O_CARET);
        addTest(HOLAM + HE, O_BAR + SUPERSCRIPT_H);
        addTest(HOLAM + HE + ALEPH, O_BAR + "h" + HOOK_LEFT);
        addTest(HOLAM + HE + DAGESH, O_BAR + "h");
        addTest(HOLAM + HE + DAGESH + ALEPH, O_BAR + "h" + HOOK_LEFT);
        addTest(HOLAM, O_BAR);
        addTest(PATAH + SOF_PASUQ + PE + DAGESH, "a:pp");
        addTest(ALEPH + SOF_PASUQ + PE + DAGESH, HOOK_LEFT + ":p");
        addTest(ALEPH + PE, HOOK_LEFT + "f");
        addTest(ALEPH + FINAL_PE, HOOK_LEFT + "f");
        addTest(QOPH + DAGESH, "qq");
        addTest(QOPH, "q");
        addTest(RESH, "r");
        addTest(SAMEK + DAGESH, "ss");
        addTest(SAMEK, "s");
        addTest(PATAH + SOF_PASUQ + TAV + DAGESH, "a:tt");
        addTest(ALEPH + SOF_PASUQ + TAV + DAGESH, HOOK_LEFT + ":t");
        addTest(TAV, "t");
        addTest(QIBBUTS, "u");
        addTest(TET + DAGESH, T_DOT + T_DOT);
        addTest(TET, T_DOT);
        addTest(WAW + DAGESH, U_CARET);
        addTest(WAW, "w");
        addTest(KHET, H_DOT);
        addTest(TSADE + DAGESH, S_DOT + S_DOT);
        addTest(TSADE, S_DOT);
        addTest(FINAL_TSADE, S_DOT);
        addTest(ZAYIN + DAGESH, "zz");
        addTest(ZAYIN, "z");
        
        addTest("מִצַּלְעֹתָ֔יו", "miṣṣalʕōtāʸw"); // GEN 2,21
        addTest("הָיְתָ֥ה", "hoytāʰ"); // GEN 1,2 // wrong! special case
        // addTest("הָיְתָ֥ה", "hāyᵊtāʰ"); // GEN 1,2 special case 
        addTest("בָּרָ֣א", "bārāˀ"); // GEN 1,1
        addTest("שָּׁמַ֖יִם", "ššāmayim"); // GEN 1,1
        addTest("אֲדָמָ֖ה", "ʔᵃdāmāʰ"); // GEN 1,25
        addTest("ר֣וּחַ", "rûₐḥ"); // GEN 1,2
        addTest("רָקִ֖יעַ", "rāqîₐʕ"); // GEN 1,6
        addTest("פְּנֵ֣י", "pᵊnê"); // GEN 1,2
        addTest("מִקְוֵ֥ה", "miqwēʰ"); // GEN 1,10
        addTest("רֵאשִׁ֖ית", "rēˀšît"); // GEN 1,1
        addTest("חַיֶּֽיךָ", "ḥayyeʸxā"); // GEN 3,14
        addTest("עֹ֤שֶׂה", "ʕōśeʰ"); // GEN 1,11
        addTest("אָֽרֶץ", "ʔāreṣ"); // GEN 1,1
        addTest("אֱלֹהִ֔ים", "ʔᵉlōhîm"); // GEN 1,2
        addTest("תְהֹ֑ום", "tᵊhôm"); // GEN 1,2
        addTest("אָכְלָֽה", "ʔoxlāʰ"); // GEN 1,29
        addTest("לֻֽקֳחָה־", "luqᵒḥāh-"); // GEN 2,23
        addTest("כִבְשֻׁ֑הָ", "xivšuhā"); // GEN 1,28
        addTest("בְּ", "bᵊ"); // GEN 1,1
        
        List<Morpheme> morphs = new ArrayList<Morpheme>();
        morphs.add(new Morpheme("צַּלְע", "rib", "N/NUC"));
        morphs.add(new Morpheme("ֹתָ֔י", "FplCS", "N/GNS"));
        morphs.add(new Morpheme("ו", "3Msg", "N/POS"));
        assertEquals("ṣṣalʕ", m_trans.transliterate(morphs, 0));
        assertEquals("ōtāʸ", m_trans.transliterate(morphs, 1));
        assertEquals("w", m_trans.transliterate(morphs, 2));
        
        m_sql.commitTransaction();
    }
    
    public void addTest(String input, String output)
    {
        assertEquals(output, m_trans.transliterate(input, "", ""));
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(TransliteratorTest.class);
    }

}
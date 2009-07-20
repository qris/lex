package com.qwirx.lex.hebrew;

import java.util.List;

import jemdros.MatchedObject;

import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;
import com.qwirx.lex.translit.DatabaseTransliterator;


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
    
    public static String wordToHtml(MatchedObject word, EmdrosDatabase emdros,
        DatabaseTransliterator transliterator)
    throws Exception
    {
        return wordTranslitToHtml(word, new HebrewMorphemeGenerator(),
            transliterator);
    }

    public static String wordTranslitToHtml(MatchedObject word, 
        HebrewMorphemeGenerator generator,
        DatabaseTransliterator transliterator)
    throws Exception
    {
        StringBuffer out = new StringBuffer();
        List<Morpheme> morphemes = generator.parse(word, false, (String)null,
            transliterator);
        
        for (int i = 0; i < morphemes.size(); i++)
        {
            out.append(morphemes.get(i).getTranslit());
        }
        return toHtml(out.toString());
    }

    public static String wordHebrewToHtml(MatchedObject word, 
        HebrewMorphemeGenerator generator)
    throws Exception
    {
        StringBuffer out = new StringBuffer();
        List<Morpheme> morphemes = generator.parse(word, false, (String)null,
            null);

        for (int i = 0; i < morphemes.size(); i++)
        {
            Morpheme morpheme = morphemes.get(i);
            out.append(morpheme.getSurface());
        }
        
        return toHtml(out.toString());
    }
}

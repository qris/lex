package com.qwirx.lex;

import java.io.IOException;

import jemdros.MatchedObject;
import jemdros.SheafConstIterator;

import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookException;

import com.qwirx.crosswire.kjv.KJV;

public class GemExporter
{
    public static String export(MatchedObject object, BookData verse)
    throws IOException, BookException
    {
        StringBuffer buf = new StringBuffer();
        buf.append("\\wordfield word\n" +
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
            "\n");
        
        exportObject(object, verse, buf);
        
        return buf.toString();
    }
    
    private static void exportObject(MatchedObject object, BookData verse,
        StringBuffer buf)
    throws IOException, BookException
    {
        if (object.getObjectTypeName().equals("word"))
        {
            buf.append("\\word ")
                .append(object.getEMdFValue("text").getString()).append("\n");
            buf.append("\\trans ").append(
                HebrewConverter.toTranslit(
                    object.getEMdFValue("graphical_word").getString()))
                .append("\n");
            buf.append("\\tag tag\n");
            buf.append("\\gloss ");
            String gloss = KJV.getStrongGloss(verse, 
                object.getEMdFValue("lexeme").getString());
            if (gloss != null) buf.append(gloss);
            buf.append("\n");
            buf.append("\\lemma lemma\n");
            buf.append("\\re\n\n");
        }
        
        if (!object.sheafIsEmpty())
        {
            SheafConstIterator straws = object.getSheaf().const_iterator();
            while (straws.hasNext())
            {
                MatchedObject child = straws.next().const_iterator().next();
                exportObject(child, verse, buf);
            }
        }
    }
}

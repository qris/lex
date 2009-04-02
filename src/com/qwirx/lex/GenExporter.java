package com.qwirx.lex;

import java.util.List;

import jemdros.MatchedObject;
import jemdros.SheafConstIterator;

import org.crosswire.jsword.book.BookData;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class GenExporter
{
    public String export(MatchedObject object, BookData verse,
        SqlDatabase sql, DatabaseTransliterator transliterator,
        boolean includeHebrew)
    throws Exception
    {
        StringBuffer buf = new StringBuffer();
        buf.append("\\wordfield morpheme\n" +
            "\\glossfield gloss\n" +
            "\\tagfield tag\n" +
            (includeHebrew ? "\\transliterationfield trans\n" : "") +
            "\\lemmafield lemma\n" +
            "\\recordend re\n" +
            "\\foreignfont SILIPAManuscriptL\n" +
            "\\glossfont Times New Roman\n" +
            "\\transliterationfont SILIPADoulosL\n" +
            (includeHebrew ? "\\righttoleft\n" : "\\lefttoright\n") +
            "\\wordfieldisUTF8\n" +
            "\\transliterationfieldisUTF8\n" +
            "\n");
        
        exportObject(object, verse, buf, sql, transliterator,
            includeHebrew);
        
        return buf.toString();
    }
    
    private void exportObject(MatchedObject object, BookData verse,
        StringBuffer buf, SqlDatabase sql,
        DatabaseTransliterator transliterator, boolean includeHebrew)
    throws Exception
    {
        if (object.getObjectTypeName().equals("word"))
        {
            List<Morpheme> morphemes = new HebrewMorphemeGenerator().parse(object,
                true, sql);
            
            for (int i = 0; i < morphemes.size(); i++)
            {
                Morpheme morpheme = morphemes.get(i);
                String hebrew = morpheme.getSurface();

                String translit = transliterator.transliterate(morphemes, i);
                if (translit.equals("")) translit = "Ø";
                if (i < morphemes.size() - 1) translit += "-";
                
                if (includeHebrew)
                {
                    buf.append("\\morpheme ").append(hebrew).append("\n");
                    buf.append("\\trans ").append(translit).append("\n");
                }
                else
                {
                    buf.append("\\morpheme ").append(translit).append("\n");
                }

                buf.append("\\tag tag\n");
                buf.append("\\gloss ").append(morpheme.getGloss()).append("\n");
                buf.append("\\lemma lemma\n");
                buf.append("\\re\n\n");
            }
        }
        
        if (!object.sheafIsEmpty())
        {
            SheafConstIterator straws = object.getSheaf().const_iterator();
            while (straws.hasNext())
            {
                MatchedObject child = straws.next().const_iterator().next();
                exportObject(child, verse, buf, sql, transliterator,
                    includeHebrew);
            }
        }
    }
}

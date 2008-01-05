package com.qwirx.lex;

import java.io.IOException;
import java.sql.SQLException;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.SheafConstIterator;

import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookException;
import org.xml.sax.SAXException;

import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.MorphemeHandler;

public class GenExporter
{
    public String export(MatchedObject object, BookData verse, SqlDatabase sql)
    throws IOException, BookException, SAXException, 
        DatabaseException, SQLException, EmdrosException
    {
        StringBuffer buf = new StringBuffer();
        buf.append("\\wordfield morpheme\n" +
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
        
        exportObject(object, verse, buf, sql);
        
        return buf.toString();
    }
    
    class HebrewFeatureConverter implements MorphemeHandler
    {
        private MatchedObject m_Word;
        private StringBuffer  m_Output;
        
        public HebrewFeatureConverter(MatchedObject word, StringBuffer output)
        {
            m_Word   = word;
            m_Output = output;
        }
        
        public void convert(String surface, 
            boolean lastMorpheme, String desc,
            String morphNode)
        throws EmdrosException
        {
            String raw = m_Word.getEMdFValue(surface).getString();

            String hebrew = HebrewConverter.toHebrew(raw);
            m_Output.append("\\morpheme ").append(hebrew).append("\n");

            String translit = HebrewConverter.toTranslit(raw);
            if (translit.equals("")) translit = "Ø";
            if (!lastMorpheme) translit += "-";
            m_Output.append("\\trans ").append(translit).append("\n");

            m_Output.append("\\tag tag\n");
            m_Output.append("\\gloss ").append(desc).append("\n");
            m_Output.append("\\lemma lemma\n");
            m_Output.append("\\re\n\n");
        }
    }

    private void exportObject(MatchedObject object, BookData verse,
        StringBuffer buf, SqlDatabase sql)
    throws IOException, BookException, SAXException,
        DatabaseException, SQLException, EmdrosException
    {
        if (object.getObjectTypeName().equals("word"))
        {
            HebrewFeatureConverter hfc = 
                new HebrewFeatureConverter(object, buf);
            
            new HebrewMorphemeGenerator().parse(object, hfc, true, sql);
        }
        
        if (!object.sheafIsEmpty())
        {
            SheafConstIterator straws = object.getSheaf().const_iterator();
            while (straws.hasNext())
            {
                MatchedObject child = straws.next().const_iterator().next();
                exportObject(child, verse, buf, sql);
            }
        }
    }
}

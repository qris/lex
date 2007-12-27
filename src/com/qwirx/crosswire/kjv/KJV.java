package com.qwirx.crosswire.kjv;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jemdros.Table;
import jemdros.TableException;
import jemdros.TableIterator;
import jemdros.TableRow;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.crosswire.jsword.passage.Verse;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewEnglishDatabase;

public class KJV
{
    public static Book get()
    {
        Books books = Books.installed();
        Book bible = books.getBook("KJV");
        return bible;
    }
    
    private static Map<String, Integer> s_BookNameToNumMap = null;
    
    public static BookData getVerse(EmdrosDatabase emdros, 
        String bookName, int chapter, int verse) 
    throws NoSuchKeyException, TableException, DatabaseException
    {
        if (s_BookNameToNumMap == null)
        {
            s_BookNameToNumMap = new HashMap<String, Integer>();
        
            Table t = emdros.getTable("SELECT ENUM CONSTANTS FROM book_name_e");
            
            for (TableIterator ti = t.iterator(); ti.hasNext();)
            {
                TableRow tr = ti.next();
                String enumBookName = tr.getColumn(1);
                Integer enumBookNum = Integer.valueOf(tr.getColumn(2));
                s_BookNameToNumMap.put(enumBookName, enumBookNum);
            }
        }
        
        Integer bookNum = s_BookNameToNumMap.get(bookName);
        
        if (bookNum == null)
        {
            throw new IllegalArgumentException("No such book named " + 
                bookName);
        }
        
        Book bible = get();
        Key key = new Verse(bookNum, chapter, verse);
        return new BookData(bible, key);
    }
    
    public static String getStrongGloss(EmdrosDatabase emdros, 
        String bookName, int chapterNum, int verseNum, int strongsNum)
    throws NoSuchKeyException, TableException, DatabaseException, BookException
    {
        BookData data = getVerse(emdros, bookName, chapterNum, verseNum);
        return getStrongGloss(data, strongsNum);
    }
    
    public static String getStrongGloss(BookData data, int strongsNum)
    throws BookException
    {
        Element div = data.getOsisFragment();
        assert div.getName().equals("div");
        
        List contents = div.getContent();
        assert contents.size() == 1;
        
        Element verse = (Element)contents.get(0);
        assert verse.getName().equals("verse");
        
        contents = verse.getContent();
        for (Iterator i = contents.iterator(); i.hasNext();)
        {
            Content content = (Content)i.next();
            if (content instanceof Text) continue;
            
            Element words = (Element)content;
            assert words.getName().equals("w");
            
            String lemma = words.getAttributeValue("lemma");
            if (lemma == null) continue;
            
            if (lemma.equals("strong:H0" + strongsNum))
            {
                return words.getText();
            }
        }
        
        return null;
    }
    
    public static String getStrongGloss(BookData verse, String amsterdam)
    throws Exception
    {
        amsterdam = amsterdam.replaceAll("[\\[/]$", "");
        
        List<HebrewEnglishDatabase.Entry> matches = 
            HebrewEnglishDatabase.getInstance().getMatches(amsterdam);

        if (matches == null)
        {
            return null;
        }
        
        for (Iterator i = matches.iterator(); i.hasNext();)
        {
            HebrewEnglishDatabase.Entry entry = 
                (HebrewEnglishDatabase.Entry)i.next();
            String gloss = getStrongGloss(verse, entry.getStrongsNum());
            if (gloss != null)
            {
                return gloss;
            }
        }
        
        return "(no exact matches)";
    }

}

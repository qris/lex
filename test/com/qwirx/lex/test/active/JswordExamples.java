/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 as published by
 * the Free Software Foundation. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * Copyright: 2005
 *     The copyright to this program is held by it's authors.
 *
 * ID: $Id: APIExamples.java 1466 2007-07-02 02:48:09Z dmsmith $
 */
package com.qwirx.lex.test.active;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.TransformerException;

import jemdros.Table;
import jemdros.TableIterator;
import jemdros.TableRow;
import junit.framework.TestCase;

import org.crosswire.common.util.NetUtil;
import org.crosswire.common.util.ResourceUtil;
import org.crosswire.common.xml.Converter;
import org.crosswire.common.xml.SAXEventProvider;
import org.crosswire.common.xml.TransformingSAXEventProvider;
import org.crosswire.common.xml.XMLUtil;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.BookFilter;
import org.crosswire.jsword.book.BookFilters;
import org.crosswire.jsword.book.BookMetaData;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.BooksEvent;
import org.crosswire.jsword.book.BooksListener;
import org.crosswire.jsword.book.OSISUtil;
import org.crosswire.jsword.book.sword.SwordBookPath;
import org.crosswire.jsword.index.search.DefaultSearchModifier;
import org.crosswire.jsword.index.search.DefaultSearchRequest;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.crosswire.jsword.passage.Passage;
import org.crosswire.jsword.passage.PassageTally;
import org.crosswire.jsword.passage.RestrictionType;
import org.crosswire.jsword.passage.Verse;
import org.crosswire.jsword.util.ConverterFactory;
import org.crosswire.jsword.versification.BibleInfo;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;
import org.xml.sax.SAXException;

import com.qwirx.crosswire.kjv.KJV;
import com.qwirx.lex.Lex;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewEnglishDatabase;

/**
 * All the methods in this class highlight some are of the API and how to use it.
 *
 * @see gnu.lgpl.License for license details.
 *      The copyright to this program is held by it's authors.
 * @author Joe Walker [joe at eireneh dot com]
 */
public class JswordExamples extends TestCase
{
    /**
     * The name of a Bible to find
     */
    private static final String BIBLE_NAME = "KJV"; //$NON-NLS-1$
    private EmdrosDatabase m_Emdros; 
    
    public void setUp() throws Exception
    {
        URL url = getClass().getResource("/com/qwirx/crosswire/kjv");
        assertNotNull(url);
        File[] files = new File[1];
        files[0] = new File(url.getPath());
        SwordBookPath.setAugmentPath(files);
        
        m_Emdros = Lex.getEmdrosDatabase("test", "localhost", 
            Lex.getSqlDatabase("test"));
    }
    
    public void tearDown()
    {
        Lex.putEmdrosDatabase(m_Emdros);
    }
    
    public String dump(Element element)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<");
        buf.append(element.getName());
        
        List attribs = element.getAttributes();
        for (Iterator i = attribs.iterator(); i.hasNext();)
        {
            Attribute attr = (Attribute)i.next();
            buf.append(" ");
            buf.append(attr.getName());
            buf.append("=\"");
            buf.append(attr.getValue());
            buf.append("\"");
        }
        
        List contents = element.getContent();
        if (contents.size() > 0)
        {
            buf.append(">");
            for (Iterator i = contents.iterator(); i.hasNext();)
            {
                Content child = (Content)i.next();
                if (child instanceof Text)
                {
                    // buf.append("[" + ((Text)child).getText() + "]");
                    buf.append(((Text)child).getText());
                }
                else if (child instanceof Element)
                {
                    buf.append(dump((Element)child));
                }
                else
                {
                    fail("Don't know how to handle contents of type " + 
                        child);
                }
            }
            buf.append("</" + element.getName() + ">");
        }
        else   
        {
            buf.append(" />");
        }
        
        return buf.toString();
    }
    
    public void testFindWordByStrongsNum() throws Exception
    {
        BookData data = KJV.getVerse(m_Emdros, "Genesis", 1, 1);
        Element e = data.getOsisFragment();
        assertEquals("<div>" +
                "<verse osisID=\"Gen.1.1\">" +
                "<w lemma=\"strong:H07225\">In the beginning</w> " +
                "<w lemma=\"strong:H0430\">God</w> " +
                "<w lemma=\"strong:H0853 strong:H01254\"" +
                " morph=\"strongMorph:TH8804\">created</w> " +
                "<w lemma=\"strong:H08064\">the heaven</w> " +
                "<w lemma=\"strong:H0853\">and</w> " +
                "<w lemma=\"strong:H0776\">the earth</w>." +
                "</verse></div>", dump(e));
    }
    
    public void testLookupGlossFromSurfaceConsonants() throws Exception
    {
        Table table = m_Emdros.getTable("GET FEATURES lexeme_wit " +
            "FROM OBJECT WITH ID_D = 413449 [word]"); // RICHT06,11(e)
        
        String surface = null;
        for (TableIterator ti = table.iterator(); ti.hasNext();)
        {
            TableRow tr = ti.next();
            assertEquals("413449", tr.getColumn(1));
            surface = tr.getColumn(2);
        }
        
        assertEquals("XVH/", surface);
        surface = surface.replaceAll("[\\[/]$", "");
        assertEquals("XVH", surface);
        
        List<HebrewEnglishDatabase.Entry> matches = 
            HebrewEnglishDatabase.getInstance().getMatches(surface);
        assertNotNull(matches);
        assertEquals(1, matches.size());
        
        HebrewEnglishDatabase.Entry entry = matches.get(0);
        assertEquals(surface, entry.getAmsterdamString());
        assertEquals(2406, entry.getStrongsNum());
        
        BookData verse = KJV.getVerse(m_Emdros, "Judges", 6, 11);
        assertNotNull(verse);
        
        String gloss = KJV.getStrongGloss(verse, entry.getStrongsNum());
        assertEquals("wheat", gloss);
    }
    
    public void testKingJamesLookup() throws Exception
    {   
        BookData verse = KJV.getVerse(m_Emdros, "Judges", 6, 11);
        assertNotNull(verse);
        
        assertNull(KJV.getKingJamesGloss(verse, "W"));
        assertEquals("Gideon", KJV.getKingJamesGloss(verse, "GD<WN/"));
        assertEquals("and his son", KJV.getKingJamesGloss(verse, "BN/"));
        assertEquals("threshed", KJV.getKingJamesGloss(verse, "XBV["));
        assertEquals("wheat", KJV.getKingJamesGloss(verse, "XVH/"));
        assertNull(KJV.getKingJamesGloss(verse, "B"));
        assertNull(KJV.getKingJamesGloss(verse, "H"));
        assertEquals("by the winepress", KJV.getKingJamesGloss(verse, "GT/"));
    }

    public void testDibLookup() throws Exception
    {   
        assertEquals(null, KJV.getDibGloss("W"));
        assertEquals("Gidon", KJV.getDibGloss("GD<WN/"));
        assertEquals("a son; Ben", KJV.getDibGloss("BN/"));
        assertEquals("to knock out, off", KJV.getDibGloss("XBV["));
        assertEquals("wheat", KJV.getDibGloss("XVH/"));
        assertEquals(null, KJV.getDibGloss("B"));
        assertEquals(null, KJV.getDibGloss("H"));
        assertEquals("a wine-press; Gath", KJV.getDibGloss("GT/"));
    }

    /**
     * The source to this method is an example of how to read the plain text of
     * a verse, and print it to stdout. Reading from a Commentary is just the
     * same as reading from a Bible.
     * @see Book
     */
    public void testReadPlainText() throws BookException, NoSuchKeyException
    {
        Books books = Books.installed();
        Book bible = books.getBook(BIBLE_NAME);

        Key key = bible.getKey("Gen 1 1"); //$NON-NLS-1$
        BookData data = new BookData(bible, key);
        String text = OSISUtil.getCanonicalText(data.getOsisFragment());

        assertEquals("In the beginning God created the heaven and the earth.",
            text);
    }

    /**
     * This method demonstrates how to get styled text (in this case HTML) from
     * a verse, and print it to stdout. Reading from a Commentary is just the
     * same as reading from a Bible.
     * @see Book
     * @see SAXEventProvider
     */
    public void testReadStyledText() 
    throws NoSuchKeyException, BookException, TransformerException, SAXException
    {
        Book bible = Books.installed().getBook(BIBLE_NAME);

        Key key = bible.getKey("Gen 1 1"); //$NON-NLS-1$
        BookData data = new BookData(bible, key);
        SAXEventProvider osissep = data.getSAXEventProvider();

        Converter styler = ConverterFactory.getConverter();

        TransformingSAXEventProvider htmlsep = 
            (TransformingSAXEventProvider) styler.convert(osissep);

        // You can also pass parameters to the xslt. What you pass 
        // depends upon what the xslt can use.
        BookMetaData bmd = bible.getBookMetaData();
        boolean direction = bmd.isLeftToRight();
        htmlsep.setParameter("direction", direction ? "ltr" : "rtl");

        // Finally you can get the styled text.
        String text = XMLUtil.writeToString(htmlsep);

        assertEquals("", text); 
    }

    /**
     * While Bible and Commentary are very similar, a Dictionary is read in a
     * slightly different way. It is also worth looking at the JavaDoc for
     * Book that has a way of treating Bible, Commentary and Dictionary the same.
     * @see Book
     */
    public void testReadDictionary() throws BookException
    {
        // This just gets a list of all the known dictionaries and picks the
        // first. In a real world app you will probably have a better way
        // of doing this.
        List dicts = Books.installed().getBooks(BookFilters.getDictionaries());
        Book dict = (Book) dicts.get(0);

        // If I want every key in the Dictionary then I do this (or something
        // like it - in the real world you want to call hasNext() on an iterator
        // before next() but the point is the same:
        Key keys = dict.getGlobalKeyList();
        Key first = (Key) keys.iterator().next();

        assertEquals("", first); //$NON-NLS-1$

        BookData data = new BookData(dict, keys);
        assertEquals("", OSISUtil.getPlainText(data.getOsisFragment()));
    }

    /**
     * An example of how to search for various bits of data.
     */
    public void testSearch() throws BookException
    {
        Book bible = Books.installed().getBook(BIBLE_NAME);

        // This does a standard operator search. See the search documentation
        // for more examples of how to search
        Key key = bible.find("+moses +aaron"); //$NON-NLS-1$

        assertEquals("Gen 1-1", key.getName()); //$NON-NLS-1$

        // You can also trim the result to a more managable quantity.
        // The test here is not necessary since we are working with a bible. 
        // It is necessary if we don't know what it is.
        if (key instanceof Passage)
        {
            Passage remaining = ((Passage) key).trimVerses(5);
            assertEquals("", key.getName()); //$NON-NLS-1$
            assertEquals("", remaining.getName()); //$NON-NLS-1$
        }
    }

    /**
     * An example of how to perform a ranked search.
     * @throws BookException
     */
    public void testRankedSearch() throws BookException
    {
        Book bible = Books.installed().getBook(BIBLE_NAME);

        // For a more complex example:
        // Rank the verses and show the first 20
        boolean rank = true;

        DefaultSearchModifier modifier = new DefaultSearchModifier();
        modifier.setRanked(rank);

        Key results = bible.find(new DefaultSearchRequest("for god so loved " +
                "the world", modifier)); //$NON-NLS-1$
        int total = results.getCardinality();
        int partial = total;

        // we get PassageTallys for rank searches
        if (results instanceof PassageTally || rank)
        {
            PassageTally tally = (PassageTally) results;
            tally.setOrdering(PassageTally.ORDER_TALLY);
            int rankCount = 20;
            if (rankCount > 0 && rankCount < total)
            {
                // Here we are trimming by ranges, where a range is a 
                // set of continuous verses.
                tally.trimRanges(rankCount, RestrictionType.NONE);
                partial = rankCount;
            }
        }
        assertEquals(1, total);
        assertEquals(1, partial);
        assertEquals("", results);
    }

    /**
     * An example of how to do a search and then get text for each range of verses.
     * @throws BookException
     * @throws SAXException
     */
    public void testSearchAndShow() throws BookException, SAXException
    {
        Book bible = Books.installed().getBook(BIBLE_NAME);

        // Search for words like Melchezedik
        Key key = bible.find("melchesidec~"); //$NON-NLS-1$

        // Here is an example of how to iterate over the ranges and get the text for each
        // The key's iterator would have iterated over verses.

        // The following shows how to use a stylesheet of your own choosing
        String path = "xsl/cswing/simple.xsl"; //$NON-NLS-1$
        URL xslurl = ResourceUtil.getResource(path);

        Iterator rangeIter = ((Passage) key).rangeIterator(RestrictionType.CHAPTER); // Make ranges break on chapter boundaries.
        while (rangeIter.hasNext())
        {
            Key range = (Key) rangeIter.next();
            BookData data = new BookData(bible, range);
            SAXEventProvider osissep = data.getSAXEventProvider();
            SAXEventProvider htmlsep = new TransformingSAXEventProvider(NetUtil.toURI(xslurl), osissep);
            String text = XMLUtil.writeToString(htmlsep);
            System.out.println("The html text of " + range.getName() + " is " + text); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * An example of how to get the text of a book for export.
     *
     * @throws NoSuchKeyException
     * @throws BookException
     */
    public void export() throws NoSuchKeyException, BookException
    {
        Book bible = Books.installed().getBook(BIBLE_NAME);
        Key keys = bible.getKey("Gen"); //$NON-NLS-1$
        // Get a verse iterator
        Iterator iter = keys.iterator();
        while (iter.hasNext())
        {
            Verse verse = (Verse) iter.next();
            BookData data = new BookData(bible, verse);
            System.out.println('|' + BibleInfo.getPreferredBookName(verse.getBook()) + '|' + verse.getChapter() + '|' + verse.getVerse() + '|' + OSISUtil.getCanonicalText(data.getOsisFragment()));
        }
    }

    /**
     * This is an example of the different ways to select a Book from the
     * selection available.
     * @see org.crosswire.common.config.Config
     * @see Books
     */
    public void testPickBible()
    {
        // The Default Bible - JSword does everything it can to make this work
        Book book = Books.installed().getBook(BIBLE_NAME);

        // And you can find out more too:
        System.out.println(book.getLanguage());

        // If you want a greater selection of Books:
        List books = Books.installed().getBooks();
        book = (Book) books.get(0);

        // Or you can narrow the range a bit
        books = Books.installed().getBooks(BookFilters.getOnlyBibles());
        book = (Book) books.get(0);

        // There are implementations of BookFilter for all sorts of things in
        // the BookFilters class

        // If you are wanting to get really fancy you can implement your own
        // BookFilter easily
        List test = Books.installed().getBooks(new MyBookFilter());
        book = (Book) test.get(0);

        if (book != null)
        {
            System.out.println(book.getInitials());
        }


        // If you want to know about new books as they arrive:
        Books.installed().addBooksListener(new MyBooksListener());
    }

    /**
     * A simple BookFilter that looks for a Bible by name.
     */
    static class MyBookFilter implements BookFilter
    {
        public boolean test(Book bk)
        {
            return bk.getName().equals("My Favorite Version"); //$NON-NLS-1$
        }
    }

    /**
     * A simple BooksListener that actually does nothing.
     */
    static class MyBooksListener implements BooksListener
    {
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BooksListener#bookAdded(org.crosswire.jsword.book.BooksEvent)
         */
        public void bookAdded(BooksEvent ev)
        {
        }

        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BooksListener#bookRemoved(org.crosswire.jsword.book.BooksEvent)
         */
        public void bookRemoved(BooksEvent ev)
        {
        }
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(JswordExamples.class);
    }
}

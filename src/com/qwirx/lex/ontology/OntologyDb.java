/**
 * 
 */
package com.qwirx.lex.ontology;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class OntologyDb extends DefaultHandler
{
	private OntologyDb() throws SAXException, IOException
	{
		super();
		
		System.setProperty("org.xml.sax.driver",
				"org.apache.crimson.parser.XMLReaderImpl");
		
		XMLReader xr = XMLReaderFactory.createXMLReader();
		xr.setContentHandler(this);
		xr.setErrorHandler(this);
		
		String path = "Ontology-Gen-1-3.xml";
		URL url = getClass().getResource(path);
		System.out.println(url);
		if (url == null)
		{
			throw new AssertionError("file not found: "+path);
		}
		xr.parse(new InputSource(url.openStream()));
	}
    
    private static OntologyDb m_Instance = null;
    
    public static OntologyDb getInstance() throws IOException, SAXException
    {
        if (m_Instance == null)
        {
            m_Instance = new OntologyDb();
        }
        
        return m_Instance;
    }
	
	private List m_OntologyEntries = new Vector(); 
	
	public class OntologyEntry
	{
		public String m_HebrewLexeme, m_EnglishGloss;
        public Long   m_Synset;
	}
	
    private String m_CurrentCluster = null;
    private String m_CurrentSynset  = null;
	private OntologyEntry m_CurrentEntry = null;
	private StringBuffer  m_CharBuffer = null;
	private Map m_OntologyEntriesMap = new Hashtable();
	
	public void startElement (String uri, String name,
			String qName, Attributes atts)
	{
        if (qName.equals("entry_cluster"))
        {
            if (m_CurrentCluster != null)
            {
                throw new AssertionError("Already processing an " +
                    "entry cluster!");
            }
            m_CurrentCluster = atts.getValue("id");
        }
        else if (qName.equals("WordNet_synset"))
        {
            if (m_CurrentSynset != null)
            {
                throw new AssertionError("Already have a synset!");
            }
            if (m_CharBuffer != null)
            {
                throw new AssertionError("Already processing a string");
            }
            m_CharBuffer = new StringBuffer();
        }
        else if (qName.equals("ontology_entry"))
		{
			if (m_CurrentEntry != null)
			{
				throw new AssertionError("Already processing an " +
						"ontology entry!");
			}
            if (m_CurrentSynset == null)
            {
                throw new AssertionError("Do not have a synset!");
            }
			m_CurrentEntry = new OntologyEntry();
            m_CurrentEntry.m_Synset = new Long(m_CurrentSynset
                .replaceFirst(".*-", ""));
		}
		else if (qName.equals("hebrew_lexeme") ||
				qName.equals("english_gloss"))
		{
			if (m_CurrentEntry == null && !qName.equals("english_gloss"))
			{
				throw new AssertionError("Not yet processing an " +
						"ontology entry!");
			}
			if (m_CharBuffer != null)
			{
				throw new AssertionError("Already processing a string");
			}
			m_CharBuffer = new StringBuffer();
		}
		/*
		if ("".equals (uri))
			System.out.println("Start element: " + qName);
		else
			System.out.println("Start element: {" + uri + "}" + name);
		*/
	}

	public void endElement (String uri, String name, String qName)
    {
        if (qName.equals("entry_cluster"))
        {
            if (m_CurrentCluster == null)
            {
                throw new AssertionError("Not yet processing an " +
                    "entry cluster!");
            }
            m_CurrentCluster = null;
            m_CurrentSynset  = null;
        }
        else if (qName.equals("WordNet_synset"))
        {
            if (m_CurrentSynset != null)
            {
                throw new AssertionError("Already have a synset!");
            }
            if (m_CharBuffer == null)
            {
                throw new AssertionError("Not yet processing a string");
            }
            m_CurrentSynset = m_CharBuffer.toString();
            m_CharBuffer    = null;
        }
        else if (qName.equals("ontology_entry"))
		{
			if (m_CurrentEntry == null)
			{
				throw new AssertionError("Not yet processing an " +
						"ontology entry!");
			}
			m_OntologyEntries.add(m_CurrentEntry);
			m_OntologyEntriesMap.put(m_CurrentEntry.m_HebrewLexeme, 
					m_CurrentEntry);
			m_CurrentEntry = null;
		}
		else if (qName.equals("hebrew_lexeme") ||
				qName.equals("english_gloss"))
		{
			if (m_CurrentEntry == null && ! qName.equals("english_gloss"))
			{
				// english_gloss does occur outside of ontology_entry/s
				throw new AssertionError("Not yet processing an " +
						"ontology entry!");
			}
			if (m_CharBuffer == null)
			{
				throw new AssertionError("Not yet processing a string");
			}
			if (m_CurrentEntry != null)
			{
				if (qName.equals("hebrew_lexeme"))
				{
					m_CurrentEntry.m_HebrewLexeme = m_CharBuffer.toString();
				}
				else if (qName.equals("english_gloss"))
				{
					m_CurrentEntry.m_EnglishGloss = m_CharBuffer.toString();
				}
				else
				{
					throw new AssertionError("buffer not handled");
				}
			}
			m_CharBuffer = null;
		}
		/*
		if ("".equals (uri))
			System.out.println("End element: " + qName);
		else
			System.out.println("End element:   {" + uri + "}" + name);
		*/
    }

	public void characters (char ch[], int start, int length)
    {
		if (m_CharBuffer != null)
		{
			m_CharBuffer.append(ch, start, length);
		}
    }
    
	
	public OntologyEntry getWordByLexeme(String lexeme)
	{
		OntologyEntry entry = (OntologyEntry)
		(
				m_OntologyEntriesMap.get(lexeme)
		);
		return entry;
	}
}
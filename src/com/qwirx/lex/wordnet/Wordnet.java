package com.qwirx.lex.wordnet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;

public class Wordnet
{
    private static Wordnet ms_Instance;
    
    public synchronized static Wordnet getInstance()
    throws IOException, JWNLException
    {
        if (ms_Instance == null)
        {
            ms_Instance = new Wordnet();
        }
        
        return ms_Instance;
    }
    
    public synchronized static void delete()
    {
        ms_Instance = null;
        JWNL.shutdown();
    }
    
    private Wordnet() throws IOException, JWNLException
    {
        InputStream is = Wordnet.class.getResourceAsStream(
            "jwnl_properties.xml");
        InputStreamReader isr = new InputStreamReader(is);
        
        StringWriter sw = new StringWriter();
        char[] buf = new char[4096];

        for (int len = isr.read(buf); len > 0; len = isr.read(buf))
        {
            sw.write(buf, 0, len);
        }
        
        String xml = sw.toString();
        
        URL url = Wordnet.class.getResource("dict");
        xml = xml.replaceFirst("@DIR@", url.getPath());
        
        byte [] bytes = xml.getBytes("UTF-8");
        JWNL.initialize(new ByteArrayInputStream(bytes));
    }
    
    public Synset [] getSenses(POS pos, String lemma)
    throws JWNLException
    {
        IndexWord iw = Dictionary.getInstance().getIndexWord(pos, lemma);
        if (iw == null) return null;
        return iw.getSenses();
    }
}

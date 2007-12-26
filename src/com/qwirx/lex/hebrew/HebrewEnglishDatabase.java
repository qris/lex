package com.qwirx.lex.hebrew;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.qwirx.csv.CommaSeparatedValueParser;

public class HebrewEnglishDatabase
{
    public static class Entry
    {
        private int m_StrongsNum;
        private String m_Language, m_TWOT, m_Form, m_GkRelated, 
            m_FullerMeaning, m_UnpointedHeb, m_CALUnpointedAscii,
            m_TABSUnpointedAscii, m_PointedHeb, m_Transliteration,
            m_Phonetic, m_Notes, m_Meaning, m_FullMeaning, m_TranslationInAV;
        public Entry(String [] row)
        {
            assert(row.length == 16);
            m_StrongsNum = Integer.parseInt(row[0]);
            m_Language = row[1];
            m_TWOT = row[2];
            m_Form = row[3];
            m_GkRelated = row[4]; 
            m_FullerMeaning = row[5];
            m_UnpointedHeb = row[6];
            m_CALUnpointedAscii = row[7];
            m_TABSUnpointedAscii = row[8];
            m_PointedHeb = row[9];
            m_Transliteration = row[10];
            m_Phonetic = row[11];
            m_Notes = row[12];
            m_Meaning = row[13];
            m_FullMeaning = row[14];
            m_TranslationInAV = row[15];
        }
        public int getStrongsNum() { return m_StrongsNum; }
        public String getLanguage() { return m_Language; }
        public String getTWOT() { return m_TWOT; }
        public String getForm() { return m_Form; }
        public String getGkRelated() { return m_GkRelated; }
        public String getFullerMeaning() { return m_FullerMeaning; }
        public String getUnpointedHeb() { return m_UnpointedHeb; }
        public String getCALUnpointedAscii() { return m_CALUnpointedAscii; }
        public String getTABSUnpointedAscii() { return m_TABSUnpointedAscii; }
        public String getPointedHeb() { return m_PointedHeb; }
        public String getTransliteration() { return m_Transliteration; }
        public String getPhonetic() { return m_Phonetic; }
        public String getNotes() { return m_Notes; }
        public String getMeaning() { return m_Meaning; }
        public String getFullMeaning() { return m_FullMeaning; }
        public String getTranslationInAV() { return m_TranslationInAV; }
    }
    
    private Map<Integer, Entry> m_StrongsNumToEntryMap = 
        new HashMap<Integer, Entry>();
    
    private HebrewEnglishDatabase() throws IOException
    {
        // test data/hebrew dictionary from 
        // http://crosswire.org/~scribe/greekheb/hebrewDiB.xls
        
        InputStream fis = getClass().getResourceAsStream(
            "/com/qwirx/lex/hebrew/crosswire_hebrew_english_dict.csv");
        assert(fis != null);
        String [][] parse = CommaSeparatedValueParser.parse(fis);
        fis.close();
        
        for (int i = 1; i < parse.length; i++)
        {
            Entry entry = new Entry(parse[i]);
            m_StrongsNumToEntryMap.put(entry.getStrongsNum(), entry);
        }
    }
    
    public Entry get(int strongsNum)
    {
        return m_StrongsNumToEntryMap.get(strongsNum);
    }
    
    private static HebrewEnglishDatabase m_Instance = null;
    
    public synchronized static HebrewEnglishDatabase getInstance()
    throws IOException
    {
        if (m_Instance == null)
        {
            m_Instance = new HebrewEnglishDatabase();
        }
        
        return m_Instance;
    }
}

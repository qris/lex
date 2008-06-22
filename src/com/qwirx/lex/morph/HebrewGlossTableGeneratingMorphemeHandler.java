package com.qwirx.lex.morph;

import java.util.List;

import jemdros.EmdrosException;
import jemdros.MatchedObject;

import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.parser.MorphEdge;

public class HebrewGlossTableGeneratingMorphemeHandler
implements MorphemeHandler
{
    private List<String[]> m_Columns;
    private MatchedObject m_Word;
    private StringBuffer m_Hebrew;
    private List<MorphEdge> m_MorphEdges;
    boolean m_IsFirstWord, m_IsLastWord;
    boolean m_IsMorpheme;
    
    public HebrewGlossTableGeneratingMorphemeHandler(
        List<String[]> columns,
        MatchedObject word,
        StringBuffer hebrew,
        List<MorphEdge> morphEdges,
        boolean isFirstWord,
        boolean isLastWord)
    {
        m_Columns = columns;
        m_Word   = word;
        m_Hebrew = hebrew;
        m_MorphEdges = morphEdges;
        m_IsFirstWord = isFirstWord;
        m_IsLastWord  = isLastWord;
    }
    
    public boolean isMorpheme()
    {
        return m_IsMorpheme;
    }
    
    public void convert(String surface, 
        boolean lastMorpheme, String desc,
        String morphNode)
    throws EmdrosException
    {
        String raw = m_Word.getEMdFValue(surface).getString();

        String hebrew = HebrewConverter.toHebrew(raw);
        m_Hebrew.append(hebrew);

        String translit = HebrewConverter.toTranslit(raw);
        translit = HebrewConverter.toHtml(translit);
        if (translit.equals("")) translit = "&Oslash;";
        
        if (desc != null)
        {
            if (desc.equals("CONJ"))
            {
                if (m_IsFirstWord) { desc = "CLM"; }
                else               { desc = "CR"; }
            }
            else if (desc.equals("PERF"))
            {
                if (m_Columns.size() > 0 &&
                    m_Columns.get(m_Columns.size() - 1)[1].equals("CLM-"))
                {
                    desc = "SER2";
                }
            }
        }
        
        // desc += ":" + lastMorpheme + ":" + m_IsLastWord;
        
        if (!lastMorpheme)
        {
            translit += "-";
            desc += "-";
            m_IsMorpheme = true;
        }
        else if (translit.endsWith("-"))
        {
            desc += "-";
            lastMorpheme = false;
            m_IsMorpheme = true;
        }
        else
        {
            m_IsMorpheme = false;
        }

        if (desc == null) desc = "";

        m_Columns.add(new String[]{translit, desc});
        
        if (lastMorpheme && !m_IsLastWord)
        {
            // blank cell between words
            m_Columns.add(new String[]{"",""});
        }
        
        m_MorphEdges.add(new MorphEdge(morphNode, 
            translit, m_MorphEdges.size()));
    }
}

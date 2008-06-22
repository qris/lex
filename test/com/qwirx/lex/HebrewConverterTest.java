package com.qwirx.lex;

import java.util.ArrayList;
import java.util.List;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import junit.framework.TestCase;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewGlossTableGeneratingMorphemeHandler;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.parser.MorphEdge;

public class HebrewConverterTest extends TestCase
{
    public void testToTranslit()
    {
        assertEquals("nākərîî", HebrewConverter.toTranslit("N@K:RIJ."));
        assertEquals("jiśśāskār", HebrewConverter.toTranslit("JIF.@#K@R"));
        assertEquals("rē?šî", HebrewConverter.toTranslit("R;>CI73J"));
        assertEquals("îm", HebrewConverter.toTranslit("I92Jm"));
    }
    
    SqlDatabase m_SQL;
    EmdrosDatabase m_Emdros;

    public void setUp() throws Exception
    {
        m_SQL = Lex.getSqlDatabase("test");
        m_Emdros = Lex.getEmdrosDatabase("test", "test", m_SQL);
    }

    private List<String[]> getGlossColumns(int clauseId)
    throws Exception
    {
        StringBuffer hebrewText = new StringBuffer();
        List<String[]> columns = new ArrayList<String[]>();
        
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + m_Emdros.getMinM() + "-" + m_Emdros.getMaxM() + "} " +
            "WHERE [clause self = " + clauseId + " " +
            "       GET logical_struct_id, logical_structure "+
            "        [phrase GET phrase_type, phrase_function, argument_name, "+
            "                    type_id, macrorole_number "+
            "          [word GET lexeme, phrase_dependent_part_of_speech, " +
            "                    tense, stem, wordnet_gloss, wordnet_synset, " +
            "                    graphical_preformative, " +
            "                    graphical_locative, " +
            "                    graphical_lexeme, " +
            "                    graphical_pron_suffix, " +
            "                    graphical_verbal_ending, " +
            "                    graphical_root_formation, " +
            "                    graphical_nominal_ending, " +
            "                    person, number, gender, state, " +
            "                    surface_consonants, " +
            "                    suffix_person, suffix_number, suffix_gender " +
            "          ]"+
            "        ]"+
            "      ]"
        );

        MatchedObject clause = null;
        SheafConstIterator sci = sheaf.const_iterator();
        if (sci.hasNext()) 
        {
            Straw straw = sci.next();
            StrawConstIterator swci = straw.const_iterator();
            if (swci.hasNext()) 
            {
                clause = swci.next();
            }
        }

        SheafConstIterator phrases = clause.getSheaf().const_iterator();
        List<MorphEdge> morphEdges = new ArrayList<MorphEdge>();
        boolean isFirstWord = true;
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase =
                phrases.next().const_iterator().next();

            SheafConstIterator words = phrase.getSheaf().const_iterator();
            
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();

                HebrewGlossTableGeneratingMorphemeHandler hmh = 
                    new HebrewGlossTableGeneratingMorphemeHandler(
                        columns, word, hebrewText,
                        morphEdges, isFirstWord,
                        !phrases.hasNext() && !words.hasNext());
                    
                generator.parse(word, hmh, true, m_SQL);
            }
        }
        
        return columns;
    }
    
    public void testGloss() throws Exception
    {
        List<String[]> columns = getGlossColumns(28951); // Gen 2,24(b)
        assertEquals("CLM-",  columns.get(0)[1]);
        assertEquals("SER2-", columns.get(1)[1]);

        columns = getGlossColumns(590946); // IKON11,2(f)
        assertEquals("P-",    columns.get(0)[1]);
        assertEquals("3Mpl",  columns.get(1)[1]);
        assertEquals("",      columns.get(2)[1]);
        assertEquals("PERF-", columns.get(3)[1]);
    }

    public void testMorphemeGeneration() throws Exception
    {
        StringBuffer hebrewText = new StringBuffer();
        List<String[]> columns = new ArrayList<String[]>();
        
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + m_Emdros.getMinM() + "-" + m_Emdros.getMaxM() + "} " +
            "WHERE [clause self = 28951 " + // Gen 2,24(b)
            "       GET logical_struct_id, logical_structure "+
            "        [phrase GET phrase_type, phrase_function, argument_name, "+
            "                    type_id, macrorole_number "+
            "          [word GET lexeme, phrase_dependent_part_of_speech, " +
            "                    tense, stem, wordnet_gloss, wordnet_synset, " +
            "                    graphical_preformative, " +
            "                    graphical_locative, " +
            "                    graphical_lexeme, " +
            "                    graphical_pron_suffix, " +
            "                    graphical_verbal_ending, " +
            "                    graphical_root_formation, " +
            "                    graphical_nominal_ending, " +
            "                    person, number, gender, state, " +
            "                    surface_consonants, " +
            "                    suffix_person, suffix_number, suffix_gender " +
            "          ]"+
            "        ]"+
            "      ]"
        );

        MatchedObject clause = null;
        SheafConstIterator sci = sheaf.const_iterator();
        if (sci.hasNext()) 
        {
            Straw straw = sci.next();
            StrawConstIterator swci = straw.const_iterator();
            if (swci.hasNext()) 
            {
                clause = swci.next();
            }
        }

        SheafConstIterator phrases = clause.getSheaf().const_iterator();
        List<MorphEdge> morphEdges = new ArrayList<MorphEdge>();
        boolean isFirstWord = true;
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase =
                phrases.next().const_iterator().next();

            SheafConstIterator words = phrase.getSheaf().const_iterator();
            
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();

                HebrewGlossTableGeneratingMorphemeHandler hmh = 
                    new HebrewGlossTableGeneratingMorphemeHandler(
                        columns, word, hebrewText,
                        morphEdges, isFirstWord,
                        !phrases.hasNext() && !words.hasNext());
                    
                generator.parse(word, hmh, true, m_SQL);
            }
        }

        assertEquals("CONJ",     morphEdges.get(0).symbol());
        assertEquals("V/TAM",    morphEdges.get(1).symbol());
        assertEquals("V/STM",    morphEdges.get(2).symbol());
        assertEquals("V/NUC",    morphEdges.get(3).symbol());
        assertEquals("AG/PSA",   morphEdges.get(4).symbol());
        assertEquals("PRON/DCA", morphEdges.get(5).symbol());
        assertEquals("P",        morphEdges.get(6).symbol());
        assertEquals("N/NUC",    morphEdges.get(7).symbol());
        assertEquals("N/PNS",    morphEdges.get(8).symbol());
        assertEquals("N/POS",    morphEdges.get(9).symbol());
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HebrewConverterTest.class);
    }
}

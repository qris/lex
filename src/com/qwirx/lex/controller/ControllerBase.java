package com.qwirx.lex.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.StringList;
import jemdros.StringListConstIterator;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;
import com.qwirx.lex.parser.MorphEdge;
import com.qwirx.lex.translit.DatabaseTransliterator;

public abstract class ControllerBase
{
    protected HttpServletRequest m_Request;
    protected EmdrosDatabase m_Emdros;
    protected SqlDatabase m_Sql;
    protected DatabaseTransliterator m_Transliterator;
    protected Navigator m_Navigator;
    protected Map<String, String> m_PhraseFunctions, m_PhraseTypes,
        m_PartsOfSpeech,  m_Gender, m_Number, m_Person, m_Stem, m_Tense;
    protected MatchedObject m_Clause;

    /**
     * For unit tests only!
     * @param request
     * @param emdros
     * @param sql
     * @param navigator
     * @throws Exception
     */
    public ControllerBase(EmdrosDatabase emdros, SqlDatabase sql)
    throws Exception
    {
        this(null, emdros, sql);
        m_Navigator = null;
        m_Transliterator = new DatabaseTransliterator(sql);
    }

    public ControllerBase(HttpServletRequest request, EmdrosDatabase emdros,
        SqlDatabase sql, Navigator navigator)
    throws Exception
    {
        this(request, emdros, sql);
        m_Navigator = navigator;
        m_Transliterator = navigator.getTransliterator();
    }
    
    private ControllerBase(HttpServletRequest request, EmdrosDatabase emdros,
        SqlDatabase sql)
    throws Exception
    {
        m_Request = request;
        m_Emdros = emdros;
        m_Sql = sql;
        
        m_PhraseFunctions = m_Emdros.getEnumerationConstants
            ("phrase_function_e",false);
        
        m_PhraseTypes = m_Emdros.getEnumerationConstants
            ("phrase_type_e",false);
        
        m_PartsOfSpeech = m_Emdros.getEnumerationConstants
            ("part_of_speech_e",false);
        
        m_Person = m_Emdros.getEnumerationConstants("person_e", false);
        m_Gender = m_Emdros.getEnumerationConstants("gender_e", false);
        m_Number = m_Emdros.getEnumerationConstants("number_e", false);
        m_Tense = m_Emdros.getEnumerationConstants("tense_e", false);
        m_Stem = m_Emdros.getEnumerationConstants("stem_e", false);
    }

    /**
     * Subclasses should call this from their constructors, after making
     * any required changes to the clause objects in Emdros, to avoid the
     * need to reload the objects later. 
     * @throws Exception
     */
    protected void loadClause()
    throws Exception
    {
        loadClause(m_Navigator.getMinM(), m_Navigator.getMaxM(),
            m_Navigator.getClauseId());
    }
    
    /**
     * For unit tests only!
     * @throws Exception
     */
    protected void loadClause(int minM, int maxM, int clauseId)
    throws Exception
    {
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + minM + "-" + maxM + "} " + 
            "WHERE [clause self = " + clauseId +
            "       GET logical_struct_id, logical_structure "+
            "        [phrase GET phrase_type, phrase_function, argument_name, "+
            "                    type_id, macrorole_number "+
            "          [word GET lexeme_wit, phrase_dependent_part_of_speech, " +
            "                    tense, stem, wordnet_gloss, wordnet_synset, " +
            "                    graphical_preformative_utf8, " +
            "                    graphical_locative_utf8, " +
            "                    graphical_lexeme_utf8, " +
            "                    graphical_pron_suffix_utf8, " +
            "                    graphical_verbal_ending_utf8, " +
            "                    graphical_root_formation_utf8, " +
            "                    graphical_nominal_ending_utf8, " +
            "                    person, number, gender, state, " +
            "                    suffix_person, suffix_number, suffix_gender " +
            "          ]"+
            "        ]"+
            "      ]"
        );

        m_Clause = null;
        SheafConstIterator sci = sheaf.const_iterator();
        
        if (sci.hasNext()) 
        {
            Straw straw = sci.next();
            StrawConstIterator swci = straw.const_iterator();
            if (swci.hasNext()) 
            {
                m_Clause = swci.next();
            }
        }
    }

    public MatchedObject getClause()
    {
        return m_Clause;
    }
    
    public String getHebrewText()
    throws Exception
    {
        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();
        StringBuffer hebrew = new StringBuffer();
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase = phrases.next().const_iterator().next();
            SheafConstIterator words = phrase.getSheaf().const_iterator();
            
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();
                List<Morpheme> morphemes = generator.parse(word, true, m_Sql);
                for (Morpheme morpheme : morphemes)
                {
                    hebrew.append(morpheme.getSurface());
                }
                hebrew.append(" ");
            }
        }
        
        return hebrew.toString();
    }

    public List<MorphEdge> getMorphEdges()
    throws Exception
    {
        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();
        List<MorphEdge> morphEdges = new ArrayList<MorphEdge>();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase = phrases.next().const_iterator().next();
            SheafConstIterator words = phrase.getSheaf().const_iterator();
            
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();
                List<Morpheme> morphemes = generator.parse(word, true, m_Sql);
                
                for (int i = 0; i < morphemes.size(); i++)
                {
                    Morpheme morpheme = morphemes.get(i);
                    String translit = m_Transliterator.transliterate(morphemes,
                        i, word);
                    translit = HebrewConverter.toHtml(translit);
                    if (translit.equals("")) translit = "&Oslash;";
                    
                    if (i < morphemes.size() - 1)
                    {
                        translit += "-";
                    }

                    String gloss = morpheme.getGloss();
                    if (gloss == null) gloss = "";

                    Map<String, String> attributes =
                        new HashMap<String, String>();
                    
                    // not safe, crashes emdros 2.0.6, emailed ulrik 090403
                    /*
                    StringList features = word.getFeatureList();
                    for (StringListConstIterator iter = features.const_iterator();
                        iter.hasNext();)
                    {
                        String feature = iter.next();
                        attributes.put("word_" + feature,
                            word.getFeatureAsString(
                                word.getEMdFValueIndex(feature)));
                    }
                    */
                    
                    // also not safe
                    /*
                    String[] featuresToCopy = new String[]{"person", "number",
                        "gender", "tense", "stem"
                    };
                    for (String feature : featuresToCopy)
                    {
                        System.out.println(feature);
                        attributes.put("word_" + feature,
                            word.getFeatureAsString(
                                word.getEMdFValueIndex(feature)));
                    }
                    */

                    attributes.put("word_person",
                        m_Person.get("" + word.getEMdFValue("person").getEnum()));
                    attributes.put("word_number",
                        m_Number.get("" + word.getEMdFValue("number").getInt()));
                    attributes.put("word_gender",
                        m_Gender.get("" + word.getEMdFValue("gender").getInt()));
                    attributes.put("word_tense",
                        m_Tense.get("" + word.getEMdFValue("tense").getInt()));
                    attributes.put("word_stem",
                        m_Stem.get("" + word.getEMdFValue("stem").getInt()));

                    morphEdges.add(new MorphEdge(morpheme.getNodeSymbol(), 
                        translit, morphEdges.size(),
                        i == 0, i == morphemes.size() - 1, attributes));
                }
            }
        }
        
        return morphEdges;
    }
}

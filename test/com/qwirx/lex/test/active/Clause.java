package com.qwirx.lex.test.active;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;

import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class Clause
{
    private EmdrosDatabase m_Emdros;
    private SqlDatabase    m_Lexicon;
    private MatchedObject  m_Clause;
    private DatabaseTransliterator m_Transliterator;
    
    private Clause(EmdrosDatabase emdros, SqlDatabase lexicon,
        MatchedObject clause, DatabaseTransliterator transliterator)
    {
        m_Emdros  = emdros;
        m_Lexicon = lexicon;
        m_Clause  = clause;
        m_Transliterator = transliterator;
    }
    
    public static Clause find(EmdrosDatabase emdros, SqlDatabase lexicon,
        DatabaseTransliterator transliterator, int id)
    throws DatabaseException, EmdrosException
    {
        String query = 
            "SELECT ALL OBJECTS IN " +
            emdros.getVisibleMonads() +
            " WHERE [clause self = "+id+
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
            "      ]";
        
        Sheaf sheaf = emdros.getSheaf(query);
        Straw straw = sheaf.const_iterator().next();
        MatchedObject clause = straw.const_iterator().next();
        
        return new Clause(emdros, lexicon, clause, transliterator);
    }
    
    public int getLogicalStructureID()
    throws DatabaseException, EmdrosException
    {
        return m_Clause.getEMdFValue("logical_struct_id").getInt();
    }
    
    public String getEvaluatedLogicalStructure()
    throws Exception
    {
        int lsid = getLogicalStructureID();
        
        Lexeme lexeme = Lexeme.load(m_Lexicon, lsid);
        if (lexeme == null)
        {
            /*
            throw new AssertionError("Logical structure not found " +
                    "with ID = " + lsid + " for clause " + m_Clause.getID_D());
            */
            return null;
        }
        
        String logicalStructure = lexeme.getLogicalStructure();
        Set<String> argNames = new TreeSet<String>();

        Pattern varPat = Pattern.compile
            ("(?s)(?i)<([^>]*)>");
        Matcher m = varPat.matcher(logicalStructure);
        while (m.find()) 
        {
            String arg = m.group(1);
            argNames.add(m.group(1));
        }

        Map<String, MatchedObject> variables = 
            new HashMap<String, MatchedObject>();

        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();
        while (phrases.hasNext())
        {
            MatchedObject phrase = phrases.next().const_iterator().next();
            String argName = phrase.getEMdFValue("argument_name").toString();

            if (argName.equals("")) 
            {
                // Argument not decided yet. Maybe we can guess
                // based on the "function" of the clause?
    
                String function_name = null;
                
                function_name = phrase.getFeatureAsString(
                    phrase.getEMdFValueIndex("phrase_function"));

                if (function_name.equals("Subj") ||
                    function_name.equals("PreS") ||
                    function_name.equals("IrpS") ||
                    function_name.equals("ModS"))
                {
                    argName = "x";
                }
                else if (function_name.equals("Objc") ||
                    function_name.equals("PreC") ||
                    function_name.equals("PreO") ||
                    function_name.equals("PtcO") ||
                    function_name.equals("IrpO"))
                {
                    argName = "y";
                }
            }
            
            variables.put(argName, phrase);
        }
        
        HebrewMorphemeGenerator hmg = new HebrewMorphemeGenerator();

        for (Iterator<String> i = variables.keySet().iterator(); i.hasNext();) 
        {
            String variable = i.next();
            MatchedObject phrase = variables.get(variable);
            String value_text = "";
            SheafConstIterator sci = phrase.getSheaf().const_iterator();
            
            while (sci.hasNext())
            {
                try
                {
                    MatchedObject word = sci.next().const_iterator().next();
                    value_text += HebrewConverter.wordTranslitToHtml(word, hmg,
                        m_Transliterator);
                }
                catch (EmdrosException e)
                {
                    throw new DatabaseException("Failed to transliterate word", 
                        e);
                }
                
                if (sci.hasNext())
                {
                    value_text += " ";
                }
            }
            
            logicalStructure = logicalStructure.replaceAll("<" +
                variable + ">", value_text);
        }
        
        return logicalStructure;
    }

    public String getPredicateText()
    throws EmdrosException
    {
        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase =
                phrases.next().const_iterator().next();

            String function_name = phrase.getFeatureAsString(
                phrase.getEMdFValueIndex("phrase_function"));

            SheafConstIterator words = phrase.getSheaf().const_iterator();
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();

                String psp = word.getFeatureAsString(
                    word.getEMdFValueIndex("phrase_dependent_part_of_speech")); 
                    
                if (psp.equals("verb"))
                {
                    return word.getEMdFValue("lexeme").getString();
                }
            }
        }
        
        return null;
    }
}

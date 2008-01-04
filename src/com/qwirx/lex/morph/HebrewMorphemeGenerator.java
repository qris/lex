package com.qwirx.lex.morph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jemdros.EMdFValue;
import jemdros.MatchedObject;

import org.xml.sax.SAXException;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.Lex;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.ontology.OntologyDb;

public class HebrewMorphemeGenerator
{
    private static Map m_Persons, m_Genders, m_Numbers, m_States, 
        m_PartsOfSpeech, m_Tenses;
    private OntologyDb m_Ontology;
    
    public HebrewMorphemeGenerator(EmdrosDatabase emdros)
    throws DatabaseException, IOException, SAXException
    {
        if (m_Persons == null)
        {
            m_Persons = emdros.getEnumerationConstants("person_e", false);
        }
        
        if (m_Genders == null)
        {
            m_Genders = emdros.getEnumerationConstants("gender_e", false);
        }
        
        if (m_Numbers == null)
        {
            m_Numbers = emdros.getEnumerationConstants("number_e", false);
        }
        
        if (m_States == null)
        {
            m_States = emdros.getEnumerationConstants("state_e", false);
        }
        
        if (m_PartsOfSpeech == null)
        {
            m_PartsOfSpeech = emdros.getEnumerationConstants("part_of_speech_e",
                false);
        }

        if (m_Tenses == null)
        {
            m_Tenses = emdros.getEnumerationConstants("tense_e", false);
        }

        /*
        if (m_Stems == null)
        {
            m_Stems = emdros.getEnumerationConstants("verbal_stem_t", false);
            
        }
        */

        if (m_Ontology == null)
        {
            m_Ontology = Lex.getOntologyDb();
        }
    }
    
    public void parse(MatchedObject word, MorphemeHandler handler,
        boolean generateGloss)
    {
        if (!word.getObjectTypeName().equals("word"))
        {
            throw new IllegalArgumentException("Can only parse words");
        }
        
        List<String> requiredFeatures = Arrays.asList(new String[]{
            "phrase_dependent_part_of_speech",
            "graphical_preformative",
            "graphical_root_formation",
            "graphical_lexeme",
            "graphical_verbal_ending",
            "graphical_nominal_ending",
            "graphical_pron_suffix",
        });
        
        if (generateGloss)
        {
            requiredFeatures = new ArrayList<String>(requiredFeatures);
            requiredFeatures.addAll(Arrays.asList(new String[]{
                "person",
                "gender",
                "number",
                "state",
                "wordnet_gloss",
                "lexeme",
                "tense",
                "stem",
            }));
        }
        
        for (Iterator<String> i = requiredFeatures.iterator(); i.hasNext();)
        {
            String feature = i.next();
            if (word.getEMdFValue(feature) == null)
            {
                throw new IllegalArgumentException("Word does not have " +
                        "required feature " + feature);
            }
        }
        
        EMdFValue pspValue = word.getEMdFValue("phrase_dependent_part_of_speech");
        String pspCode = pspValue.toString();
        String psp = (String)m_PartsOfSpeech.get(pspCode);
       
        String gloss = null;
        String verbEnding = null;
        String nounEnding = null; 
        
        if (generateGloss)
        {
            String person = (String)m_Persons.get(
                word.getEMdFValue("person").toString());
            if      (person.equals("first_person"))  person = "1";
            else if (person.equals("second_person")) person = "2";
            else if (person.equals("third_person"))  person = "3";
            else if (person.equals("unknown"))       person = "";
            
            String gender;
            switch (word.getFeatureAsLong(word.getEMdFValueIndex("gender")))
            {
            case 2: gender = ""; break;
            case 3: gender = "M"; break;
            case 4: gender = "F"; break;
            default: gender = "=" + word.getFeatureAsLong(
                word.getEMdFValueIndex("gender"));
            }
            
            String number;
            switch (word.getFeatureAsLong(word.getEMdFValueIndex("number")))
            {
            case 0: number = ""; break;
            case 3: number = "sg"; break;
            case 4: number = "pl"; break;
            case 5: number = "dl"; break;
            default: number = word.getFeatureAsString(
                word.getEMdFValueIndex("number"));
            }
            
            String state = (String)m_States.get(
                word.getEMdFValue("state").toString());
            if      (state.equals("construct")) { state = "CS"; }
            else if (state.equals("absolute"))  { state = "AB"; }
            
            gloss = word.getEMdFValue("wordnet_gloss").getString();
            
            if (gloss.equals(""))
            {
                String lexeme = word.getEMdFValue("lexeme")
                    .getString();
                    
                OntologyDb.OntologyEntry entry = 
                    m_Ontology.getWordByLexeme(lexeme);
                    
                if (entry != null)
                {
                    gloss = entry.m_EnglishGloss;
                }
                else
                {
                    gloss = null;
                }
            }
            
            verbEnding = person + gender + number;
            nounEnding = gender + number + state;
        }
        
        if (psp.equals("verb"))
        {
            String tense = null;
            String stem  = null;
            
            if (generateGloss)
            {
                tense = word.getFeatureAsString(word.getEMdFValueIndex("tense"));
                if      (tense.equals("imperfect"))  { tense = "IMPF"; }
                else if (tense.equals("perfect"))    { tense = "PRFV"; }
                else if (tense.equals("imperative")) { tense = "IMP"; }
                else if (tense.equals("infinitive_construct")) { tense = "INF"; }
                else if (tense.equals("infinitive_absolute"))  { tense = "EMPH"; }
                else if (tense.equals("participle")) { tense = "PART"; }
                else if (tense.equals("wayyiqtol"))  { tense = "NARR"; }

                stem = word.getFeatureAsString(word.getEMdFValueIndex("stem"));
                if      (stem.equals("qal"))     { stem = "Qa"; }
                else if (stem.equals("piel"))    { stem = "Pi"; }
                else if (stem.equals("hifil"))   { stem = "Hi"; } 
                else if (stem.equals("nifal"))   { stem = "Ni"; }
                else if (stem.equals("pual"))    { stem = "Pu"; }
                else if (stem.equals("hitpael")) { stem = "Hit"; }
                else if (stem.equals("hofal"))   { stem = "Ho"; }
            }
            
            handler.convert("graphical_preformative", false, tense, "V/TNS");
            
            // String stemNum = word.getEMdFValue("verbal_stem").toString();
            handler.convert("graphical_root_formation", false,
                stem, "V/STM");
            
            handler.convert("graphical_lexeme", false, gloss, "V/LEX");
            
            handler.convert("graphical_verbal_ending", false,
                verbEnding, "V/PGN");

            handler.convert("graphical_pron_suffix", true, "SUFF", "V/SFX");
        }
        else if (psp.equals("noun")
            || psp.equals("proper_noun"))
        {
            String type = "HEAD/NCOM";
            
            if (psp.equals("proper_noun"))
            {
                type = "HEAD/NPROP";
            }
            
            handler.convert("graphical_lexeme", false, gloss, type);
            handler.convert("graphical_nominal_ending", false,
                nounEnding, "MARK/N");
            handler.convert("graphical_pron_suffix", true, "SUFF", "SFX/N");
        }
        else
        {
            String type;

            if (psp.equals("adjective"))
            {
                type = "ADJ";
            }
            else if (psp.equals("adverb"))
            {
                type = "ADV";
            }
            else if (psp.equals("article"))
            {
                type = "ART";
            }
            else if (psp.equals("conjunction"))
            {
                type = "CONJ";
            }
            else if (psp.equals("demonstrative_pronoun"))
            {
                type = "PRON/DEM";
            }
            else if (psp.equals("interjection"))
            {
                type = "INTJ";
            }
            else if (psp.equals("interrogative"))
            {
                type = "INTR";
            }
            else if (psp.equals("interrogative_pronoun"))
            {
                type = "PRON/INT";
            }
            else if (psp.equals("negative"))
            {
                type = "NEG";
            }
            else if (psp.equals("personal_pronoun"))
            {
                type = "PRON/PERS";
            }
            else if (psp.equals("pronoun"))
            {
                type = "PRON";
            }
            else if (psp.equals("preposition"))
            {
                type = "P";
            }
            else
            {
                throw new IllegalArgumentException("Unknown " +
                    "part of speech: " + psp);
            }
            
            handler.convert("graphical_lexeme", true, type, type);
        }   

    }
}

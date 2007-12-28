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
            
            String gender = ((String)m_Genders.get(
                word.getEMdFValue("gender").toString()
                )).substring(0, 1);
            
            String number = ((String)m_Numbers.get(
                word.getEMdFValue("number").toString()
                )).substring(0, 1);
            
            String state = (String)m_States.get(
                word.getEMdFValue("state").toString());
            
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
            nounEnding = gender + number + "." + state;
        }
        
        if (psp.equals("verb"))
        {
            String tense = null;
            
            if (generateGloss)
            {
                String tenseNum = word.getEMdFValue("tense").toString();
                tense = (String)m_Tenses.get(tenseNum);
            }
            
            handler.convert("graphical_preformative", false, tense, "V/TNS");
            
            // String stemNum = word.getEMdFValue("verbal_stem").toString();
            handler.convert("graphical_root_formation", false,
                "(stem)", "V/STM");
            
            handler.convert("graphical_lexeme", false, gloss, "V/LEX");
            
            handler.convert("graphical_verbal_ending", false,
                verbEnding, "V/PGN");

            handler.convert("graphical_pron_suffix", true, "SFX", "V/SFX");
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
            handler.convert("graphical_pron_suffix", true, "SFX", "SFX/N");
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
                type = "DET";
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

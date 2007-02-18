package com.qwirx.lex.morph;

import java.io.IOException;
import java.util.Map;

import jemdros.MatchedObject;

import org.xml.sax.SAXException;

import com.qwirx.lex.DatabaseException;
import com.qwirx.lex.Lex;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.ontology.OntologyDb;

public class HebrewMorphemeGenerator
{
    private MorphemeHandler m_Handler;
    private static Map m_Persons, m_Genders, m_Numbers, m_States, 
        m_PartsOfSpeech, m_Tenses;
    private OntologyDb m_Ontology;
    
    public HebrewMorphemeGenerator(EmdrosDatabase emdros, 
        MorphemeHandler handler)
    throws DatabaseException, IOException, SAXException
    {
        m_Handler = handler;
        
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
    
    public void parse(MatchedObject word)
    {
        if (!word.getObjectTypeName().equals("word"))
        {
            throw new AssertionError("Can only parse words");
        }
        
        String psp = (String)( m_PartsOfSpeech.get(
            word.getEMdFValue("phrase_dependent_part_of_speech").toString()) 
        );
        
        String person = (String)m_Persons.get(
            word.getEMdFValue("person").toString());
        if      (person.equals("first_person"))  person = "1";
        else if (person.equals("second_person")) person = "2";
        else if (person.equals("third_person"))  person = "3";
        
        String gender = ((String)m_Genders.get(
            word.getEMdFValue("gender").toString()
            )).substring(0, 1);
        
        String number = ((String)m_Numbers.get(
            word.getEMdFValue("number").toString()
            )).substring(0, 1);
        
        String state = (String)m_States.get(
            word.getEMdFValue("state").toString());
        
        String gloss = word.getEMdFValue("wordnet_gloss")
            .getString();
        
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

        if (psp.equals("verb"))
        {
            String tenseNum = word.getEMdFValue("tense").toString();
            m_Handler.convert("graphical_preformative", false,
                (String)m_Tenses.get(tenseNum), "V/TNS");
            
            // String stemNum = word.getEMdFValue("verbal_stem").toString();
            m_Handler.convert("graphical_root_formation", false,
                "(stem)", "V/STM");
            
            m_Handler.convert("graphical_lexeme", false, 
                gloss != null ? gloss : "&nbsp;", "V/LEX");
            
            m_Handler.convert("graphical_verbal_ending", true,
                person + gender + number, "V/PGN");

            m_Handler.convert("graphical_pron_suffix", true,
                "&nbsp;", "V/SFX");
        }
        else if (psp.equals("noun")
            || psp.equals("proper_noun"))
        {
            String type = "HEAD/NCOM";
            
            if (psp.equals("proper_noun"))
            {
                type = "HEAD/NPROP";
            }
            
            m_Handler.convert("graphical_lexeme", false, gloss, type);
            m_Handler.convert("graphical_nominal_ending", true,
                gender + number + "." + state, "MARK/N");
            m_Handler.convert("graphical_pron_suffix", true,
                "&nbsp;", "SFX/N");
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
            
            m_Handler.convert("graphical_lexeme", true, psp, type);
        }   

    }
}

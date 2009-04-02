package com.qwirx.lex.morph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jemdros.EmdrosException;
import jemdros.MatchedObject;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.lexicon.Lexeme;

public class HebrewMorphemeGenerator
{
    public static class Morpheme
    {
        private String m_Surface, m_Gloss, m_Symbol;
        
        public Morpheme(String surface, String gloss, String symbol)
        {
            m_Surface = surface;
            m_Gloss = gloss;
            m_Symbol = symbol;
        }
        
        public String getSurface() { return m_Surface; }
        public String getGloss() { return m_Gloss; }
        public String getNodeSymbol() { return m_Symbol; }
    }
    
    public List<Morpheme> parse(MatchedObject word, boolean generateGloss,
        SqlDatabase sql)
    throws Exception
    {
        String gloss = null;
        
        if (generateGloss)
        {
            Lexeme lexeme = Lexeme.load(sql, word);
            if (lexeme != null)
            {
                gloss = lexeme.getGloss();
            }
        }
        
        return parse(word, generateGloss, gloss);
    }
    
    public List<Morpheme> parse(MatchedObject word, boolean generateGloss,
        String gloss)
    throws Exception
    {
        if (!word.getObjectTypeName().equals("word"))
        {
            throw new IllegalArgumentException("Can only parse words");
        }
        
        List<String> requiredFeatures = Arrays.asList(new String[]{
            "phrase_dependent_part_of_speech",
            "graphical_preformative_utf8",
            "graphical_root_formation_utf8",
            "graphical_lexeme_utf8",
            "graphical_verbal_ending_utf8",
            "graphical_nominal_ending_utf8",
            "graphical_pron_suffix_utf8",
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
                "lexeme_wit",
                "tense",
                "stem",
                "suffix_gender",
                "suffix_number",
                "suffix_person"
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
        
        String psp = word.getFeatureAsString(
            word.getEMdFValueIndex("phrase_dependent_part_of_speech"));
       
        String verbEnding = null;
        String nounEnding = null;

        String suffixText = 
            word.getEMdFValue("graphical_pron_suffix_utf8").getString();
        String suffixGloss = null;
        
        List<Morpheme> results = new ArrayList<Morpheme>();
        
        if (generateGloss)
        {
            String person = word.getFeatureAsString(
                word.getEMdFValueIndex("person"));
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
            
            String state = word.getFeatureAsString(
                word.getEMdFValueIndex("state"));
            if      (state.equals("construct")) { state = "CS"; }
            else if (state.equals("absolute"))  { state = "AB"; }
            
            verbEnding = person + gender + number;
            nounEnding = gender + number + state;
            
            if (suffixText.equals(""))
            {
                suffixGloss = "SUFF";
            }
            else
            {
                String suffixGender = word.getFeatureAsString(
                    word.getEMdFValueIndex("suffix_gender"));
                if      (suffixGender.equals("masculine")) { suffixGender = "M"; }
                else if (suffixGender.equals("feminine"))  { suffixGender = "F"; }
                else if (suffixGender.equals("common"))    { suffixGender = "="; }

                String suffixNumber = word.getFeatureAsString(
                    word.getEMdFValueIndex("suffix_number"));
                if      (suffixNumber.equals("singular")) { suffixNumber = "sg"; }
                else if (suffixNumber.equals("plural"))   { suffixNumber = "pl"; }

                String suffixPerson = word.getFeatureAsString(
                    word.getEMdFValueIndex("suffix_person"));
                if      (suffixPerson.equals("first_person"))  { suffixPerson = "1"; }
                else if (suffixPerson.equals("second_person")) { suffixPerson = "2"; }
                else if (suffixPerson.equals("third_person"))  { suffixPerson = "3"; }
                
                suffixGloss = suffixPerson + suffixGender + suffixNumber;
            }
        }
        
        if (psp.equals("verb"))
        {
            String tense = null;
            String stem  = null;
            
            if (generateGloss)
            {
                tense = word.getFeatureAsString(word.getEMdFValueIndex("tense"));
                if      (tense.equals("imperfect"))  { tense = "IMPF"; }
                else if (tense.equals("perfect"))    { tense = "PERF"; }
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
            
            convert(results, word, "graphical_preformative_utf8",
                tense, "V/TAM");
            
            // String stemNum = word.getEMdFValue("verbal_stem").toString();
            convert(results, word, "graphical_root_formation_utf8",
                stem, "V/STM");
            
            convert(results, word, "graphical_lexeme_utf8", 
                gloss, "V/NUC");
            
            convert(results, word, "graphical_verbal_ending_utf8",
                verbEnding, "AG/PSA");

            convert(results, word, "graphical_pron_suffix_utf8",
                suffixGloss, "PRON/DCA");
        }
        else if (psp.equals("noun")
            || psp.equals("proper_noun"))
        {
            String type = "N/NUC";
            
            if (psp.equals("proper_noun"))
            {
                type = "HEAD/NPROP";
            }
            
            convert(results, word, "graphical_lexeme_utf8", gloss, type);
            convert(results, word, "graphical_nominal_ending_utf8",
                nounEnding, "N/GNS");
            convert(results, word, "graphical_pron_suffix_utf8",
                suffixGloss, "N/POS");
        }
        else if (psp.equals("none"))
        {
            // hack for LexemeTest
            convert(results, word, "lexeme", "none", "none");
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

            boolean hasSuffix = !(suffixText.equals(""));
            convert(results, word, "graphical_lexeme_utf8", type, type);
            
            if (hasSuffix)
            {
                convert(results, word, "graphical_pron_suffix_utf8",
                    suffixGloss, type + "/SFX");
            }
        }   

        return results;
    }
    
    private void convert(List<Morpheme> results, MatchedObject word,
        String feature, String gloss, String symbol)
    throws EmdrosException
    {
        results.add(new Morpheme(word.getEMdFValue(feature).toString(),
            gloss, symbol));
    }
}

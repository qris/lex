package com.qwirx.lex.morph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jemdros.EmdrosException;
import jemdros.MatchedObject;

import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class HebrewMorphemeGenerator
{
    public static class Morpheme
    {
        private String m_Surface, m_Gloss, m_Symbol, m_Translit;
        private boolean m_IsLastMorpheme, m_DisplayWithEquals;
        
        public Morpheme(String surface, String gloss, String symbol,
            boolean isLastMorpheme, boolean displayWithEquals)
        {
            m_Surface = surface;
            m_Gloss = gloss;
            m_Symbol = symbol;
            m_IsLastMorpheme = isLastMorpheme;
            m_DisplayWithEquals = displayWithEquals;
        }
        
        public String getSurface() { return m_Surface; }
        public String getGloss() { return m_Gloss; }
        public String getNodeSymbol() { return m_Symbol; }
        public boolean isLastMorpheme() { return m_IsLastMorpheme; }
        public boolean isDisplayedWithEquals() { return m_DisplayWithEquals; }
        void setTranslit(String translit) { m_Translit = translit; }
        public String getTranslit() { return m_Translit; }
        public String getTranslitWithMorphemeMarkers()
        {
            if (m_Translit == null)
            {
                return null;
            }
            
            String translit = m_Translit;
            
            if (translit.equals(""))
            {
                translit = "Ø";
            }
            
            if (m_DisplayWithEquals)
            {
                return translit + "=";
            }
            else if (!m_IsLastMorpheme)
            {
                return translit + "-";
            }
            else
            {
                return translit;
            }
        }
    }
    
    public List<Morpheme> parse(MatchedObject word, boolean generateGloss,
        SqlDatabase sql, DatabaseTransliterator transliterator)
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
        
        return parse(word, generateGloss, gloss, transliterator);
    }

    private static final List<String> REQUIRED_FEATURES_BASE =
        Arrays.asList(new String[]{
            "phrase_dependent_part_of_speech",
            "graphical_preformative_utf8",
            "graphical_root_formation_utf8",
            "graphical_lexeme_utf8",
            "graphical_verbal_ending_utf8",
            "graphical_nominal_ending_utf8",
            "graphical_pron_suffix_utf8",
            "graphical_lexeme_wit",
            "wivu_alternate_gloss",
            "tense",
        });

    private static final List<String> REQUIRED_FEATURES_GLOSS =
        Arrays.asList(new String[]{
            "person",
            "gender",
            "number",
            "state",
            "wordnet_gloss",
            "lexeme_wit",
            "stem",
            "suffix_gender",
            "suffix_number",
            "suffix_person"
        });
    
    public List<String> getRequiredFeatures(boolean generateGloss)
    {
        List<String> requiredFeatures =
            new ArrayList<String>(REQUIRED_FEATURES_BASE);
        
        if (generateGloss)
        {
            requiredFeatures.addAll(REQUIRED_FEATURES_GLOSS);
        }

        return requiredFeatures;
    }
    
    public String getRequiredFeaturesString(boolean includeGloss)
    {
        StringBuffer out = new StringBuffer();
        List<String> features = getRequiredFeatures(includeGloss);
        for (String feature : features)
        {
            if (out.length() > 0)
            {
                out.append(", ");
            }
            
            out.append(feature);
        }
        
        return out.toString();
    }
    
    public List<Morpheme> parse(MatchedObject word, boolean generateGloss,
        String gloss, DatabaseTransliterator transliterator)
    throws DatabaseException
    {
        if (!word.getObjectTypeName().equals("word"))
        {
            throw new IllegalArgumentException("Can only parse words");
        }
        
        List<String> requiredFeatures = getRequiredFeatures(generateGloss);        
        Map<String, String> features = new HashMap<String, String>();
        
        for (Iterator<String> i = requiredFeatures.iterator(); i.hasNext();)
        {
            String feature = i.next();
            
            int index;
            String value;
            
            try
            {
                index = word.getEMdFValueIndex(feature);
                if (index == -1)
                {
                    throw new IllegalArgumentException("Word does not have " +
                            "required feature " + feature);
                }
                
                value = word.getFeatureAsString(index);
                if (value == null)
                {
                    throw new IllegalArgumentException("Word does not have " +
                            "required feature " + feature);
                }
            }
            catch (EmdrosException e)
            {
                throw new DatabaseException("Failed to retrieve feature '" +
                    feature + "' from word " + word.getID_D(), e);
            }
            
            features.put(feature, value);
        }
        
        String psp = features.get("phrase_dependent_part_of_speech");
       
        String verbEnding = null;
        String nounEnding = null;

        String suffixText = features.get("graphical_pron_suffix_utf8");
        String suffixGloss = null;
        
        List<Morpheme> results = new ArrayList<Morpheme>();
        
        if (generateGloss)
        {
            String person = features.get("person");
            if      (person.equals("first_person"))  person = "1";
            else if (person.equals("second_person")) person = "2";
            else if (person.equals("third_person"))  person = "3";
            else if (person.equals("unknown"))       person = "";
            
            String gender = features.get("gender");
            if      (gender.equals("masculine")) { gender = "M"; }
            else if (gender.equals("feminine"))  { gender = "F"; }
            else if (gender.equals("common"))    { gender = "="; }
            else if (gender.equals("unknown"))   { gender = "u"; }
            
            String number = features.get("number");
            if (number.equals("singular")) { number = "sg"; }
            else if (number.equals("plural")) { number = "pl"; }
            
            String state = features.get("state");
            if      (state.equals("construct")) { state = "CS"; }
            else if (state.equals("absolute"))  { state = "AB"; }
            
            verbEnding = person + gender + number;
            nounEnding = gender + number + state;
            
            if (suffixText.equals(""))
            {
                suffixGloss = "CLT";
            }
            else
            {
                String suffixGender = features.get("suffix_gender");
                if      (suffixGender.equals("masculine")) { suffixGender = "M"; }
                else if (suffixGender.equals("feminine"))  { suffixGender = "F"; }
                else if (suffixGender.equals("common"))    { suffixGender = "="; }

                String suffixNumber = features.get("suffix_number");
                if      (suffixNumber.equals("singular")) { suffixNumber = "sg"; }
                else if (suffixNumber.equals("plural"))   { suffixNumber = "pl"; }

                String suffixPerson = features.get("suffix_person");
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
                tense = features.get("tense");
                if      (tense.equals("imperfect"))  { tense = "IMPF"; }
                else if (tense.equals("perfect"))    { tense = "PERF"; }
                else if (tense.equals("imperative")) { tense = "IMP"; }
                else if (tense.equals("infinitive_construct")) { tense = "INF"; }
                else if (tense.equals("infinitive_absolute"))  { tense = "EMPH"; }
                else if (tense.equals("participle")) { tense = "PART"; }
                else if (tense.equals("wayyiqtol"))  { tense = "NARR"; }

                stem = features.get("stem");
                if      (stem.equals("qal"))     { stem = "Qa"; }
                else if (stem.equals("piel"))    { stem = "Pi"; }
                else if (stem.equals("hifil"))   { stem = "Hi"; } 
                else if (stem.equals("nifal"))   { stem = "Ni"; }
                else if (stem.equals("pual"))    { stem = "Pu"; }
                else if (stem.equals("hitpael")) { stem = "Hit"; }
                else if (stem.equals("hofal"))   { stem = "Ho"; }
            }
            
            convert(results, features, "graphical_preformative_utf8",
                tense, "V/TAM", false, false);
            
            // String stemNum = word.getEMdFValue("verbal_stem").toString();
            convert(results, features, "graphical_root_formation_utf8",
                stem, "V/STM", false, false);
            
            // Requested by Nicolai: if the verb's tense is "participle",
            // the graphical_nominal_ending_utf8 goes onto the verbal ending,
            // otherwise it goes onto the end of the lexeme.
            
            if (features.get("tense").equals("participle"))
            {
                convert(results, features, "graphical_lexeme_utf8",
                    gloss, "V/NUC", false, false);

                results.add(new Morpheme(
                    features.get("graphical_verbal_ending_utf8") +
                    features.get("graphical_nominal_ending_utf8"),
                    verbEnding, "AG/PSA", false, true));
            }
            else
            {
                results.add(new Morpheme(
                    features.get("graphical_lexeme_utf8") +
                    features.get("graphical_nominal_ending_utf8"),
                    gloss, "V/NUC", false, false));
    
                convert(results, features, "graphical_verbal_ending_utf8",
                    verbEnding, "AG/PSA", false, true);
            }
            
            convert(results, features, "graphical_pron_suffix_utf8",
                suffixGloss, "PRON/DCA", true, false);
        }
        else if (psp.equals("noun")
            || psp.equals("proper_noun"))
        {
            String type = "N/NUC";
            
            if (psp.equals("proper_noun"))
            {
                type = "HEAD/NPROP";
            }
            
            convert(results, features, "graphical_lexeme_utf8", gloss, type,
                false, false);
            convert(results, features, "graphical_nominal_ending_utf8",
                nounEnding, "N/GNS", false, true);
            convert(results, features, "graphical_pron_suffix_utf8",
                suffixGloss, "N/POS", true, false);
        }
        else if (psp.equals("none"))
        {
            // hack for LexemeTest
            convert(results, features, "lexeme", "none", "none", true, false);
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
            
            // horrible hack to save us from starting a research project
            // into when articles and prepositions are joined to the next word
            // and when they are separated.
            boolean isGraphicalWordEnding =
                !features.get("graphical_lexeme_wit").endsWith("-");

            boolean hasSuffix = !(suffixText.equals(""));
            convert(results, features, "graphical_lexeme_utf8", type, type,
                !hasSuffix, !isGraphicalWordEnding && !hasSuffix);
            
            if (hasSuffix)
            {
                convert(results, features, "graphical_pron_suffix_utf8",
                    suffixGloss, type + "/SFX", true, !isGraphicalWordEnding);
            }
        }

        if (transliterator != null)
        {
            // Transliteration rules may require access to surface text in
            // previous and subsequent morphemes, so transliteration can only
            // be done once the entire morpheme list has been generated.
    
            for (int i = 0; i < results.size(); i++)
            {
                String translit = transliterator.transliterate(results, i,
                    word);                
                results.get(i).setTranslit(translit);
            }
        }

        return results;
    }
    
    private void convert(List<Morpheme> results, Map<String, String> features,
        String feature, String gloss, String symbol,
        boolean isLastMorpheme, boolean isGraphicalWordEnd)
    {
        results.add(new Morpheme(features.get(feature), gloss, symbol,
            isLastMorpheme, isGraphicalWordEnd));
    }
}

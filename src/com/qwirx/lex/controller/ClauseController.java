package com.qwirx.lex.controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import jemdros.EMdFValue;
import jemdros.MatchedObject;
import jemdros.SheafConstIterator;

import org.aptivate.webutils.EditField;
import org.aptivate.webutils.EditField.SelectBox;
import org.crosswire.jsword.book.BookData;
import org.crosswire.jsword.book.OSISUtil;

import com.qwirx.crosswire.kjv.KJV;
import com.qwirx.db.Change;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.Lex;
import com.qwirx.lex.TableRenderer;
import com.qwirx.lex.emdros.EmdrosChange;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.hebrew.WivuLexicon.Entry;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;

public class ClauseController extends ControllerBase
{
    private BookData m_SwordVerse;
    private int m_NumMacroroles;
    private String m_LogicalStructure = "", m_LinkedLogicalStructure;
    private Map<String, MatchedObject> m_VariableToPhraseMap;
    private Map<Integer, String> m_PhraseIdToVariableMap;
    private int m_SelectedLogicalStructureId; 
    private Map<String, String> m_StemForms;

    private static final String WIVU_SAVE_PARAM = "wivu_save",
        WIVU_WORD_PARAM = "wivu_word", WIVU_INDEX_PARAM = "wivu_index";
    
    public static class BorderTableRenderer extends TableRenderer
    {
        public String getTable(String contents)
        {
            return "<table class=\"tree\" border>" + contents + "</table>\n";
        }
    }

    private int getParamInt(String name)
    {
        String valueString = m_Request.getParameter(name);
        return Integer.parseInt(valueString);
    }

    private int getParamInt(String name, int defaultValue)
    {
        String valueString = m_Request.getParameter(name);
        
        if (valueString == null)
        {
            return defaultValue;
        }
        else
        {
            return Integer.parseInt(valueString);
        }
    }
    
    /**
     * For use by unit tests only!
     * @param request
     * @param emdros
     * @param sql
     * @param navigator
     * @throws Exception
     */
    public ClauseController(EmdrosDatabase emdros, SqlDatabase sql,
        int clauseId)
    throws Exception
    {
        super(emdros, sql);
        loadClause(emdros.getMinM(), emdros.getMaxM(), clauseId);
    }

    public ClauseController(HttpServletRequest request, EmdrosDatabase emdros,
        SqlDatabase sql, Navigator navigator)
    throws Exception
    {
        super(request, emdros, sql, navigator);
        
        m_SwordVerse = KJV.getVerse(emdros, navigator);

        if (request.getParameter("savearg") != null)
        {
            int phraseId  = getParamInt("phraseid");
            String newArg = request.getParameter("newarg");
            Change ch = emdros.createChange(EmdrosChange.UPDATE,
                "phrase", new int[]{phraseId});
            ch.setString("argument_name", newArg);
            ch.execute();
        }
        
        if (request.getParameter("changemr") != null &&
            request.getParameter("mr") != null) 
        {
            int phraseId          = getParamInt("pid");
            int newMR             = getParamInt("mr");
            Change ch = emdros.createChange(EmdrosChange.UPDATE,
                "phrase", new int[]{phraseId});
            ch.setInt("macrorole_number", newMR);
            ch.execute();
        }
        
        if (m_Request.getParameter(WIVU_SAVE_PARAM) != null)
        {
            int wivuWord = getParamInt(WIVU_WORD_PARAM);
            int wivuIndex = getParamInt(WIVU_INDEX_PARAM);
            Change ch = m_Emdros.createChange(EmdrosChange.UPDATE, "word",
                new int[]{wivuWord});
            ch.setInt("wivu_lexicon_id", wivuIndex);
            ch.execute();
        }

        loadClause();

        m_SelectedLogicalStructureId = 
            m_Clause.getEMdFValue("logical_struct_id").getInt();
        
        {   
            String newLsString = request.getParameter("newls");
            boolean saveLs = (request.getParameter("lssave") != null);
        
            if (request.getParameter("create") != null && newLsString != null) 
            {
                Change ch = sql.createChange(SqlChange.INSERT,
                    "lexicon_entries", null);
                ch.setString("Lexeme",    getPredicateText());
                ch.setString("Structure", newLsString);
                ch.execute();
                m_SelectedLogicalStructureId = ((SqlChange)ch).getInsertedRowId();
                saveLs = true;
            } 
            else 
            {       
                String selLsIdString = request.getParameter("lsid");
                try
                {
                    m_SelectedLogicalStructureId = Integer.parseInt(selLsIdString);
                }
                catch (Exception e)
                {
                    /* do nothing, use default */
                }
            }
        
            if (saveLs) 
            {
                Change ch = emdros.createChange(EmdrosChange.UPDATE,
                    "clause", new int[]{navigator.getClauseId()});
                ch.setInt("logical_struct_id", m_SelectedLogicalStructureId);
                ch.execute();
            }
        }

        PreparedStatement stmt = sql.prepareSelect(
            "SELECT ID,Structure,Syntactic_Args " +
            "FROM lexicon_entries WHERE ID = ?");
        stmt.setInt(1, m_SelectedLogicalStructureId);
        
        ResultSet rs = sql.select();

        if (rs.next()) 
        {
            int    thisLsId      = rs.getInt("ID");
            String thisStructure = rs.getString("Structure");
            int    thisNumSMRs   = rs.getInt("Syntactic_Args");
            
            if (thisStructure != null)
            {
                m_LogicalStructure = thisStructure;
            }
            
            m_NumMacroroles = thisNumSMRs;
        }
        
        sql.finish();
        
        if (request.getParameter("nc") != null &&
            request.getParameter("nt") != null)
        {
            String newNoteText = request.getParameter("nt");
            
            EmdrosChange ch = (EmdrosChange)(
                m_Emdros.createChange(EmdrosChange.CREATE,
                    "note", new int[]{navigator.getClauseId()}));
            ch.setString("text", newNoteText);
            ch.execute();
        }
    
        if (request.getParameter("nu") != null &&
            request.getParameter("ni") != null &&
            request.getParameter("nt") != null)
        {
            String updateNoteIdString = request.getParameter("ni");
            int updateNoteId = Integer.parseInt(updateNoteIdString);
            String newNoteText = request.getParameter("nt");
            
            EmdrosChange ch = (EmdrosChange)(
                emdros.createChange(EmdrosChange.UPDATE,
                    "note", new int [] {updateNoteId}));
            ch.setString("text", newNoteText);
            ch.execute();
        }

        if (request.getParameter("nd") != null &&
            request.getParameter("ni") != null)
        {
            String deleteNoteIdString = request.getParameter("ni");
            int deleteNoteId = Integer.parseInt(deleteNoteIdString);
            
            EmdrosChange ch = (EmdrosChange)(
                emdros.createChange(EmdrosChange.DELETE,
                    "note", new int[]{deleteNoteId}));
            ch.execute();
        }
        
        if (m_Emdros.canWriteTo(m_Clause))
        {
            if (m_Request.getParameter("publish") != null)
            {
                Change ch = m_Sql.createChange(SqlChange.INSERT,
                    "user_text_access", null);
                ch.setInt("Monad_First", m_Clause.getMonads().first());
                ch.setInt("Monad_Last",  m_Clause.getMonads().last());
                ch.setString("User_Name",   "anonymous");
                ch.setInt("Write_Access", 0);
                ch.execute();

                ch = m_Emdros.createChange(EmdrosChange.UPDATE, "clause", 
                    new int[]{m_Clause.getID_D()});
                ch.setInt("published", 1);
                ch.setString("predicate", getPredicateText());
                ch.execute();
            }
            else if (request.getParameter("unpublish") != null)
            {
                Change ch = sql.createChange(SqlChange.DELETE,
                    "user_text_access",
                    "Monad_First = " + m_Clause.getMonads().first() + " AND " +
                    "Monad_Last  = " + m_Clause.getMonads().last()  + " AND " +
                    "User_Name   = \"anonymous\"");
                ch.execute();
    
                ch = emdros.createChange(EmdrosChange.UPDATE, "clause", 
                    new int[]{m_Clause.getID_D()});
                ch.setInt("published", 0);
                ch.execute();
            }
        }
        
        List<String> arguments = getArgumentNames();
        m_VariableToPhraseMap = new HashMap<String, MatchedObject>();
        m_PhraseIdToVariableMap = new HashMap<Integer, String>();
        
        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();
        while (phrases.hasNext())
        {
            MatchedObject phrase = phrases.next().const_iterator().next();

            String pf = phrase.getEMdFValue("phrase_function").toString();
            String function_name = m_PhraseFunctions.get(pf);

            String pt = phrase.getEMdFValue("phrase_type").toString();
            String phrase_type = m_PhraseTypes.get(pt);

            if (phrase_type.equals("NP") ||
                phrase_type.equals("IrPronNP") ||
                phrase_type.equals("PersPronNP") ||
                phrase_type.equals("DemPronNP") ||
                phrase_type.equals("PropNP") ||
                phrase_type.equals("PP"))
            {
                String oldArg = phrase.getEMdFValue("argument_name").toString();
                String newArg = "";
                
                if (oldArg.equals("") || !arguments.contains(oldArg))
                {
                    // Argument not decided yet. Maybe we can guess
                    // based on the "function" of the clause?
                    newArg = getDefaultVariableName(function_name);
                    
                    if (newArg != null && !arguments.contains(newArg))
                    {
                        newArg = null;
                    }
                    
                    if (newArg == null)
                    {
                        newArg = "";
                    }
                    else
                    {
                        oldArg = newArg;
                    }
                }
                
                if (oldArg != null && !oldArg.equals(""))
                {
                    if (m_VariableToPhraseMap.get(oldArg) == null)
                    {
                        m_VariableToPhraseMap.put(oldArg, phrase);
                    }
                    
                    m_PhraseIdToVariableMap.put(
                        Integer.valueOf(phrase.getID_D()), oldArg);
                }
            }
        }
                
        m_LinkedLogicalStructure = m_LogicalStructure;
        
        for (String variable : m_VariableToPhraseMap.keySet())
        {
            MatchedObject value = m_VariableToPhraseMap.get(variable);
            String value_text = "";
            SheafConstIterator sci = value.getSheaf().const_iterator();
            
            while (sci.hasNext())
            {
                MatchedObject word = sci.next().const_iterator().next();
                value_text += HebrewConverter.wordTranslitToHtml(word,
                    new HebrewMorphemeGenerator(), m_Transliterator);
                if (sci.hasNext())
                {
                    value_text += " ";
                }
            }
            
            m_LinkedLogicalStructure = m_LinkedLogicalStructure.replaceAll
                ("<" + variable + ">", value_text);
        }
        
        m_LinkedLogicalStructure = m_LinkedLogicalStructure
            .replaceAll("<","&lt;").replaceAll(">","&gt;");

        String currentStruct = m_Clause.getEMdFValue("logical_structure")
            .getString();
        if (! currentStruct.equals(m_LinkedLogicalStructure) &&
            emdros.canWriteTo(m_Clause))
        {
            Change ch = emdros.createChange(EmdrosChange.UPDATE,
                "clause", new int[]{navigator.getClauseId()});
            ch.setString("logical_structure", m_LinkedLogicalStructure);
            ch.execute();
        }
    }
    
    private final Pattern varPat = Pattern.compile("(?s)(?i)<([^>]*)>");
    
    private Matcher getVariableMatcher()
    {
        return varPat.matcher(m_LogicalStructure);
    }
    
    private List<String> getArgumentNames()
    {
        Set<String> resultsSet = new HashSet<String>();
        List<String> results = new ArrayList<String>();
        
        Matcher m = getVariableMatcher();
        while (m.find()) 
        {
            String name = m.group(1);
            if (!resultsSet.contains(name))
            {
                resultsSet.add(name);
                results.add(name);
            }
        }
        
        return results;
    }

    public String getDefaultVariableName(String phrase_function)
    {
        if (phrase_function.equals("Subj") ||
            phrase_function.equals("PreS") ||
            phrase_function.equals("IrpS") ||
            phrase_function.equals("ModS"))
        {
            return "x";
        }
        else if (phrase_function.equals("Objc") ||
            phrase_function.equals("PreC") ||
            phrase_function.equals("PreO") ||
            phrase_function.equals("PtcO") ||
            phrase_function.equals("IrpO"))
        {
            return "y";
        }

        return null;
    }
    
    class Cell
    {
        String label, link, format, html;
        int columns;
        List<Cell> subcells = new ArrayList<Cell>();
    }

    private Cell getLexiconGlossCell(MatchedObject word)
    throws Exception
    {
        int wid = word.getID_D();

        // "ewg" stands for "edit word gloss"
        String ewgString = m_Request.getParameter("ewg");
        int ewgId = -1;
        if (ewgString != null)
        {
            ewgId = Integer.parseInt(ewgString);
        }

        Cell cell = new Cell();

        Lexeme lexeme = Lexeme.findOrBuild(m_Sql, word);

        if (ewgId == wid &&
            m_Request.getParameter("ewgs") != null) 
        {
            lexeme.setGloss(m_Request.getParameter("gloss"));
            lexeme.save();
            lexeme = Lexeme.load(m_Sql, word);
            ewgId = -1;
        }
        
        String lexiconGloss = lexeme.getGloss();
        
        if (ewgId == wid) 
        {
            if (lexiconGloss == null)
            {
                lexiconGloss = "";
            }
            cell.html = "<form method=\"post\">\n" +
                "<input type=\"hidden\" name=\"ewg\"" +
                " value=\"" + wid + "\">\n" +
                "<input name=\"gloss\" size=\"10\" value=\"" +
                HebrewConverter.toHtml(lexiconGloss) +
                "\">\n" +
                "<input type=\"submit\" name=\"ewgs\""+
                " value=\"Save\">\n" +
                "</form>";
        } 
        else 
        {
            if (lexiconGloss == null)
            {
                lexiconGloss = "(gloss)";
            }
            cell.html = "<a href=\"clause.jsp?ewg=" + 
                wid + "\">" + lexiconGloss + "</a>";
        }
        
        return cell;
    }

    private String getVerbForm(MatchedObject word)
    throws Exception
    {
        String verbForm = "";
        
        if (m_StemForms == null)
        {
            m_StemForms = m_Emdros.getEnumerationConstants("stem_e",
                false);
        }
        
        String part_of_speech = word.getFeatureAsString(
            word.getEMdFValueIndex("phrase_dependent_part_of_speech"));

        
        if (part_of_speech.equals("verb"))
        {
            verbForm = m_StemForms.get(word.getEMdFValue("stem").toString());
        }
   
        return verbForm;
    }
    
    private String getWivuGloss(MatchedObject word)
    throws Exception
    {
        int wivuIndex = 0;
        EMdFValue wivuIndexE = word.getEMdFValue("wivu_lexicon_id");

        if (wivuIndexE != null)
        {
            wivuIndex = wivuIndexE.getInt();
        }

        Entry [] entries = Lex.getWivuLexicon().getEntry(
            word.getEMdFValue("lexeme_wit").toString(),
            getVerbForm(word));

        if (entries == null || entries.length == 0)
        {
            return null;
        }
        
        if (wivuIndex > entries.length)
        {
            wivuIndex = 0; 
        }
        
        return entries[wivuIndex].getGloss();
    }
    
    private Cell getWivuLexiconCell(MatchedObject word, String part_of_speech,
        boolean canWrite)
    throws Exception
    {        
        int wivuIndex = 0;
        EMdFValue wivuIndexE = word.getEMdFValue("wivu_lexicon_id");
        if (wivuIndexE != null)
        {
            wivuIndex = wivuIndexE.getInt();
        }
        
        Cell cell = new Cell();
        cell.html = "[Wivu] ";
        
        Entry [] entries = Lex.getWivuLexicon().getEntry(
            word.getEMdFValue("lexeme_wit").toString(),
            getVerbForm(word));
        List<String[]> meanings = new ArrayList<String[]>();
        
        if (entries == null)
        {
            cell.html += "not found";
        }
        else if (entries.length == 1)
        {
            cell.html += EditField.escapeEntities(entries[0].getGloss());
        }
        else if (!canWrite)
        {
            // just list all possible meanings
            if (wivuIndex < entries.length)
            {
                cell.html += EditField.escapeEntities(entries[wivuIndex].getGloss());
            }
            
            boolean first = true;
            
            for (int i = 0; i < entries.length; i++)
            {
                if (i != wivuIndex)
                {
                    if (first)
                    {
                        cell.html += " (alternatives: ";
                    }
                    else
                    {
                        cell.html += ". ";
                    }
                    
                    cell.html += EditField.escapeEntities(entries[i].getGloss());
                    first = false;
                }
            }
            
            if (!first)
            {
                cell.html += ")";
            }
            
            /*
            cell.html += "<ul class=\"mini\">\n";
            
            for (int i = 0; i < entries.length; i++)
            {
                cell.html += "<li>";

                if (i == wivuIndex)
                {
                    cell.html += "<strong>";
                }
                
                cell.html += EditField.escapeEntities(entries[i].getGloss());

                if (i == wivuIndex)
                {
                    cell.html += "</strong>";
                }

                cell.html += "</li>\n";
            }
            cell.html += "</ul>";
            */            
        }
        else
        {
            for (int i = 0; i < entries.length; i++)
            {
                meanings.add(new String[]{Integer.toString(i),
                    entries[i].getGloss()});
            }
            
            EditField form = new EditField(m_Request);
            cell.html += "<form method=\"post\">\n";
            cell.html += form.hidden(WIVU_WORD_PARAM, word.getID_D());
            SelectBox sel = form.select(WIVU_INDEX_PARAM, meanings);
            sel.setDefaultValue(wivuIndex);
            cell.html += sel.toString();
            cell.html += form.submit(WIVU_SAVE_PARAM, "Save");
            cell.html += "</form>";
        }
        
        return cell;
    }
    
    private Cell getDibLookupCell(MatchedObject word)
    throws Exception
    {
        Cell cell = new Cell();
        
        String gloss = KJV.getDibGloss(word.getEMdFValue("lexeme_wit")
            .getString());
        
        if (gloss == null)
        {
            cell.html = "";
        }
        else
        {
            cell.html = "[DiB] " + gloss;
        }
        
        return cell;
    }
    
    public Cell getKingJamesGloss(MatchedObject word)
    throws Exception
    {
        // Hebrew-English Dictionary lookup
        Cell cell = new Cell();
        cell.html = "";
        
        if (m_SwordVerse != null)
        {

            String gloss = KJV.getKingJamesGloss(m_SwordVerse,
                word.getEMdFValue("lexeme_wit").getString());
            
            if (gloss == null)
            {
                cell.html = "";
            }
            else
            {
                cell.html = "[KJV] " + gloss;
            }   
        }
         
        return cell;
    }
    
    private Cell getMacroroleCell(MatchedObject phrase, String phrase_type,
        boolean canWriteToPhrase)
    throws Exception
    {
        Cell cell = new Cell();
        cell.html = "";

        if (phrase_type.equals("VP"))
        {
            String mrHtml = "Macroroles: ";
            
            switch (m_NumMacroroles)
            {
                case -1: mrHtml += "MR? (unknown)"; break;
                case 0:  mrHtml += "MR0"; break;
                case 1:  mrHtml += "MR1"; break;
                case 2:  mrHtml += "MR2"; break;
                case 3:  mrHtml += "MR3"; break;
                default: mrHtml += "MR! (invalid)"; break;
            }
            
            cell.html = mrHtml;
        }
        else if (phrase_type.equals("NP") ||
                 phrase_type.equals("IrPronNP") ||
                 phrase_type.equals("PersPronNP") ||
                 phrase_type.equals("DemPronNP") ||
                 phrase_type.equals("PropNP") ||
                 phrase_type.equals("PP"))
        {
            int oldMR = phrase.getEMdFValue("macrorole_number").getInt();
            String formName = "mr_" + phrase.getID_D();
                
            StringBuffer mrHtml = new StringBuffer();
            mrHtml.append("<form name=\"" + formName + "\" " +
                "method=\"POST\">\n");
            mrHtml.append("<input type=\"hidden\" name=\"pid\" " +
                "value=\"" + phrase.getID_D() + "\">\n");
            mrHtml.append("<input type=\"hidden\" name=\"prev\" " +
                "value=\"" + oldMR + "\">\n");
            mrHtml.append("<select name=\"mr\" " +
                "onChange=\"return enableChangeButton(" +
                "changemr, "+oldMR+", mr)\">\n");
            mrHtml.append("<option "+((oldMR==-1)?"SELECTED":"")+
                " value=\"-1\">Unknown\n");
            mrHtml.append("<option "+((oldMR==0) ?"SELECTED":"")+
                " value=\"0\">None\n");
            mrHtml.append("<option "+((oldMR==1) ?"SELECTED":"")+
                " value=\"1\">1 (Actor)\n");
            mrHtml.append("<option "+((oldMR==2) ?"SELECTED":"")+
                " value=\"2\">2 (Undergoer)\n");
            mrHtml.append("</select>\n");
            
            if (canWriteToPhrase)
            {
                mrHtml.append("<input type=\"submit\" "+
                    "name=\"changemr\" value=\"Change\">\n");
            }
            
            mrHtml.append("</form>\n");

            mrHtml.append("<script type=\"text/javascript\"><!--\n");
            mrHtml.append("\tenableChangeButton(" +
                "document.forms."+formName+".changemr, "+
                oldMR+", "+
                "document.forms."+formName+".mr)\n");
            mrHtml.append("//--></script>\n");

            cell.html = mrHtml.toString();
        }

        return cell;
    }
    
    public String getWordTable()
    throws Exception
    {
        StringBuffer html = new StringBuffer();
        HebrewMorphemeGenerator morphemes = new HebrewMorphemeGenerator();
        Map<String, String> stemForms = m_Emdros.getEnumerationConstants("stem_e",
            false);
        
        String [] object_types = new String [] {
            "word",
            "phrase"
        };
        
        List<String> arguments = getArgumentNames();
        
        for (int objectNum = 0; objectNum < object_types.length; 
            objectNum++) 
        {
            String type = object_types[objectNum];
            List<Cell> word_row = new ArrayList<Cell>(),
                struct_row = new ArrayList<Cell>();
            int column = 0;
            
            SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();
            while (phrases.hasNext())
            {
                MatchedObject phrase = phrases.next().const_iterator().next();
                int first_col = column;
                boolean canWriteToPhrase = m_Emdros.canWriteTo(phrase);
    
                String pf = phrase.getEMdFValue("phrase_function").toString();
                String function_name = m_PhraseFunctions.get(pf);

                String pt = phrase.getEMdFValue("phrase_type").toString();
                String phrase_type = m_PhraseTypes.get(pt);
                
                SheafConstIterator words = phrase.getSheaf().const_iterator();
                while (words.hasNext())
                {
                    MatchedObject word = words.next().const_iterator().next();
                    column++;
                    
                    if (type.equals("word"))
                    {
                        String part_of_speech = word.getFeatureAsString(
                            word.getEMdFValueIndex(
                                "phrase_dependent_part_of_speech"));
                            
                        Cell cell = new Cell();
                        cell.label = HebrewConverter.wordTranslitToHtml(word,
                            morphemes, m_Transliterator);
                        
                        cell.columns = 1;
                        word_row.add(cell);
                        
                        cell.subcells.add(getLexiconGlossCell(word));
                        cell.subcells.add(getWivuLexiconCell(word,
                            part_of_speech, canWriteToPhrase));
                        cell.subcells.add(getDibLookupCell(word));
                        cell.subcells.add(getKingJamesGloss(word));
                    }
                }
                
                if (type.equals("phrase")) 
                {
                    Cell pCell    = new Cell();
                    pCell.label   = phrase.getEMdFValue("phrase_function").toString();
                    pCell.columns = column - first_col;
                    struct_row.add(pCell);

                    if (function_name != null)
                        pCell.label = phrase_type + " (" + function_name + ")";
                    
                    if (phrase_type == null)
                        continue;

                    pCell.subcells.add(getMacroroleCell(phrase, phrase_type,
                        canWriteToPhrase));
                        
                    if (! phrase_type.equals("NP") &&
                        ! phrase_type.equals("IrPronNP") &&
                        ! phrase_type.equals("PersPronNP") &&
                        ! phrase_type.equals("DemPronNP") &&
                        ! phrase_type.equals("PropNP") &&
                        ! phrase_type.equals("PP"))
                        continue;

                    Cell varCell = new Cell();
                    pCell.subcells.add(varCell);

                    StringBuffer editHtml = new StringBuffer();
                    String variable = m_PhraseIdToVariableMap.get(
                        Integer.valueOf(phrase.getID_D()));
                    
                    int phraseId = phrase.getID_D();
                    if (variable == null)
                    {
                        variable = "";
                    }
                    else if (m_VariableToPhraseMap.get(variable).getID_D() !=
                        phraseId) 
                    {
                        editHtml.append("<font color=\"red\">" +
                            "Duplicate variable!</font>");
                    } 
                    
                    String formName = "sv_" + phrase.getID_D();

                    editHtml.append(
                        "<form method=\"post\" name=\""+formName+"\">\n" +
                        "<input type=\"hidden\" name=\"phraseid\"" +
                        " value=\"" + phrase.getID_D() + "\">\n" +
                        "<select name=\"newarg\" onChange=\"return " +
                        "enableChangeButton(savearg,'"+variable+"',newarg)" +
                        "\">\n" +
                        "<option value=\"\" " +
                        (variable.equals("") ? " SELECTED" : "") +
                        ">Auto\n" +
                        "<option value=\" \" " +
                        (variable.equals(" ") ? " SELECTED" : "") +
                        ">None\n");

                    for (String arg : arguments)
                    {
                        editHtml.append("<option value=\""+arg+
                            "\""+(variable.equals(arg)?" SELECTED":"")+
                            ">"+arg+"\n");
                    }

                    editHtml.append("</select>\n");
                    
                    if (canWriteToPhrase)
                    {
                        editHtml.append
                        (
                            "<input type=\"submit\" name=\"savearg\" "+
                            "value=\"Change\">\n"
                        );
                    }
                    
                    editHtml.append("</form>\n");

                    editHtml.append("<script type=\"text/javascript\"><!--\n" +
                        "enableChangeButton(" +
                        "document.forms."+formName+".savearg,\""+variable+"\"," +
                        "document.forms."+formName+".newarg)\n" +
                        "//--></script>\n");
                                                
                    varCell.html = editHtml.toString();

                    /*
                    Cell typeCell = new Cell();
                    pCell.subcells.add(typeCell);
                    editHtml = new StringBuffer();

                    int oldType = phrase.getEMdFValue("type_id")
                        .getInt();
                        
                    editHtml.append("<form method=\"post\">\n" +
                        "<input type=\"hidden\" name=\"phraseid\"" +
                        " value=\"" + phrase.getID_D() + "\">\n" +
                        "<select name=\"newtype\">\n" +
                        "<option value=\"\" " +
                        (oldType == 0 ? " SELECTED" : "") +
                        ">None\n");

                    for (int j = 0; j < types.length; j++) {
                        DataType t = types[j];
                        editHtml.append("<option value=\""+t.id+
                            "\""+(oldType == t.id ? " SELECTED" : "")+
                            ">");
                        while (t.depth-- > 0) {
                            editHtml.append("&nbsp;");
                        }
                        editHtml.append(t.name+"\n");
                    }

                    editHtml.append("</select>\n" +
                        "<input type=\"submit\" name=\"savetype\" "+
                        "value=\"Save\">\n"+
                        "</form>\n");
                    
                    typeCell.html = editHtml.toString();
                    */
                }
            }

            
            List<List<Cell>> bigRows = new ArrayList<List<Cell>>();
            
            if (word_row.size() > 0)
            {
                bigRows.add(word_row);
            }
            
            if (struct_row.size() > 0)
            {
                bigRows.add(struct_row);
            }

            Cell filler = new Cell();
            filler.label = "";

            for (int nBigRow = 0; nBigRow < bigRows.size(); nBigRow++)
            {
                List<Cell> bigRow = bigRows.get(nBigRow);
                int numColumns = bigRow.size();
                int littleRowsThisBigRow = 0;
                
                for (Cell cell : bigRow) 
                {
                    int reqRows = cell.subcells.size();
                    if (reqRows > littleRowsThisBigRow) 
                    {
                        littleRowsThisBigRow = reqRows;
                    }
                }
                
                // don't forget to count the top cell as well!
                littleRowsThisBigRow++;
                
                for (int r = 0; r < littleRowsThisBigRow; r++)
                {
                    html.append("<tr>\n");
                    
                    if (r == 0)
                    {
                        html.append("<th rowspan=\"" + littleRowsThisBigRow +
                            "\">");
                        html.append(type);
                        html.append("</th>\n");
                    }

                    for (Cell topCell : bigRow)
                    {
                        Cell thisCell = topCell;
                        
                        if (r > 0)
                        {
                            if (topCell.subcells != null && 
                                r <= topCell.subcells.size()) 
                            {
                                thisCell = topCell.subcells.get(r - 1);
                            }
                            else
                            {
                                thisCell = filler;
                            }
                        }

                        html.append("<td colspan=\"" + topCell.columns + "\">");

                        if (thisCell.html != null)
                        {
                            html.append(thisCell.html);
                        }
                        else
                        {
                            String cellHtml = thisCell.label
                                .replaceAll("<", "&lt;")
                                .replaceAll(">", "&gt;");
                            
                            if (thisCell.link != null)
                            {
                                html.append("<a href=\"" + thisCell.link +
                                    "\">");
                            }

                            html.append(cellHtml);

                            if (thisCell.link != null)
                            {
                                html.append("</a>");
                            }
                        }

                        html.append("</td>\n");
                    }
    
                    html.append("</tr>\n");
                }
            }
        }

        return html.toString();
    }
    
    public boolean isPublished(MatchedObject mo)
    throws DatabaseException
    {
        return m_Sql.getSingleInteger("SELECT COUNT(1) " +
            "FROM user_text_access " +
            "WHERE Monad_First = " + mo.getMonads().first() +
            " AND  Monad_Last  = " + mo.getMonads().last()  + 
            " AND  User_Name   = \"anonymous\"") > 0;
    }

    public List<String[]> getWordColumns()
    throws Exception
    {
        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();
        List<String[]> columns = new ArrayList<String[]>();
        boolean isFirstWord = true;
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase = phrases.next().const_iterator().next();
            SheafConstIterator words = phrase.getSheaf().const_iterator();
            
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();

                String wivuGloss = getWivuGloss(word);
                if (wivuGloss != null)
                {
                    wivuGloss = wivuGloss.replaceFirst("[;,].*", "");
                    wivuGloss = wivuGloss.replaceAll("[<>]", "");
                    wivuGloss = wivuGloss.replaceAll(" ", ".");
                }
                
                List<Morpheme> morphemes = generator.parse(word, true, wivuGloss);
                
                for (int i = 0; i < morphemes.size(); i++)
                {
                    Morpheme morpheme = morphemes.get(i);

                    String translit = m_Transliterator.transliterate(morphemes,
                        i, word);
                    translit = HebrewConverter.toHtml(translit);
                    if (translit.equals("")) translit = "&Oslash;";
                    
                    String gloss = morpheme.getGloss();
                    if (gloss == null)
                    {
                        gloss = "";
                    }
                    else if (gloss.equals("CONJ"))
                    {
                        if (isFirstWord)
                        {
                            gloss = "CLM";
                        }
                        else
                        {
                            gloss = "CR";
                        }
                    }
                    else if (gloss.equals("PERF"))
                    {
                        if (columns.size() >= 2 &&
                            columns.get(columns.size() - 2)[1].equals("CLM"))
                        {
                            gloss = "SER2";
                        }
                    }
                    
                    // desc += ":" + lastMorpheme + ":" + m_IsLastWord;
                    
                    boolean lastMorpheme = (i == morphemes.size() - 1);
                    
                    if (!lastMorpheme)
                    {
                        translit += "-";
                        if (!gloss.equals(""))
                        {
                            gloss += "-";
                        }
                    }
                    else if (translit.endsWith("-"))
                    {
                        gloss += "-";
                        lastMorpheme = false;
                    }

                    columns.add(new String[]{translit, gloss});
                    
                    if (lastMorpheme && (phrases.hasNext() || words.hasNext()))
                    {
                        // blank cell between words
                        columns.add(new String[]{"",""});
                    }
                }
                
                isFirstWord = false;
            }
        }
        
        return columns;
    }

    public String getTableRows(List<String[]> wordColumns)
    {
        StringBuffer html = new StringBuffer();
        
        for (int row = 0; row < 2; row++)
        {
            html.append("\t<tr>\n");
            for (String [] rows : wordColumns) 
            {
                html.append("\t\t<td>");
                html.append(rows[row]);
                html.append("</td>\n");
            }
            html.append("\t</tr>\n");
        }
        
        return html.toString();
    }
    
    public String getGlossTable()
    throws Exception
    {
        return getTableRows(getWordColumns());
    }

    public String getPredicateText()
    throws Exception
    {
        SheafConstIterator phrases = m_Clause.getSheaf().const_iterator();

        while (phrases.hasNext()) 
        {
            MatchedObject phrase = phrases.next().const_iterator().next();
            SheafConstIterator words = phrase.getSheaf().const_iterator();
            
            while (words.hasNext()) 
            {
                MatchedObject word = words.next().const_iterator().next();
                String pspv = word.getEMdFValue("phrase_dependent_" +
                        "part_of_speech").toString();
                String psp = m_PartsOfSpeech.get(pspv);
                if (psp.equals("verb"))
                {
                    return word.getEMdFValue("lexeme_wit").getString();
                }
            }
        }
        
        return null;
    }
    
    public String getKingJamesVerse()
    {
        try
        {
            return OSISUtil.getCanonicalText(m_SwordVerse.getOsisFragment());
        }
        catch (Exception e)
        {
            return "KJV Gloss not found (" + e.toString() + ")";
        }
    }
    
    public int getSelectedLogicalStructureId()
    {
        return m_SelectedLogicalStructureId;
    }
    
    public String getUnlinkedLogicalStructure()
    {
        return m_LogicalStructure;
    }
    
    public String getLinkedLogicalStructure()
    {
        return m_LinkedLogicalStructure;
    }
}

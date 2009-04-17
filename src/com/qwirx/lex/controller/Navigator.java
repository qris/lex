package com.qwirx.lex.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Table;
import jemdros.TableIterator;
import jemdros.TableRow;

import org.aptivate.webutils.EditField;
import org.aptivate.webutils.EditField.SelectBox;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class Navigator
{
    private HttpServletRequest m_Request;
    private HttpSession m_Session;
    private EmdrosDatabase m_Emdros;
    private SetOfMonads m_VisibleMonads;
    private int m_MinM, m_MaxM;
    private DatabaseTransliterator m_Transliterator;
    private EditField m_Form;
    
    public Navigator(HttpServletRequest request, HttpSession session,
        EmdrosDatabase emdros, SetOfMonads visibleMonads,
        DatabaseTransliterator transliterator)
    throws DatabaseException
    {
        m_Request = request;
        m_Session = session;
        m_Emdros = emdros;
        m_VisibleMonads = emdros.getVisibleMonads();
        m_MinM = emdros.getMinM();
        m_MaxM = emdros.getMaxM();
        m_Transliterator = transliterator;
        m_Form = new EditField(request);
    }

    private Map<String, String> m_ParamOverrideMap =
        new HashMap<String, String>();

    public String getSessionString(String name, String defaultValue)
    {
        String value;
        
        value = m_ParamOverrideMap.get(name);
        
        if (value == null)
        {
            value = m_Request.getParameter(name);
        }
        
        if (value == null)
        {
            value = (String)m_Session.getAttribute(name);
        }
        
        if (value == null)
        {
            value = defaultValue;
        }
        
        return value;
    }
    
    private void setParamOverride(String name, String value)
    {
        m_ParamOverrideMap.put(name, value);
        m_Session.setAttribute(name, value);
    }

    private Map<String, String> m_Labels = new HashMap<String, String>();
    public String getLabel(String name) { return m_Labels.get(name); }
    
    public String getObjectNavigator(String objectType, String labelAttribute)
    throws DatabaseException, EmdrosException
    {
        return getObjectNavigator(objectType, new String[]{labelAttribute});
    }
    
    public String getObjectNavigator(String objectType,
        String [] labelAttributes)
    throws DatabaseException, EmdrosException
    {
        String selectedObject = getSessionString(objectType, null);

        boolean foundObject = false;
    
        Table objectTable = m_Emdros.getTable
        (
            "SELECT OBJECTS HAVING MONADS IN " + 
            m_Emdros.intersect(m_VisibleMonads, m_MinM, m_MaxM) +
            " [" + objectType + "]"
        );
        
        StringBuffer id_dList = new StringBuffer();
        String firstObject = null;
        
        TableIterator rows = objectTable.iterator();
        while (rows.hasNext()) 
        {
            TableRow row = rows.next();
            String objectId = row.getColumn(3);
            
            id_dList.append(objectId);

            if (rows.hasNext())
            {
                id_dList.append(",");
            }

            if (objectId.equals(selectedObject))
            {
                foundObject = true;
            }
            
            if (firstObject == null)
            {
                firstObject = objectId;
            }
        }
        
        if (!foundObject && firstObject != null)
        {
            selectedObject = firstObject;
        }
        
        StringBuffer query = new StringBuffer();
        query.append("GET FEATURES ");
        int [] columnMap = new int [labelAttributes.length + 1];
        columnMap[0] = 1;
        for (int i = 0; i < labelAttributes.length; i++)
        {
            query.append(labelAttributes[i]);
            if (i < labelAttributes.length - 1)
            {
                query.append(",");
            }
            columnMap[i + 1] = i + 2;
        }
        query.append(" FROM OBJECTS WITH ID_DS = " + 
            id_dList.toString() + " [" + objectType + "]");
        
        List<String[]> objects = m_Emdros.getTableAsListOfArrays(
            query.toString(), columnMap);

        for (String [] objectInfo : objects)
        {
            String objectId = objectInfo[0];
            
            if (objectId.equals(selectedObject))
            {
                for (int i = 0; i < labelAttributes.length; i++)
                {
                    m_Labels.put(labelAttributes[i], objectInfo[i + 1]);
                }
                
                Table monadTable = m_Emdros.getTable
                (
                    "GET MONADS FROM OBJECT WITH ID_D = " + objectId +
                    " [" + objectType + "]"
                );
                TableRow monad_row = monadTable.iterator().next();
                int new_min_m = Integer.parseInt(monad_row.getColumn(2));   
                int new_max_m = Integer.parseInt(monad_row.getColumn(3));   
                if (m_MinM < new_min_m) m_MinM = new_min_m;
                if (m_MaxM > new_max_m) m_MaxM = new_max_m;
                if (m_MinM > m_MaxM) m_MaxM = m_MinM + 1;
                // System.out.println("book restricts to " + min_m + "-" + max_m);
            }
        }
        
        setParamOverride(objectType, selectedObject);
        
        SelectBox sb = new SelectBox(objectType, objects, selectedObject);
        sb.setAttribute("onChange", "document.forms.nav.submit()");
        return sb.toString();
    }

    public int getClauseId()
    {
        return Integer.parseInt(getSessionString("clause", "0"));
    }
    
    public String getClauseNavigator()
    throws Exception
    {
        int selClauseId = getClauseId();
        MatchedObject verse = null;
        
        boolean foundSelectedClause = false;
        int defaultClauseId = 0;
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();
    
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            m_Emdros.intersect(m_VisibleMonads, m_MinM, m_MaxM) +
            " WHERE " +
            "[clause "+
            " [word GET phrase_dependent_part_of_speech, " +
            "  graphical_preformative_utf8, " +
            "  graphical_root_formation_utf8, " +
            "  graphical_lexeme_utf8, " +
            "  graphical_verbal_ending_utf8, " +
            "  graphical_nominal_ending_utf8, " +
            "  graphical_pron_suffix_utf8, " +
            "  person, number, gender, tense, stem " +
            " ]"+
            "]");
             
        SheafConstIterator clause_iter = sheaf.const_iterator();
        List<String[]> clauses = new ArrayList<String[]>();
                
        while (clause_iter.hasNext())
        {
            MatchedObject clause = clause_iter.next().const_iterator().next();
            SheafConstIterator word_iter = clause.getSheaf().const_iterator();
            String lexemes = "";
                    
            while (word_iter.hasNext())
            {
                MatchedObject word = word_iter.next().const_iterator().next();
                        
                lexemes += HebrewConverter.wordTranslitToHtml(word, generator,
                    m_Transliterator);
                    
                if (word_iter.hasNext()) 
                {
                    lexemes += " ";
                }
            }
            
            clauses.add(new String[]{ Integer.toString(clause.getID_D()),
                lexemes });
                
            int thisClauseId = clause.getID_D();
            if (thisClauseId == selClauseId)
            {
                foundSelectedClause = true;
            }
                    
            if (defaultClauseId == 0)
            {
                defaultClauseId = thisClauseId;
            }
        }
        
        if (!foundSelectedClause)
        {
            selClauseId = defaultClauseId;
        }
            
        setParamOverride("clause", Integer.toString(selClauseId));

        SelectBox sb = new SelectBox("clause", clauses, selClauseId + "");
        sb.setAttribute("onChange", "document.forms.nav.submit()");
        sb.setAttribute("class", "translit");
        return sb.toString();
    }
    
    public int getMinM()
    {
        return m_MinM;
    }

    public int getMaxM()
    {
        return m_MaxM;
    }
    
    public DatabaseTransliterator getTransliterator()
    {
        return m_Transliterator;
    }
}

package com.qwirx.lex.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jemdros.BadMonadsException;
import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Table;
import jemdros.TableException;
import jemdros.TableIterator;
import jemdros.TableRow;

import org.aptivate.web.controls.SelectBox;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;
import com.qwirx.lex.morph.HebrewMorphemeGenerator.Morpheme;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class Navigator
{
    private HttpServletRequest m_Request;
    private HttpSession m_Session;
    private EmdrosDatabase m_Emdros;
    private SetOfMonads m_VisibleMonads, m_FocusMonads;
    private DatabaseTransliterator m_Transliterator;
    
    public Navigator(HttpServletRequest request, HttpSession session,
        EmdrosDatabase emdros, SetOfMonads visibleMonads,
        DatabaseTransliterator transliterator)
    throws DatabaseException
    {
        m_Request = request;
        m_Session = session;
        m_Emdros = emdros;
        m_VisibleMonads = visibleMonads;
        m_FocusMonads = m_VisibleMonads;
        m_Transliterator = transliterator;
    }

    private Map<String, String> m_ParamOverrideMap =
        new HashMap<String, String>();

    public String getSessionString(String name, String defaultValue)
    {
        String value;
        
        value = m_ParamOverrideMap.get(name);
        
        if (value == null && m_Request != null)
        {
            value = m_Request.getParameter(name);
        }
        
        if (value == null && m_Session != null)
        {
            value = (String)m_Session.getAttribute(name);
        }
        
        if (value == null)
        {
            value = defaultValue;
        }
        
        return value;
    }
    
    public void setParamOverride(String name, String value)
    {
        m_ParamOverrideMap.put(name, value);
        
        if (m_Session != null)
        {
            m_Session.setAttribute(name, value);
        }
    }

    private Map<String, String> m_Labels = new HashMap<String, String>();
    public String getLabel(String name) { return m_Labels.get(name); }
    
    public SelectBox getObjectNavigator(String objectType,
        String labelAttribute)
    throws DatabaseException
    {
        return getObjectNavigator(objectType, new String[]{labelAttribute});
    }
    
    public SelectBox getObjectNavigator(String objectType,
        String [] labelAttributes)
    throws DatabaseException
    {
        String selectedObject = getSessionString(objectType, null);

        boolean foundObject = false;
    
        Table objectTable = m_Emdros.getTable
        (
            "SELECT OBJECTS HAVING MONADS IN " + m_FocusMonads.toString() +
            " [" + objectType + "]"
        );
        
        StringBuffer id_dList = new StringBuffer();
        String firstObject = null;
        
        TableIterator rows = objectTable.iterator();
        try
        {
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
        }
        catch (EmdrosException e)
        {
            throw new DatabaseException("Failed to find visible objects", e);
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
        
        List<String[]> objects = m_Emdros.getTableAsListOfArrays(query.toString(),
            columnMap);

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

                SetOfMonads objectMonads = new SetOfMonads();

                try
                {
                    TableRow monad_row = monadTable.iterator().next();
                    System.out.println(monad_row.getColumn(2) + "-" +
                        monad_row.getColumn(3));
                    objectMonads.add(Integer.parseInt(monad_row.getColumn(2)),
                        Integer.parseInt(monad_row.getColumn(3)));
                }
                catch (TableException te)
                {
                    throw new DatabaseException("Failed to retrieve monads " +
                        "from object " + objectId, te);
                }
                catch (BadMonadsException bme)
                {
                    throw new DatabaseException("Failed to retrieve monads " +
                        "from object " + objectId, bme);                    
                }
                
                m_FocusMonads = SetOfMonads.intersect(m_FocusMonads,
                    objectMonads);

                // System.out.println("book restricts to " + min_m + "-" + max_m);
            }
        }
        
        setParamOverride(objectType, selectedObject);
        
        SelectBox sb = new SelectBox(objectType, objects, selectedObject);
        sb.setAttribute("onChange", "document.forms.nav.submit()");
        return sb;
    }

    public int getClauseId()
    {
        return Integer.parseInt(getSessionString("clause", "0"));
    }
    
    public SelectBox getClauseNavigator()
    throws DatabaseException
    {
        int selClauseId = getClauseId();
        
        boolean foundSelectedClause = false;
        int defaultClauseId = 0;
        HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();
    
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " + m_FocusMonads.toString() +
            " WHERE " +
            "[clause "+
            " [word GET " + 
            new HebrewMorphemeGenerator().getRequiredFeaturesString(false) + 
            " ]"+
            "]");
             
        SheafConstIterator clause_iter = sheaf.const_iterator();
        List<String[]> clauses = new ArrayList<String[]>();
                
        while (clause_iter.hasNext())
        {
            MatchedObject clause;
            
            try
            {
                clause = clause_iter.next().const_iterator().next();
            }
            catch (EmdrosException e)
            {
                throw new DatabaseException("Failed to iterate over clauses", e);
            }
            
            SheafConstIterator word_iter = clause.getSheaf().const_iterator();
            StringBuffer lexemes = new StringBuffer();
                    
            while (word_iter.hasNext())
            {
                MatchedObject word;
                
                try
                {
                    word = word_iter.next().const_iterator().next();
                }
                catch (EmdrosException e)
                {
                    throw new DatabaseException("Failed to iterate over words",
                        e);
                }
                
                List<Morpheme> morphemes = generator.parse(word, false,
                    (String)null, m_Transliterator);
                
                for (Morpheme morpheme : morphemes)
                {
                    lexemes.append(HebrewConverter.toHtml(morpheme.getTranslit()));
                }
                
                Morpheme lastMorpheme = morphemes.get(morphemes.size() - 1);
                
                if (word_iter.hasNext() && !lastMorpheme.isDisplayedWithEquals())
                {
                    lexemes.append(" ");
                }
            }
            
            clauses.add(new String[]{ Integer.toString(clause.getID_D()),
                lexemes.toString() });
                
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
        return sb;
    }
    
    public SelectBox [] getHebrewNavigator()
    throws DatabaseException
    {
        return new SelectBox [] {
            getObjectNavigator("book", "book"),
            getObjectNavigator("chapter", "chapter"),
            getObjectNavigator("verse", new String[]{"verse_label", "verse"}),
            getClauseNavigator()  
        };
    }
    
    public SetOfMonads getFocusMonads()
    {
        return new SetOfMonads(m_FocusMonads);
    }
    
    public DatabaseTransliterator getTransliterator()
    {
        return m_Transliterator;
    }
}

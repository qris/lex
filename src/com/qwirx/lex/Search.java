package com.qwirx.lex;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jemdros.EmdrosException;
import jemdros.FlatSheaf;
import jemdros.FlatSheafConstIterator;
import jemdros.FlatStraw;
import jemdros.FlatStrawConstIterator;
import jemdros.MatchedObject;
import jemdros.MonadSetElement;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.Table;
import jemdros.TableException;
import jemdros.TableIterator;
import jemdros.TableRow;

import com.qwirx.db.DatabaseException;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.hebrew.HebrewConverter;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;

public class Search
{
    private int m_ResultCount = 0;
    private int m_MaxResults = 100;
    private EmdrosDatabase m_Emdros;
    
    public Search(EmdrosDatabase emdros)
    {
        m_Emdros = emdros;
    }

    private static class ResultBase
    {
        String location;
        String url;
        SetOfMonads monads;
        MatchedObject clause;
        String predicate, logicalStructure;
        List<MatchedObject> words = new ArrayList<MatchedObject>();
    }
    
    public void setMaxResults(int limit)
    {
        m_MaxResults = limit;
    }
    
    public List<SearchResult> basic(String query) 
    throws DatabaseException, SQLException, TableException, EmdrosException
    {
        return advanced("[word " +
            "lexeme = '"+query+"' OR " +
            "lexeme = '"+query+"/' OR " +
            "lexeme = '"+query+"[']");
    }
    
    private void addToMonadSet(Sheaf sheaf, SetOfMonads set)
    throws EmdrosException
    {
        if (sheaf == null)
        {
            return;
        }
        
        SheafConstIterator shci = sheaf.const_iterator();
        
        while (shci.hasNext())
        {
            Straw straw = shci.next();
            StrawConstIterator swci = straw.const_iterator();
            
            while (swci.hasNext())
            {
                MatchedObject object = swci.next();
                set.unionWith(object.getMonads());
            }
        }
    }
    
    public List<SearchResult> advanced(String query) 
    throws DatabaseException, SQLException, TableException, EmdrosException
    {    
        HebrewMorphemeGenerator generator = 
            new HebrewMorphemeGenerator();

        SetOfMonads visible = m_Emdros.getVisibleMonads();
        
        // First pass to find matching clauses
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " + visible.toString() + " " +
            "WHERE [clause " + query + " ]"
        );
        
        SheafConstIterator sci = sheaf.const_iterator();
        SetOfMonads clauseMonads = new SetOfMonads();
        List<ResultBase> resultBases = new ArrayList<ResultBase>();
        SetOfMonads matchSet = new SetOfMonads();
        Map<Integer,ResultBase> clauseIdToBase =
            new HashMap<Integer,ResultBase>();
        
        while (sci.hasNext())
        {
            Straw straw = sci.next();
            MatchedObject clause = straw.const_iterator().next();

            m_ResultCount++;
            if (m_ResultCount > m_MaxResults) continue; // just count them

            ResultBase base = new ResultBase();
            base.monads = clause.getMonads();
            base.clause = clause;
            resultBases.add(base);
            
            clauseIdToBase.put(new Integer(clause.getID_D()), base);
            clauseMonads.unionWith(base.monads);
            addToMonadSet(clause.getSheaf(), matchSet);
        }

        List<SearchResult> results = new ArrayList<SearchResult>();

        if (clauseMonads.isEmpty())
        {
            return results;
        }

        // now retrieve the verses
        
        Table table = m_Emdros.getTable("SELECT OBJECTS " +
            "HAVING MONADS IN " + clauseMonads + " [verse]");
        StringBuffer verseIdList = new StringBuffer();
        
        for (TableIterator ti = table.iterator(); ti.hasNext();)
        {
            TableRow tr = ti.next();
            
            verseIdList.append(tr.getColumn(3));

            if (ti.hasNext())
            {
                verseIdList.append(",");
            }
        }

        StringBuffer verseMonadsQuery = new StringBuffer("GET MONADS " +
            "FROM OBJECTS WITH ID_DS = ");
        verseMonadsQuery.append(verseIdList);
        verseMonadsQuery.append(" [verse]");
        
        table = m_Emdros.getTable(verseMonadsQuery.toString());
        Map<Integer,SetOfMonads> verseIdToMonadsMap =
            new HashMap<Integer,SetOfMonads>();
        
        for (TableIterator ti = table.iterator(); ti.hasNext();)
        {
            TableRow tr = ti.next();
            int ID_D = Integer.parseInt(tr.getColumn(1));
            
            SetOfMonads som = verseIdToMonadsMap.get(new Integer(ID_D));
            if (som == null)
            {
                som = new SetOfMonads();
                verseIdToMonadsMap.put(new Integer(ID_D), som);
            }
            
            som.add(Integer.parseInt(tr.getColumn(2)),
                Integer.parseInt(tr.getColumn(3)));
        }

        StringBuffer verseFeaturesQuery = new StringBuffer("GET FEATURES " +
            "book, chapter, verse, verse_label FROM OBJECTS WITH ID_DS = ");
        verseFeaturesQuery.append(verseIdList);
        verseFeaturesQuery.append(" [verse]");
        
        table = m_Emdros.getTable(verseFeaturesQuery.toString());

        for (TableIterator ti = table.iterator(); ti.hasNext();)
        {
            TableRow tr = ti.next();
            int ID_D = Integer.parseInt(tr.getColumn(1));
            SetOfMonads monads = verseIdToMonadsMap.get(new Integer(ID_D));
            
            // TODO fix horribly slow algorithm
            for (Iterator<ResultBase> i = resultBases.iterator(); i.hasNext();)
            {
                ResultBase base = i.next();
                if (base.monads.part_of(monads))
                {
                    base.location = tr.getColumn(5);
                    base.url = "clause.jsp?book=" + tr.getColumn(2) +
                        "&amp;chapter=" + tr.getColumn(3) +
                        "&amp;verse="   + tr.getColumn(4) +
                        "&amp;clause="  + base.clause.getID_D();
                }
            }
        }
        
        {
            String mql = "GET FEATURES predicate, logical_structure " +
                "FROM OBJECTS WITH ID_DS = ";
            for (Iterator<ResultBase> i = resultBases.iterator(); i.hasNext();)
            {
                ResultBase base = i.next();
                mql += base.clause.getID_D();
                if (i.hasNext())
                {
                    mql += ",";
                }
            }
            mql += " [clause]";
            table = m_Emdros.getTable(mql);
            for (TableIterator ti = table.iterator(); ti.hasNext();)
            {
                TableRow tr = ti.next();
                ResultBase base = clauseIdToBase.get(new Integer(tr.getColumn(1)));
                base.predicate = tr.getColumn(2);
                base.logicalStructure = tr.getColumn(3);
            }
        }

        FlatSheaf flat = m_Emdros.getFlatSheaf(
            "GET OBJECTS HAVING MONADS IN " +
            clauseMonads.toString() + 
            "[word GET " +
            " lexeme, " +
            " phrase_dependent_part_of_speech, " +
            " graphical_preformative, " +
            " graphical_root_formation, " +
            " graphical_lexeme, " +
            " graphical_verbal_ending, " +
            " graphical_nominal_ending, " +
            " graphical_pron_suffix" +
            "]");
        
        FlatSheafConstIterator fsci = flat.const_iterator();
        FlatStraw fs = fsci.next();
        FlatStrawConstIterator fwci = fs.const_iterator();

        while (fwci.hasNext())
        {
            MatchedObject word = fwci.next();
            for (Iterator<ResultBase> i = resultBases.iterator(); i.hasNext();)
            {
                ResultBase base = i.next();
                if (SetOfMonads.overlap(base.monads, word.getMonads()))
                {
                    base.words.add(word);
                }
            }
        }
        
        for (Iterator<ResultBase> i = resultBases.iterator(); i.hasNext();)
        {
            ResultBase base = i.next();
            String original = "";
            String translit = "";
            
            for (Iterator<MatchedObject> j = base.words.iterator(); j.hasNext();)
            {
                MatchedObject word = j.next();
                
                boolean isMatch = SetOfMonads.overlap(word.getMonads(),
                    matchSet);
                
                if (isMatch)
                {
                    original += "<strong>";
                    translit += "<strong>";
                }
                
                original += HebrewConverter.wordHebrewToHtml  (word, generator);
                translit += HebrewConverter.wordTranslitToHtml(word, generator);
                
                if (isMatch)
                {
                    original += "</strong>";
                    translit += "</strong>";
                }
        
                original += " ";
                translit += " ";
            }

            SearchResult result = new SearchResult(base.location, 
                "<div class=\"hebrew\">"   + original + "</div>\n" +
                "<div class=\"translit\">" + translit + "</div>\n",
                base.url, base.predicate, base.logicalStructure);
            results.add(result);
        }
        
        return results;
    }
    
    public int getResultCount() { return m_ResultCount; }
    
    public static class SearchResult
    {
        private String m_Location, m_Description, m_LinkUrl;
        private String m_Predicate, m_LogicalStructure;
        
        public SearchResult(String location, String description,
            String linkUrl, String predicate, String logicalStructure)
        {
            m_Location = location;
            m_Description = description;
            m_LinkUrl = linkUrl;
            m_Predicate = predicate;
            m_LogicalStructure = logicalStructure;
        }
        
        public String getLocation() { return m_Location; }
        public String getDescription() { return m_Description; }
        public String getLinkUrl() { return m_LinkUrl; }
        public String getPredicate() { return m_Predicate; }
        public String getLogicalStructure() { return m_LogicalStructure; }
    }
}

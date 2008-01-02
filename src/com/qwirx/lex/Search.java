package com.qwirx.lex;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jemdros.FlatSheaf;
import jemdros.FlatSheafConstIterator;
import jemdros.FlatStraw;
import jemdros.FlatStrawConstIterator;
import jemdros.MatchedObject;
import jemdros.SetOfMonads;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.Table;
import jemdros.TableException;
import jemdros.TableIterator;
import jemdros.TableRow;

import org.xml.sax.SAXException;

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
    throws DatabaseException, IOException, SAXException, SQLException,
        TableException
    {
        return advanced("[word " +
            "lexeme = '"+query+"' OR " +
            "lexeme = '"+query+"/' OR " +
            "lexeme = '"+query+"[']");
    }
    
    private void addToMonadSet(Sheaf sheaf, SetOfMonads set)
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
    throws DatabaseException, IOException, SAXException, SQLException,
        TableException
    {    
        HebrewMorphemeGenerator generator = 
            new HebrewMorphemeGenerator(m_Emdros);
        
        Sheaf sheaf = m_Emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            m_Emdros.getVisibleMonads().toString() + " " +
            "WHERE [verse GET book, chapter, verse, verse_label " +
            "       [clause " + query + " ]]"
        );
        
        List<ResultBase> resultBases = new ArrayList<ResultBase>();
        SetOfMonads powerSet = new SetOfMonads();
        SetOfMonads matchSet = new SetOfMonads();
        SheafConstIterator sci = sheaf.const_iterator();
        
        Map<Integer,ResultBase> clauseIdToBase =
            new HashMap<Integer,ResultBase>();
        
        m_ResultCount = 0;
        
        while (sci.hasNext())
        {
            Straw straw = sci.next();
            MatchedObject verse = straw.const_iterator().next();
            
            SheafConstIterator clause_iter =
                verse.getSheaf().const_iterator();
                
            while (clause_iter.hasNext())
            {
                m_ResultCount++;
                if (m_ResultCount > m_MaxResults) continue; // just count them
                
                MatchedObject clause =
                    clause_iter.next().const_iterator().next();
                ResultBase base = new ResultBase();
                base.monads = clause.getMonads();
                base.location = verse.getFeatureAsString(
                    verse.getEMdFValueIndex("verse_label"));
                base.url = "clause.jsp?book=" + 
                    m_Emdros.getEnumConstNameFromValue("book_name_e",
                        verse.getEMdFValue("book").getInt()) +
                    "&amp;chapter=" + verse.getEMdFValue("chapter") +
                    "&amp;verse="   + verse.getEMdFValue("verse") +
                    "&amp;clause="  + clause.getID_D();
                base.clause = clause;
                resultBases.add(base);
                powerSet.unionWith(base.monads);
                addToMonadSet(clause.getSheaf(), matchSet);
                
                clauseIdToBase.put(new Integer(clause.getID_D()), base);
            }
        }
        
        List<SearchResult> results = new ArrayList<SearchResult>();

        if (powerSet.isEmpty())
        {
            return results;
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
            Table table = m_Emdros.getTable(mql);
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
            powerSet.toString() + 
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
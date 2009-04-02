package com.qwirx.lex.lexicon;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import jemdros.EmdrosException;
import jemdros.MatchedObject;

import com.qwirx.db.Change;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;

public class Lexeme implements Comparable 
{
    public static class Variable implements Comparable
    {
        public int id, lexemeId;
        public String name, value;
        public Variable(String name, String value)
        {
            this.name  = name;
            this.value = value;
        }
        public int compareTo(Object obj) 
        {
            Variable that = (Variable)obj;
            return this.name.compareTo(that.name);
        }
        public boolean equals(Object obj)
        {
            Variable that = (Variable)obj;
            return this.name.equals(that.name);
        }
    }

    private SqlDatabase m_sqldb;
    public Lexeme(SqlDatabase sqldb)
    {
        this.m_sqldb = sqldb;
    }
    
    public int    id, parentId, numSyntacticArgs;
    public String label, desc, surface, m_Gloss;
    public Lexeme parent;
    public List   children  = new Vector();

    public String getLogicalStructure() 
    { 
        String ls = "";
        
        if (m_isCaused)
        {
            ls += "do'(<x>, \u00D8) CAUSE ";
        }
        
        if (m_isPunctual)
        {
            if (m_hasResultState) 
            {
                ls += "INGR ";
            }
            else
            {
                ls += "SEMEL ";
            }
        }
        
        if (m_isTelic)
        {
            ls += "BECOME ";
        }
        
        if (m_isDynamic)
        {
            ls += "do'(<x>, [";
        }
        
        if (m_predicate != null)
        {
            ls += m_predicate;
        }
        
        if (m_thematicRelation != null)
        {
            ls += m_thematicRelation.getArgText();
        }
        
        if (m_isDynamic)
        {
            ls += "])";

            if (m_hasEndpoint)
            {
                ls += " & INGR ";
                
                if (m_resultPredicate != null)
                {
                    ls += m_resultPredicate;
                }
                
                if (m_resultPredicateArg != null)
                {
                    String [] args = m_resultPredicateArg.split(",");
                    ls += "(";
                    for (int i = 0; i < args.length; i++)
                    {
                        ls += "<" + args[i] + ">";
                        if (i < args.length - 1)
                        {
                            ls += ", ";
                        }
                    }
                    ls += ")";
                }
            }
        }

        return ls; 
    }
    
    private boolean m_isCaused;
    public boolean isCaused() { return m_isCaused; }
    public void setCaused(boolean value) { m_isCaused = value; }

    private boolean m_isPunctual;
    public boolean isPunctual() { return m_isPunctual; }
    public void setPunctual(boolean value) { m_isPunctual = value; }

    private boolean m_hasResultState;
    public boolean hasResultState() { return m_hasResultState; }
    public void setHasResultState(boolean value) { m_hasResultState = value; }

    private boolean m_isTelic;
    public boolean isTelic() { return m_isTelic; }
    public void setTelic(boolean value) { m_isTelic = value; }

    private String m_predicate;
    public String getPredicate() { return m_predicate; }
    public void setPredicate(String pred) { m_predicate = pred; }

    private boolean m_isDynamic;
    public boolean isDynamic() { return m_isDynamic; }
    public void setDynamic(boolean value) { m_isDynamic = value; }

    private boolean m_hasEndpoint;
    public boolean hasEndpoint() { return m_hasEndpoint; }
    public void setHasEndpoint(boolean value) { m_hasEndpoint = value; }

    private ThematicRelation m_thematicRelation;
    public ThematicRelation getThematicRelation() { return m_thematicRelation; }
    public void setThematicRelation(ThematicRelation trel) 
    { 
        m_thematicRelation = trel; 
    }
    public String getThematicRelationName() 
    { 
        if (m_thematicRelation == null)
            return null;
        return m_thematicRelation.getLabel();
    }

    private String m_resultPredicate;
    public String getResultPredicate() { return m_resultPredicate; }
    public void setResultPredicate(String pred) { m_resultPredicate = pred; }

    private String m_resultPredicateArg;
    public String getResultPredicateArg() { return m_resultPredicateArg; }
    public void setResultPredicateArg(String arg) { m_resultPredicateArg = arg; }

    // for sorting
    public int compareTo(Object o) 
    {
        Lexeme e = (Lexeme)o;
        if (e.label == null && this.label == null)
            return 0;
        if (this.label == null)
            return 1;
        if (e.label == null)
            return -1;
        
        try 
        {
            int thisLabel  = new Integer(this.label).intValue();
            int otherLabel = new Integer(e.label)   .intValue();
            return thisLabel - otherLabel;
        } 
        catch (NumberFormatException ne) 
        {
            return this.label.compareTo(e.label);
        }
    }       

    private boolean compareMaybeNull(String a, String b)
    {
        if (a == null && b == null)
        {
            return true;
        }

        if (a != null && b != null && a.equals(b))
        {
            return true;
        }
        
        return false;
    }
    
    public boolean equals(Lexeme that)
    {
        if (this.id != that.id) return false;
        if (!compareMaybeNull(this.getLogicalStructure(), 
            that.getLogicalStructure()))
            return false;
        if (this.isCaused()       != that.isCaused())       return false;
        if (this.isPunctual()     != that.isPunctual())     return false;
        if (this.hasResultState() != that.hasResultState()) return false;
        if (this.isTelic()        != that.isTelic())        return false;
        if (this.isDynamic()      != that.isDynamic())      return false;
        if (this.hasEndpoint()    != that.hasEndpoint())    return false;
        if (!compareMaybeNull(this.getPredicate(), that.getPredicate())) 
            return false;
        if (this.getThematicRelation() != that.getThematicRelation())
            return false;
        if (!compareMaybeNull(this.getResultPredicate(), 
            that.getResultPredicate())) 
            return false;
        
        if (!compareMaybeNull(this.getResultPredicateArg(), 
            that.getResultPredicateArg())) 
        {
            return false;
        }
        
        return true;
    }
    
    public String getGloss()
    {
        return m_Gloss;
    }
    
    public void setGloss(String gloss)
    {
        this.m_Gloss = gloss; 
    }
    
    public int getID()
    {
        return id;
    }

    private static Lexeme load(SqlDatabase sqldb, ResultSet rs) 
    throws DatabaseException
    {
        try
        {
            Lexeme l = new Lexeme(sqldb);
            
            l.id       = rs.getInt("ID");
            l.surface  = rs.getString("Lexeme");
            l.m_Gloss    = rs.getString("Gloss");
            l.label    = rs.getString("Domain_Label");
            l.desc     = rs.getString("Domain_Desc");
            l.parentId = rs.getInt("Domain_Parent_ID");
            l.numSyntacticArgs = rs.getInt("Syntactic_Args");
    
            l.setCaused         (rs.getInt("Caused")           == 1);
            l.setPunctual       (rs.getInt("Punctual")         == 1);
            l.setHasResultState (rs.getInt("Has_Result_State") == 1);
            l.setTelic          (rs.getInt("Telic")            == 1);
            l.setDynamic        (rs.getInt("Dynamic")          == 1);
            l.setHasEndpoint    (rs.getInt("Has_Endpoint")     == 1);
    
            l.setPredicate      (rs.getString("Predicate"));
            l.setResultPredicate(rs.getString("Result_Predicate"));
            l.setResultPredicateArg(rs.getString("Result_Predicate_Arg"));
            
            l.setThematicRelation(ThematicRelation.get(
                rs.getString("Thematic_Relation")));
            
            return l;
        }
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to load lexeme from ResultSet",
                e);
        }
    }
    
    private static String getColumnList() 
    {
        return "ID,Lexeme,Gloss,Structure,Domain_Label,Domain_Desc,"+
            "Domain_Parent_ID,Syntactic_Args, Caused, "+
            "Punctual, Has_Result_State, Telic, Predicate, "+
            "Thematic_Relation, Dynamic, Has_Endpoint, "+
            "Result_Predicate, Result_Predicate_Arg";
    }

    public static Lexeme load(SqlDatabase sqldb, int id)
    throws DatabaseException
    {
        Lexeme result = null;
        
        try 
        {
            PreparedStatement stmt = sqldb.prepareSelect
                ("SELECT " + getColumnList() + " " +
                 "FROM lexicon_entries WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = sqldb.select();
            
            if (!rs.next()) 
            {
                return null;
            }
            
            result = load(sqldb, rs);
        } 
        catch (SQLException e)
        {
            throw new DatabaseException("Failed to load lexeme", e);
        }
        finally 
        {
            sqldb.finish();
        }
        
        return result;
    }

    public static Lexeme load(SqlDatabase sqldb, String predicate)
    throws DatabaseException, SQLException
    {
        Lexeme result = null;
        
        try 
        {
            PreparedStatement stmt = sqldb.prepareSelect
                ("SELECT " + getColumnList() + " " +
                 "FROM lexicon_entries " +
                 "WHERE Lexeme = ?");
            stmt.setString(1, predicate);
            ResultSet rs = sqldb.select();
            
            if (!rs.next()) 
            {
                return null;
            }
            
            result = load(sqldb, rs);
        } 
        finally 
        {
            sqldb.finish();
        }
        
        return result;
    }

    private MatchedObject m_WordObject = null;

    public static Lexeme load(SqlDatabase sqldb, MatchedObject word)
    throws DatabaseException, SQLException, EmdrosException
    {
        Lexeme result = null;
        
        try 
        {
            PreparedStatement stmt = sqldb.prepareSelect
                ("SELECT " + getColumnList() + " " +
                 "FROM lexicon_entries " +
                 "WHERE Lexeme = ?");
            stmt.setString(1, word.getEMdFValue("lexeme_wit").getString());
            ResultSet rs = sqldb.select();
            
            if (!rs.next()) 
            {
                return null;
            }
            
            result = load(sqldb, rs);
        } 
        finally 
        {
            sqldb.finish();
        }
        
        result.m_WordObject = word;
        return result;
    }

    public static Lexeme findOrBuild(SqlDatabase sqldb, MatchedObject word)
    throws DatabaseException, SQLException, EmdrosException
    {
        Lexeme result = load(sqldb, word);

        if (result == null)
        {
            result = new Lexeme(sqldb);
            result.m_WordObject = word;
            result.surface = word.getEMdFValue("lexeme_wit").getString();
        }
        
        return result;
    }

    public static Lexeme getTreeRoot(SqlDatabase sqldb) 
    throws DatabaseException, SQLException
    {
        Lexeme root = new Lexeme(sqldb);
        Map m = new Hashtable();

        try 
        {
            // PreparedStatement stmt = 
            sqldb.prepareSelect("SELECT " + getColumnList() +
                " FROM lexicon_entries ORDER BY Domain_Label + 0");
            ResultSet rs = sqldb.select();
            
            while (rs.next()) 
            {
                Lexeme l = load(sqldb, rs); 
                
                if (l.id == 0) 
                {
                    System.err.println("Bad ID 0 for "+l.desc);
                    continue;
                }
    
                m.put(l.id + "", l);
            }
        } 
        finally 
        {
            sqldb.finish();
        }

        // find children, build the tree structure
        Iterator k = m.keySet().iterator();
        while (k.hasNext()) 
        {
            Lexeme l = (Lexeme)( m.get(k.next()) );
            l.parent = (Lexeme)( m.get(l.parentId + "") );
            if (l.parent == null)
                l.parent = root;
            l.parent.children.add(l);
        }
        
        return root;
    }
    
    public void sortInPlace()
    {
        Collections.sort(children);
        for (Iterator i = children.iterator(); i.hasNext(); ) 
        {
            Lexeme child = (Lexeme)( i.next() );
            child.sortInPlace();
        }
    }
    
    public void save() throws DatabaseException
    {
        Change ch;
        if (id == 0)
        {
            ch = m_sqldb.createChange(SqlChange.INSERT, "lexicon_entries", 
                null);
            ch.setString("Lexeme", surface);
        }
        else
        {
            ch = m_sqldb.createChange(SqlChange.UPDATE, "lexicon_entries", 
                "ID = " + id);
        }

        ch.setString("Gloss",      m_Gloss);
        ch.setString("Structure",  getLogicalStructure());
        ch.setString("Caused",     isCaused()   ? "1" : "0");
        ch.setString("Punctual",   isPunctual() ? "1" : "0");
        ch.setString("Has_Result_State", hasResultState() ? "1" : "0");
        ch.setString("Telic",      isTelic()    ? "1" : "0");
        ch.setString("Dynamic",    isDynamic()  ? "1" : "0");
        ch.setString("Has_Endpoint", hasEndpoint() ? "1" : "0");
        ch.setString("Predicate",  getPredicate());
        ch.setString("Thematic_Relation", getThematicRelationName());
        ch.setString("Result_Predicate",  getResultPredicate());
        ch.setString("Result_Predicate_Arg",  getResultPredicateArg());
        
        /*
        if (request.getParameter("lspred") != null)
            ch.setString("Predicate",   request.getParameter("lspred"));
        if (request.getParameter("lsarg") != null)
            ch.setString("Arguments",   request.getParameter("lsarg"));
        ch.setString("Become",
            request.getParameter("lsbec")  == null ? "0" : "1");
        if (request.getParameter("lsbp") != null)
            ch.setString("Become_Pred", request.getParameter("lsbp"));
        if (request.getParameter("lsba") != null)
            ch.setString("Become_Args", request.getParameter("lsba"));


        if (request.getParameter("lscakt") != null)
            ch.setString("Caused_Aktionsart",  request.getParameter("lscakt"));
        ch.setString("Caused_Active",      
            request.getParameter("lscact") == null ? "0" : "1");
        ch.setString("Caused_Pred_Enable", 
            request.getParameter("lscpe")  == null ? "0" : "1");
        if (request.getParameter("lscpred") != null)
            ch.setString("Caused_Predicate", request.getParameter("lscpred"));
        if (request.getParameter("lscarg") != null)
            ch.setString("Caused_Arguments", request.getParameter("lscarg"));
        ch.setString("Caused_Become",
            request.getParameter("lscbec")  == null ? "0" : "1");
        if (request.getParameter("lscbp") != null)
            ch.setString("Caused_Become_Pred", request.getParameter("lscbp"));
        if (request.getParameter("lscba") != null)
            ch.setString("Caused_Become_Args", request.getParameter("lscba"));
        */
        
        ch.execute();
        if (id == 0)
        {
            id = ((SqlChange)ch).getInsertedRowId();
        }
    }

    public void delete() throws DatabaseException
    {
        if (id == 0)
        {
            throw new AssertionError("Cannot delete a Lexeme without an ID");
        }
        
        Change ch = m_sqldb.createChange(SqlChange.DELETE, "lexicon_entries", 
            "ID = " + id);
        ch.execute();
    }
    
    public static abstract class Visitor 
    {
        protected Lexeme root;
        
        protected Visitor(Lexeme root)
        {
            this.root = root;
        }
        
        public void visit() throws IOException
        {
            visit(root, "");
        }
        
        protected abstract void output(Lexeme e, String fullPath, String desc)
        throws IOException;
        
        protected void visit(Lexeme e, String parentPath) throws IOException 
        {
            String fullPath = "";
            
            if (e != root) 
            {
                String desc = e.desc;
                if (desc == null) 
                {
                    desc = "(no description)";
                }
                
                fullPath = parentPath + e.label + ".";
                
                output(e, fullPath, desc);
            }
            
            Iterator ci = e.children.iterator();
            
            while (ci.hasNext()) 
            {
                Lexeme ce = (Lexeme)( ci.next() );
                visit(ce, fullPath);
            }
        }
    }

    public static final class Finder extends Visitor
    {
        private int m_lsIdToFind;
        private Lexeme m_found = null;
        
        public Finder(Lexeme root, int idToFind)
        {
            super(root);
            m_lsIdToFind = idToFind;
        }

        protected void visit(Lexeme e, String parentPath) throws IOException
        {
            if (e.id == m_lsIdToFind)
            {
                m_found = e;
            }
            else
            {
                super.visit(e, parentPath);
            }
        }

        protected void output(Lexeme e, String fullPath, String desc) { }
        
        public Lexeme getFoundLexeme() { return m_found; }
    }

    public Map getVariables(SqlDatabase sql) 
    throws DatabaseException, SQLException 
    {
        Map vars = new Hashtable();
        
        try {
            PreparedStatement stmt = sql.prepareSelect
                ("SELECT ID,Name,Value "+
                 "FROM   lexicon_variables "+
                 "WHERE  Lexeme_ID = ?");
            stmt.setInt(1, id);
            ResultSet rs = sql.select();
            
            while (rs.next()) 
            {
                Variable v = new Variable(
                    rs.getString("Name"), 
                    rs.getString("Value"));
                v.lexemeId = id;
                v.id = rs.getInt   ("ID");
                
                if (vars.get(v.name) == null) 
                {
                    vars.put(v.name, v);
                }
            }
        } 
        finally 
        {
            sql.finish();
        }

        return vars;
    }
    
    public Map getAllVariables(SqlDatabase sql) 
    throws DatabaseException, SQLException
    {
        Map allVars = new Hashtable();
        
        for (Lexeme l = this; l != null; l = l.parent) {
            Map vars = l.getVariables(sql);
            for (Iterator vi = vars.keySet().iterator(); vi.hasNext(); ) {
                String name = (String)( vi.next() );
                if (allVars.get(name) == null)
                    allVars.put(name, vars.get(name));
            }
        }

        return allVars;
    }

    public String getEvaluatedLogicalStructure(Map vars)
    {
        Set varSet = new TreeSet();

        for (Iterator vi = vars.keySet().iterator(); vi.hasNext(); ) 
        {
            Variable v = (Variable)( vars.get(vi.next()) );
            varSet.add(v);
        }
        
        return getEvaluatedLogicalStructure(varSet);
    }
    
    public String getEvaluatedLogicalStructure(Set vars)
    {
        String logic = getLogicalStructure();

        for (Iterator vi = vars.iterator(); vi.hasNext(); ) 
        {
            Variable v = (Variable)( vi.next() );
            String placeholder = "<" + v.name + ">";
            logic = logic.replaceAll(placeholder, v.value);
        }

        return logic;
    }
}

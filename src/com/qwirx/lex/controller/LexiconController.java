package com.qwirx.lex.controller;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;

import com.qwirx.db.Change;
import com.qwirx.db.DatabaseException;
import com.qwirx.db.sql.SqlChange;
import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosChange;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.lexicon.Lexeme;
import com.qwirx.lex.lexicon.ThematicRelation;

public class LexiconController extends ControllerBase
{
    private Lexeme m_Lexeme;
    private List<String> m_ErrorMessages = new ArrayList<String>();
    
    public LexiconController(HttpServletRequest request, EmdrosDatabase emdros,
        SqlDatabase sql)
    throws Exception
    {
        super(request, emdros, sql, null);
        
        m_Lexeme = new Lexeme(sql);
        
        try
        {
            m_Lexeme = Lexeme.load(sql, 
                Integer.parseInt(request.getParameter("lsid")));
        }
        catch (Exception e)
        {
            /* do nothing, use default */
        }
        
        // String current.surface = "", current.logic = "";
        // int domain_parent_id = 0;
        // String current.label = "", current.desc = "";
        
        if (request.getParameter("createnew") != null)
        {
            SqlChange ch = (SqlChange)sql.createChange(SqlChange.INSERT,
                "lexicon_entries", null);
            ch.setString("Domain_Desc",      "New Entry");
            ch.execute();
            m_Lexeme = Lexeme.load(sql, ch.getInsertedRowId());
        }
        else if (request.getParameter("savecopy") != null)
        {
            throw new AssertionError("broken");
            /*
            boolean createNew = false;
            
            if (lsId <= 0) 
                createNew = true;
            if (request.getParameter("savecopy") != null)
                createNew = true;
                
            try {

                if (request.getParameter("surface") != null)
                {
                    ch.setString("Lexeme",       request.getParameter("surface"));
                }
                ch.setString("Structure",        request.getParameter("ls"));
                ch.setInt   ("Domain_Parent_ID", domain_parent_id);
                ch.setString("Domain_Label",     request.getParameter("dl"));
                ch.setString("Domain_Desc",      request.getParameter("dd"));
                ch.setString("Syntactic_Args",   request.getParameter("sa"));
                ch.execute();
                
                if (createNew) {
                    lsId = ((SqlChange)ch).getInsertedRowId();
                }
            } catch (SQLException sqlEx) {
                %><%= sqlEx %><%
            }
            */
        }
        else if (request.getParameter("delrepl") != null)
        {
            try
            {
                int newLsId = Integer.parseInt(request.getParameter("newlsid"));
                
                Sheaf sheaf = emdros.getSheaf
                    ("SELECT ALL OBJECTS "+
                     "IN {" + emdros.getMinM() + "-" + emdros.getMaxM() + "} "+
                     "WHERE [clause logical_struct_id = "+m_Lexeme.getID()+"]");
                
                SheafConstIterator sci = sheaf.const_iterator();
                if (sci.hasNext())
                {
                    Vector objectIds = new Vector();
        
                    while (sci.hasNext())
                    {
                        Straw straw = sci.next();
                        MatchedObject clause = straw.const_iterator().next();
                        objectIds.add(new Integer(clause.getID_D()));
                    }

                    int [] objectIdArray = new int[objectIds.size()];
                    for (int i = 0; i < objectIds.size(); i++)
                    {
                        objectIdArray[i] = 
                            ((Integer)( objectIds.get(i) )).intValue();
                    }
                    
                    Change ch = emdros.createChange(EmdrosChange.UPDATE, 
                        "clause", objectIdArray);
                    ch.setInt("logical_struct_id", newLsId);
                    ch.execute();
                }

                sql.createChange(SqlChange.DELETE,
                    "lexicon_entries", "ID = "+m_Lexeme.getID()).execute();
                m_Lexeme = Lexeme.load(sql, newLsId);
            } 
            catch (Exception e) 
            {
                m_ErrorMessages.add("Failed to delete lexicon entry: " + 
                    e.toString());
            }
        } 
        else if (request.getParameter("vcu") != null) 
        {
            // variable create or update
            
            boolean createVar = false;
            int vid = 0;
            if (request.getParameter("vid") != null)
            {
                try
                {
                    vid = new Integer(request.getParameter("vid")).intValue();
                }
                catch (NumberFormatException e)
                {
                    vid = 0;
                }
            }
            
            if (vid == 0)
            {
                createVar = true;
            }
            
            try
            {
                String query = 
                    "SELECT ID FROM lexicon_variables WHERE Name = ? "+
                    "AND Lexeme_ID = ?";

                if (!createVar)
                {
                    query += " AND ID <> ?";
                }
                
                PreparedStatement stmt = sql.prepareSelect(query);
                stmt.setString(1, request.getParameter("vn"));
                stmt.setInt   (2, m_Lexeme.getID());
                if (!createVar)
                {
                    stmt.setInt(3, vid);
                }
                
                ResultSet rs = sql.select();
                boolean alreadyExists = rs.next();
                sql.finish();
                
                if (alreadyExists)
                {
                    m_ErrorMessages.add("Duplicate variable name: " +
                        request.getParameter("vn"));
                }
                else
                {
                    Change ch;
                    
                    if (createVar)
                    {
                        ch = sql.createChange(SqlChange.INSERT,
                            "lexicon_variables", null);
                    }
                    else
                    {
                        ch = sql.createChange(SqlChange.UPDATE,
                            "lexicon_variables", "ID = "+vid);
                    }

                    ch.setString("Name",      request.getParameter("vn"));
                    ch.setString("Value",     request.getParameter("vv"));
                    ch.setInt   ("Lexeme_ID", m_Lexeme.getID());
                    ch.execute();
                }
            }
            catch (DatabaseException sqlEx)
            {
                m_ErrorMessages.add("Failed to modify variable: " + 
                    sqlEx.toString());
            }
        } 
        else if (request.getParameter("vd") != null) 
        {
            // variable delete 
            
            int vid = 0;

            if (request.getParameter("vid") != null)
            {
                try
                {
                    vid = new Integer(request.getParameter("vid")).intValue();
                }
                catch (NumberFormatException e)
                {
                    vid = 0;
                }
            }

            if (vid != 0)
            {
                try
                {
                    sql.createChange(SqlChange.DELETE,
                        "lexicon_variables", "ID = "+vid).execute();
                }
                catch (DatabaseException sqlEx)
                {
                    m_ErrorMessages.add("Failed to delete variable: " +
                        sqlEx.toString());
                }
            }
        }
        else if (request.getParameter("dpid") != null)
        {
            int domain_parent_id = new Integer(request.getParameter("dpid"))
                .intValue();
                    
            // parent hierarchy loop check

            if (domain_parent_id == m_Lexeme.getID()) 
            {
                domain_parent_id = 0;
            }
            
            if (domain_parent_id > 0) 
            {
                int maxDepth = 20;
                int thisAncestor = domain_parent_id;
                
                while (maxDepth > 0) 
                {
                    try 
                    {
                        PreparedStatement stmt = sql.prepareSelect
                            ("SELECT Domain_Parent_ID "+
                            "FROM lexicon_entries "+
                            "WHERE ID = ?");
                        stmt.setInt(1, thisAncestor);
                        ResultSet rs = sql.select();
                        
                        if (!rs.next()) {
                            // parent tree has no path to root?
                            domain_parent_id = 0;
                            break;
                        }
                        
                        thisAncestor = rs.getInt(1);
                        
                        if (thisAncestor == 0)
                        {
                            // reached the root
                            break;
                        }
                        
                        if (thisAncestor == m_Lexeme.getID())
                        {
                            // loop detected
                            domain_parent_id = 0;
                            break;
                        }                               
                        maxDepth--;
                    }
                    finally
                    {
                        sql.finish();
                    }
                }

                if (maxDepth == 0)
                {
                    m_ErrorMessages.add("You cannot set the domain parent " +
                        "to one of this object's children: that would " +
                        "create a loop!");
                    domain_parent_id = 0;
                }
            }
            
            Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
                "ID = " + m_Lexeme.getID());
            ch.setInt("Domain_Parent_ID", domain_parent_id);
            ch.execute();
        }
        else if (request.getParameter("dl") != null)
        {
            Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
                "ID = " + m_Lexeme.getID());
            ch.setString("Domain_Label", request.getParameter("dl"));
            ch.execute();
        }
        else if (request.getParameter("dd") != null)
        {
            Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
                "ID = " + m_Lexeme.getID());
            ch.setString("Domain_Desc", request.getParameter("dd"));
            ch.execute();
        }
        else if (request.getParameter("sa") != null)
        {
            Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
                "ID = " + m_Lexeme.getID());
            ch.setString("Syntactic_Args",   request.getParameter("sa"));
            ch.execute();
        }
        else if (request.getParameter("ls_save") != null)
        {
            Lexeme lexeme = new Lexeme(sql);
            
            if (m_Lexeme.getID() != -1)
            {
                lexeme = Lexeme.load(sql, m_Lexeme.getID());
            }
            
            lexeme.setCaused          (request.getParameter("ls_caused") != null);
            lexeme.setPunctual        (request.getParameter("ls_punct")  != null);
            lexeme.setHasResultState  (request.getParameter("ls_punct_result") != null &&
                                       request.getParameter("ls_punct_result").equals("1"));
            lexeme.setTelic           (request.getParameter("ls_telic") != null);
            lexeme.setDynamic         (request.getParameter("ls_dynamic") != null &&
                                       request.getParameter("ls_dynamic").equals("1"));
            lexeme.setHasEndpoint     (request.getParameter("ls_endpoint") != null &&
                                       request.getParameter("ls_endpoint").equals("1"));
            
            String pred = request.getParameter("ls_pred");
            if (pred != null && pred.equals(""))
            {
                pred = null;
            }
            lexeme.setPredicate(pred);
            
            pred = request.getParameter("ls_pred_2");
            if (pred != null && pred.equals(""))
            {
                pred = null;
            }
            lexeme.setResultPredicate(pred);

            String arg2 = request.getParameter("ls_arg_2");
            if (arg2 != null && arg2.equals(""))
            {
                arg2 = null;
            }
            lexeme.setResultPredicateArg(arg2);
            
            if (request.getParameter("ls_trel") != null)
            {
                if (request.getParameter("ls_trel").equals(""))
                {
                    lexeme.setThematicRelation(null);
                }
                else
                {
                    int i = Integer.parseInt(request.getParameter("ls_trel"));
                    lexeme.setThematicRelation(ThematicRelation.list()[i]);
                }
            }
            
            lexeme.save();
            m_Lexeme = lexeme;
        }
    }
    
    public class LogicalStructureList extends Lexeme.Visitor
    {
        private int exclude_domain_id;
        private List<String[]> m_Values = new ArrayList<String[]>();
        
        public LogicalStructureList(int exclude_domain_id, Lexeme root)
        {
            super(root);
            this.exclude_domain_id = exclude_domain_id;
        }
        
        protected void visit(Lexeme e, String parentPath) throws IOException 
        {
            if (e.getID() == exclude_domain_id) return;
            super.visit(e, parentPath);
        }
        
        protected void output(Lexeme e, String fullPath, String desc)
        throws IOException
        {
            if (desc.length() > 60)
            {
                desc = desc.substring(0, 60) + "...";
            }
            
            if (e.surface != null)
            {
                desc += ": " + e.surface + "";
            }
            
            String ls = e.getLogicalStructure();
            if (ls != null) 
            {
                desc += ": " + ls;
            }
            
            desc = desc.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");

            m_Values.add(new String[]{"" + e.getID(), fullPath + " " + desc});
        }
        
        public List<String[]> getValues()
        {
            return new ArrayList<String[]>(m_Values);
        }
    }

    public class LogicalStructureArray extends Lexeme.Visitor
    {
        private JspWriter out;
        private int exclude_domain_id;

        public LogicalStructureArray(JspWriter out, Lexeme root)
        {
            super(root);
            this.out = out;
        }

        protected void output(Lexeme e, String fullPath, String desc)
        throws IOException
        {
            String ls = e.getLogicalStructure();

            if (e.surface != null && ! e.surface.equals(""))
            {
                desc += ": " + e.surface + "";
            }
                        
            if (ls != null && ! ls.equals("")) 
            {
                desc += ": " + ls;
            }

            out.println("[ " + e.getID() + ", \"" + fullPath + " " + desc + 
                "\" ],");
        }       
    }

    public Lexeme getLexeme()
    {
        return m_Lexeme;
    }
    
    public List<String> getErrorMessages()
    {
        return new ArrayList<String>(m_ErrorMessages);
    }
}

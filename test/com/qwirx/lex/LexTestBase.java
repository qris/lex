package com.qwirx.lex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jemdros.EmdrosException;
import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.Table;
import jemdros.TableIterator;
import jemdros.TableRow;
import junit.framework.TestCase;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;
import com.qwirx.lex.morph.HebrewMorphemeGenerator;

public class LexTestBase extends TestCase
{
    private SqlDatabase m_SQL;
    private EmdrosDatabase m_Emdros;
    
    protected LexTestBase() throws Exception
    {
        m_SQL = Lex.getSqlDatabase("test");
        m_Emdros = Lex.getEmdrosDatabase("test", "test", m_SQL);
    }
    
    protected SqlDatabase getSql() { return m_SQL; }
    protected EmdrosDatabase getEmdros() { return m_Emdros; }
    
    protected Map<String, Integer> m_Location = new HashMap<String, Integer>();

    protected MatchedObject getNestedObject(Sheaf sheaf)
    throws EmdrosException
    {
        SheafConstIterator sci = sheaf.const_iterator();
        
        if (sci.hasNext()) 
        {
            Straw straw = sci.next();
            StrawConstIterator swci = straw.const_iterator();
            if (swci.hasNext()) 
            {
                return swci.next();
            }
        }

        return null;
    }
    
    protected MatchedObject getWord(int monad)
    throws Exception
    {
        HebrewMorphemeGenerator hmg = new HebrewMorphemeGenerator();
        String query = "SELECT ALL OBJECTS IN {" + monad + "} " +
            "WHERE [word GET " + hmg.getRequiredFeaturesString(true) + "]";

        Sheaf sheaf = getEmdros().getSheaf(query);
        MatchedObject word = getNestedObject(sheaf);
        
        Table table = getEmdros().getTable("SELECT OBJECTS HAVING MONADS IN {" + 
            monad + "} [ALL]");
        for (TableIterator ti = table.iterator(); ti.hasNext();)
        {
            TableRow row = ti.next();
            String type = row.getColumn(1);
            Integer id_d = Integer.valueOf(row.getColumn(3));
            m_Location.put(type, Integer.valueOf(id_d));
        }
        
        return word;
    }
}

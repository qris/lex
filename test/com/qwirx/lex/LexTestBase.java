package com.qwirx.lex;

import junit.framework.TestCase;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;

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
}

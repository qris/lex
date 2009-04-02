package com.qwirx.lex.controller;

import javax.servlet.http.HttpServletRequest;

import com.qwirx.db.sql.SqlDatabase;
import com.qwirx.lex.emdros.EmdrosDatabase;

public class ParseController extends ControllerBase
{
    public ParseController(HttpServletRequest request, EmdrosDatabase emdros,
        SqlDatabase sql, Navigator navigator)
    throws Exception
    {
        super(request, emdros, sql, navigator);
        loadClause();
    }
}

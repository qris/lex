package com.qwirx.lex.lexicon;

import java.sql.Connection;

import com.qwirx.db.sql.DbColumn;
import com.qwirx.db.sql.DbTable;

public class Lexicon
{
    public static void checkDatabase(Connection dbconn) throws Exception
    {
        new DbTable("lexicon_entries", "utf8",
            new DbColumn[]{
                new DbColumn("ID",        "INT(11)",     false, 
                        true, true),
                new DbColumn("Lexeme",    "VARCHAR(40)", true),
                new DbColumn("Structure", "VARCHAR(160)", true),
                new DbColumn("Domain_Parent_ID", "INT(11)",
                        true),
                new DbColumn("Domain_Label", "VARCHAR(40)", 
                        true),
                new DbColumn("Domain_Desc", "VARCHAR(160)", 
                        true),
                new DbColumn("Symbol",      "VARCHAR(40)", true),
                new DbColumn("Gloss",       "VARCHAR(40)", true),
                new DbColumn("Syntactic_Args", "INT(11)",  false),
                new DbColumn("Aktionsart",  
                        "ENUM('NONE','INGR','SEML','BECOME')", false),
                new DbColumn("Active",      "ENUM('0','1')", false),
                new DbColumn("Pred_Enable", "ENUM('0','1')", false),
                new DbColumn("Predicate",   "VARCHAR(40)", true),
                new DbColumn("Arguments",   "ENUM('','X','XY')", false),
                new DbColumn("Become",      "ENUM('0','1')", false),
                new DbColumn("Become_Pred", "VARCHAR(40)", false),
                new DbColumn("Become_Args", "ENUM('','Y','ZX')", false),
                new DbColumn("Caused",      "ENUM('0','1')", false),
                new DbColumn("Caused_Aktionsart",  
                        "ENUM('NONE','INGR','SEML','BECOME')", false),
                new DbColumn("Caused_Active",      "ENUM('0','1')", false),
                new DbColumn("Caused_Pred_Enable", "ENUM('0','1')", false),
                new DbColumn("Caused_Predicate",   "VARCHAR(40)", false),
                new DbColumn("Caused_Arguments",   "ENUM('','X','XY')", false),
                new DbColumn("Caused_Become",      "ENUM('0','1')", false),
                new DbColumn("Caused_Become_Pred", "VARCHAR(40)", false),
                new DbColumn("Caused_Become_Args", "ENUM('','Y','ZX')", false),
                new DbColumn("Punctual",           "ENUM('0','1')", false),
                new DbColumn("Has_Result_State",   "ENUM('0','1')", false),
                new DbColumn("Telic",              "ENUM('0','1')", false),
                new DbColumn("Thematic_Relation",  "VARCHAR(11)", true),
                new DbColumn("Dynamic",            "ENUM('0','1')", false),
                new DbColumn("Has_Endpoint",       "ENUM('0','1')", false),
                new DbColumn("Result_Predicate",   "VARCHAR(40)", true),
                new DbColumn("Result_Predicate_Arg",
                        "ENUM('x','y','x,y')", true),
            }
        ).check(dbconn, true);

        new DbTable("lexicon_variables", "utf8",
            new DbColumn[]{
                new DbColumn("ID",        "INT(11)", false, 
                        true, true),
                new DbColumn("Lexeme_ID", "INT(11)", false),
                new DbColumn("Name",  "VARCHAR(20)", false),
                new DbColumn("Value", "VARCHAR(40)", false),
            }
        ).check(dbconn, true);
    }
}

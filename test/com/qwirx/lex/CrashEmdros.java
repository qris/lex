/*
 * Created on 04-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import jemdros.EmdrosEnv;
import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.TableRow;
import jemdros.eCharsets;
import jemdros.eOutputKind;
import junit.framework.TestCase;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CrashEmdros extends TestCase 
{
    /*
	public void testCrashWithZeroColumn() 
    {
		System.load("/usr/local/lib/emdros/libjemdros.so");
		
		EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
				eCharsets.kCSISO_8859_1, "localhost", "emdf", 
				"changeme", "wihebrew1202");

		String query = "GET FEATURES argument_name " +
				"FROM OBJECT WITH ID_D = 69977 [phrase]";

		boolean[] bCompilerResult = new boolean[1];
		env.executeString(query, bCompilerResult, false, false);
		
		Table t = env.takeOverTable();
		TableRow tr = t.iterator().next();
		tr.getColumn(0);
	}

    public void testCrashWithColumnTooLarge() 
    {
        System.load("/usr/local/lib/emdros/libjemdros.so");
        
        EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
                eCharsets.kCSISO_8859_1, "localhost", "emdf", 
                "changeme", "wihebrew1202");

        String query = "GET FEATURES argument_name " +
                "FROM OBJECT WITH ID_D = 69977 [phrase]";

        boolean[] bCompilerResult = new boolean[1];
        env.executeString(query, bCompilerResult, false, false);
        
        Table t = env.takeOverTable();
        TableRow tr = t.iterator().next();
        tr.getColumn(3);
    }
    */

    public void testCrashWithGetStringOnFeature() 
    throws Exception
    {
        System.load("/usr/local/lib/emdros/libjemdros.so");
        
        EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
                eCharsets.kCSISO_8859_1, "localhost", "emdf", 
                "changeme", "wihebrew1202");

        String query = "SELECT ALL OBJECTS IN {1-28735} " +
                "WHERE [phrase self = 69977 GET phrase_type]";

        boolean[] bCompilerResult = new boolean[1];
        env.executeString(query, bCompilerResult, false, false);
        
        Sheaf sheaf = env.takeOverSheaf();
        SheafConstIterator sci = sheaf.const_iterator();
        Straw straw = sci.next();
        MatchedObject mo = straw.const_iterator().next();
        String str = mo.getEMdFValue("phrase_type").getString();
    }

	public static void main(String[] args) throws Exception
    {
		// junit.textui.TestRunner.run(CrashEmdros.class);
		new CrashEmdros().testCrashWithGetStringOnFeature();
	}

}

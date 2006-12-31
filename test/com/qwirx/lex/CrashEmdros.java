/*
 * Created on 04-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import jemdros.EmdrosEnv;
import jemdros.Table;
import jemdros.TableRow;
import jemdros.eCharsets;
import jemdros.eOutputKind;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CrashEmdros /* extends TestCase */ {

	public void testCrash() {
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
	
	public static void main(String[] args) {
		// junit.textui.TestRunner.run(CrashEmdros.class);
		new CrashEmdros().testCrash();
	}

}

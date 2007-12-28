/*
 * Created on 04-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.net.URL;

import jemdros.EmdrosEnv;
import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.Table;
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
    private boolean m_LibraryLoaded = false;
    
    private void loadLibrary()
    {
        if (m_LibraryLoaded)
        {
            return;
        }
        
        URL url = Preloader.class.getResource("/../lib/");
        
        if (url == null)
        {
            url = Preloader.class.getResource("/../jsp/WEB-INF/lib/");
        }
        
        if (url == null)
        {
            url = Preloader.class.getResource("/");
        }

        String path = url.getPath() + "libjemdros.so";
        if (! new java.io.File(path).exists())
        {
            path = "/usr/local/lib/emdros/libjemdros.so";
        }
        if (! new java.io.File(path).exists())
        {
            path = "/home/chris/tomcat/common/lib/libjemdros.so";
        }
        
        if (new java.io.File(path).exists())
        {
            System.out.println("Library path: "+path);
            System.load(path);
        }
        else
        {
            System.out.println("Library not found, trying System.loadLibrary()");
            System.loadLibrary("libjemdros.so");
        }
        
        m_LibraryLoaded = true;
    }

	public void testCrashWithZeroColumn() throws Exception
    {
		loadLibrary();
		
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

    public void testCrashWithColumnTooLarge() throws Exception
    {
        loadLibrary();
        
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

    public void testCrashJavaWithGetStringOnFeature1() 
    throws Exception
    {
        loadLibrary();
        
        EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
                eCharsets.kCSISO_8859_1, "localhost", "emdf", 
                "changeme", "wihebrew");

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
    
    public void testAbortJavaWithSheafConstIteratorNext() throws Exception
    {
        loadLibrary();
        
        EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
            eCharsets.kCSISO_8859_1, "localhost", "emdf", "changeme", 
            "wihebrew");

        boolean[] bCompilerResult = new boolean[1];

        assertTrue(env.executeString(
            "SELECT ALL OBJECTS IN " +
            "{1-1000000} " +
            "WHERE " +
            "[verse GET book, chapter, verse " +
            " [clause self = 1 " +
            "  [word GET text, graphical_word, lexeme]"+
            " ]"+
            "]", bCompilerResult, false, false));

        Sheaf sheaf = env.takeOverSheaf();
        
        SheafConstIterator sci = sheaf.const_iterator();
        Straw straw = sci.next();
        /*
        StrawConstIterator swci = straw.const_iterator();
        MatchedObject verse = swci.next();
        
        verse.getEMdFValue("book").getString();
        verse.getEMdFValue("chapter").getInt();
        verse.getEMdFValue("verse").getInt();

        sci = verse.getSheaf().const_iterator();
        straw = sci.next();
        swci = straw.const_iterator();
        MatchedObject clause = swci.next();
        */
    }

    public void testCrashJavaWithGetStringOnFeature2() throws Exception
    {
        loadLibrary();

        EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
            eCharsets.kCSISO_8859_1, "localhost", "emdf", "changeme", 
            "wihebrew");

        boolean[] bCompilerResult = new boolean[1];

        assertTrue(env.executeString(
            "SELECT ALL OBJECTS IN " +
            "{1-1000000} " +
            "WHERE " +
            "[verse GET book, chapter, verse " +
            " [clause self = 1324989 " +
            "  [word GET text, graphical_word, lexeme]"+
            " ]"+
            "]", bCompilerResult, false, false));

        Sheaf sheaf = env.takeOverSheaf();

        SheafConstIterator sci = sheaf.const_iterator();
        Straw straw = sci.next();
        StrawConstIterator swci = straw.const_iterator();
        MatchedObject verse = swci.next();
        
        verse.getEMdFValue("book").getString();
        /*
        verse.getEMdFValue("chapter").getInt();
        verse.getEMdFValue("verse").getInt(); 

        sci = verse.getSheaf().const_iterator();
        straw = sci.next();
        swci = straw.const_iterator();
        MatchedObject clause = swci.next();
        */
    }

	public static void main(String[] args) throws Exception
    {
		// junit.textui.TestRunner.run(CrashEmdros.class);
		new CrashEmdros().testCrashJavaWithGetStringOnFeature1();
	}

}

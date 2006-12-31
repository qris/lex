// TestEmdros.java
import java.util.*;

import jemdros.*;


public class TestEmdros {
    static String NEWLINE;
    static {
	// The following doesn't seem to work...
	//	String newlibrarypath = System.getProperty("java.library.path",".") + ":/usr/local/lib/emdros";
	//	System.setProperty("java.library.path", newlibrarypath);
	//	System.out.println(System.getProperty("java.library.path","."));
	//	System.loadLibrary("jemdros");

	// So we do the following instead...

        // Get OS name
        String osName = System.getProperty("os.name");
        System.out.println(osName);
        if (osName.matches(".*[Ww]in.*")) {
	    // Substitute your path
	    System.load("c:\\emdros\\lib\\jemdros.dll");
	} else {
	    // Substitute your path
	    System.load("/usr/local/lib/emdros/libjemdros.so");
	}

	// Get newline character(s)
	NEWLINE = System.getProperty("line.separator");
     }

    static CEmdrosEnv env;

    abstract static class EMdFObject {
	protected CSet_of_monad_ms monads;
	protected int object_id_d;
	EMdFObject() {
	    monads = new CSet_of_monad_ms();
	    object_id_d = jemdros.NIL;
	}
	EMdFObject(int id_d) {
	    monads = new CSet_of_monad_ms();
	    object_id_d = id_d;
	}
	public void addMonad(int monad) {
	    monads.add(monad);
	}
	CSet_of_monad_ms getMonads() { return monads; };
	int getID_D() { return object_id_d; };
	void setID_D(int oid) { object_id_d = oid; };
	final boolean createObject() {	
	    // Build query
	    String query;
	    query = "CREATE OBJECT" + NEWLINE
		+ "FROM MONADS = " + monads.ToString() + NEWLINE
		+ "[" + objectType() + NEWLINE
		+ featureString()
		+ "]" + NEWLINE
		+ "GO";
	    
	    // Execute query
	    if (!TestEmdros.execute_query(query, false)) 
		return false;

	    // Get result
	    CMQLResult result = env.getResult();
	    if (!result.isTable()) {
		System.out.println("Error: Was expecting table as result.");
		return false;
	    }

	    // Get first column of first row
	    String object_id_d_str = result.get_column(result.iterator(), 1);
	    
	    // Set object_id_d
	    try {
		object_id_d = Integer.parseInt(object_id_d_str);
	    } catch (NumberFormatException e) {
		System.out.println("Error: object_id_d_str was '" + object_id_d_str + "', which threw a NumberFormatException.");
		return false;
	    }
	    
	    // If we got this far, there were no errors
	    return true;
	}
	abstract String objectType();
	abstract String featureString();
    }

    static class Phrase extends EMdFObject {
	protected String phraseType;
	Phrase() {
	    phraseType = "Not yet set";
	}
	Phrase(int oid) {
	    super(oid);
	    phraseType = "Not yet set";
	}
	Phrase(String phrType) {
	    phraseType = phrType;
	}
	String getPhraseType() { return phraseType; };
	void setPhraseType(String phrasetype) { phraseType = phrasetype; };
	String objectType() { return "Phrase"; }
	String featureString() { 
	    String result;
	    result = "  phrase_type := " + phraseType + ";" + NEWLINE;
	    return result;
	}
    }

    static class Word extends EMdFObject {
	protected String surface;
	protected String psp;
	Word() {
	    surface = "Not yet set";
	    psp = "Not yet set";
	}
	Word(int oid) {
	    super(oid);
	    surface = "Not yet set";
	    psp = "Not yet set";
	}
	Word(String surf, String p) {
	    surface = surf;
	    psp = p;
	}
	String getSurface() { return surface; };
	void setSurface(String surf) { surface = surf; };
	String getPsp() { return psp; };
	void setPsp(String p) { psp = p; };
	String objectType() { return "Word"; }
	String featureString() { 
	    String result;
	    result = "  surface := \"" + surface + "\";" + NEWLINE
		+ "  psp := " + psp + ";" + NEWLINE;
	    return result;
	}
    }

    private static void show_DB_error() {
	System.out.println("DB Error:");
	String strDBError;
	strDBError = env.get_DB_error();
	System.out.println(strDBError);
    }

    private static boolean execute_query(String query, boolean showResult) {
	// Show query
	if (showResult) {
	    System.out.println("Executing query: '" + query + "'");
	}

	
	// Execute query
	boolean[] bCompilerResult;
	bCompilerResult = new boolean[1];
	boolean bDBResult;
	bDBResult = env.execute_string(query, bCompilerResult, showResult, false);

	// Show success-info
	if (showResult) {
	    System.out.println("DB sucess: " + bDBResult + ", Compiler success: " + bCompilerResult[0]);
	}

	// Show DB error, if any
	if (!bDBResult) {
	    show_DB_error();
	}

	// Show compiler error, if any
	if (!bCompilerResult[0]) {
	    System.out.println("Compiler error:");
	    String strCompilerError;
	    strCompilerError = env.get_compiler_error();
	    System.out.println(strCompilerError);
	    System.out.println("Query was:'");
	    System.out.println(query + "'");
	}

	// Return success-info
	return !(!bCompilerResult[0] || !bDBResult);
    }

    private static boolean execute_query(String query, String success, boolean showResult) {
	boolean bResult = execute_query(query, showResult);
	if (bResult) {
	    System.out.println("SUCCESS in " + success);
	} else {
	    System.out.println("FAILURE in " + success);
	}
	return bResult;
    }

    private static boolean create_database() {

	// Create database
	if (!execute_query("CREATE DATABASE EMDF6 GO", "creating database", false)) 
	    return false;


	// Create enumeration psp_t
	String query;
	query = "CREATE ENUMERATION psp_t = {" + NEWLINE
	    + "  psp_None, Noun, Verb, ProperNoun, Adjective, Adverb, Preposition," + NEWLINE
	    + "  Demonstrative, Pronoun, Article" + NEWLINE
	    + "}" + NEWLINE
	    + "GO";
	if (!execute_query(query, "creating psp_t enumeration", false))
	    return false;

	// Create enumeration phrase_type_t
	query = "CREATE ENUMERATION phrase_type_t = {" + NEWLINE
	    + "  pt_None, NP, VP, AP, AdvP, PP" + NEWLINE
	    + "}" + NEWLINE
	    + "GO";
	if (!execute_query(query, "creating phrase_type_t enumeration", false))
	    return false;

	// Create object type Word
	query = "CREATE OBJECT TYPE" + NEWLINE
	    + "[Word" + NEWLINE
	    + "  surface : ASCII;" + NEWLINE
	    + "  psp : psp_t;" + NEWLINE
	    + "]" + NEWLINE
	    + "GO";
	if (!execute_query(query, "creating object type Word", false))
	    return false;

	// Create object type Clause
	query = "CREATE OBJECT TYPE" + NEWLINE
	    + "[Phrase" + NEWLINE
	    + "   phrase_type : phrase_type_t;" + NEWLINE
	    + "]" + NEWLINE
	    + "GO";
	if (!execute_query(query, "creating object type Phrase", false))
	    return false;

	// If we got this far, there were no errors
	return true;
    }

    private static boolean printPhrase(CMatched_object mo, HashMap phraseHM, HashMap wordHM) {
	// Get object id_d
	int object_id_d = mo.get_id_d();
	Integer myIntOid = new Integer(object_id_d);

	// Get phrase and check that it isn't null
	Phrase phr = (Phrase) phraseHM.get(myIntOid);
	if (phr == null) {
	    System.out.println("Error: phr was null.");
	    return false;
	}
	String phrase_type = phr.getPhraseType();
	System.out.print("[" + phrase_type + " ");
	if (!printSheaf(mo.get_sheaf(), phraseHM, wordHM))
	    return false;
	System.out.println(phrase_type + "]");
	return true;
    }

    private static boolean printWord(CMatched_object mo, HashMap wordHM) {
	// Get object id_d
	int object_id_d = mo.get_id_d();
	Integer myIntOid = new Integer(object_id_d);

	// Get word and check that it isn't null
	Word w = (Word) wordHM.get(myIntOid);
	if (w == null) {
	    System.out.println("Error: w was null.");
	    return false;
	}

	// Get surface
	String surface = w.getSurface();
	System.out.print(surface + " ");
	return true;
    }

    private static boolean printMatchedObject(CMatched_object mo, HashMap phraseHM, HashMap wordHM) {
	if (mo.get_kind() == jemdros.kMOKID_D) {
	    // So the matched object matched an object block.
	    String object_type_name = mo.getObjectTypeName();
	    if (object_type_name.equals("phrase")) {
		return printPhrase(mo, phraseHM, wordHM);
	    } else if (object_type_name.equals("word")) {
		return printWord(mo, wordHM);
	    } else {
		System.out.println("Error: object_type_name was '" + object_type_name + "', which wasn't either 'phrase' or 'word'.");
		return false;
	    }
	} else {
	    // We shouldn't get here: All the mo's in this
	    // example should be of kind ID_D
	    System.out.println("ERROR: mo.get_kind() wasn't kMOKID_D.");
	    return false;
	}
    }

    private static boolean printStraw(CStraw straw, HashMap phraseHM, HashMap wordHM) {
	StrawConstIterator sci = straw.const_iterator();
	while (sci.hasNext()) {
	    if (!printMatchedObject(sci.next(), phraseHM, wordHM)) 
		return false;
	}
	return true;
    }

    private static boolean printSheaf(CSheaf sheaf, HashMap phraseHM, HashMap wordHM) {
	SheafConstIterator sci = sheaf.const_iterator();
	while (sci.hasNext()) {
	    CStraw straw = sci.next();
	    if (!printStraw(straw, phraseHM, wordHM)) 
		return false;
	}

	// If we got this far, there were no errors
	return true;
    }

    private static boolean gatherPhrase(CMatched_object mo, HashMap phraseHM, HashMap wordHM) {
	int object_id_d = mo.get_id_d();
	Integer oid = new Integer(object_id_d);
	Phrase phr = new Phrase(object_id_d);
	phraseHM.put(oid, phr);
	if (!gatherSheaf(mo.get_sheaf(), phraseHM, wordHM))
	    return false;
	return true;
    }

    private static boolean gatherWord(CMatched_object mo, HashMap wordHM) {
	int object_id_d = mo.get_id_d();
	Integer oid = new Integer(object_id_d);
	Word w = new Word(object_id_d);
	wordHM.put(oid, w);
	return true;
    }

    private static boolean gatherMatchedObject(CMatched_object mo, HashMap phraseHM, HashMap wordHM) {
	if (mo.get_kind() == jemdros.kMOKID_D) {
	    // So the matched object matched an object block.
	    String object_type_name = mo.getObjectTypeName();
	    if (object_type_name.equals("phrase")) {
		return gatherPhrase(mo, phraseHM, wordHM);
	    } else if (object_type_name.equals("word")) {
		return gatherWord(mo, wordHM);
	    } else {
		System.out.println("Error: object_type_name was '" + object_type_name + "', which wasn't either 'phrase' or 'word'.");
		return false;
	    }
	} else {
	    // We shouldn't get here: All the mo's in this
	    // example should be of kind ID_D
	    System.out.println("ERROR: mo.get_kind() wasn't kMOKID_D.");
	    return false;
	}
    }

    private static boolean gatherStraw(CStraw straw, HashMap phraseHM, HashMap wordHM) {
	StrawConstIterator sci = straw.const_iterator();
	while (sci.hasNext()) {
	    if (!gatherMatchedObject(sci.next(), phraseHM, wordHM)) 
		return false;
	}
	return true;
    }

    private static boolean gatherSheaf(CSheaf sheaf, HashMap phraseHM, HashMap wordHM) {
	SheafConstIterator sci = sheaf.const_iterator();
	while (sci.hasNext()) {
	    CStraw straw = sci.next();
	    if (!gatherStraw(straw, phraseHM, wordHM)) 
		return false;
	}

	// If we got this far, there were no errors
	return true;
    }

    private static String getID_DString(HashMap HM) {
	// Get set of object id_ds
	Set oid_set = HM.keySet();

	// Make it an array
	Object intArray[] = oid_set.toArray();

	// Declare string result
	String result = new String();

	for (int i = 0; i < intArray.length; ) {
	    // Get integer object
	    Integer myInt = (Integer) intArray[i];

	    // Add it to the query
	    result += myInt;

	    // Advance i
	    i++;

	    // See if we should output comma
	    if (i < intArray.length) {
		result += ", ";
	    }

	    // See if we should output newline
	    if ((i % 5) == 4) {
		result += NEWLINE;
	    }
	}

	return result;

    }

    private static boolean fillPhrases(HashMap phraseHM) {
	// get and checkID_D string
	String id_dString = getID_DString(phraseHM);
	if (id_dString.equals("")) {
	    System.out.println("Error: there were no id_ds.");
	    return false;
	}

	// Build query
	String query = "GET FEATURES phrase_type" + NEWLINE
              + "FROM OBJECTS" + NEWLINE
	    + "WITH ID_DS = " + id_dString + NEWLINE
	    + "[Phrase]" + NEWLINE
	    + "GO";

	// execute query
	if (!execute_query(query, false)) {
	    return false;
	}

	// Take over result
	CMQLResult result = env.takeOverResult();

	// Check that result is table
	if (!result.isTable()) {
	    System.out.println("Error: result wasn't a table.");
	    return false;
	}

	// Iterate over table
	CTable table = result.get_table();
	TableIterator it = table.iterator();
	while (it.hasNext()) {
	    // Get current table row
	    TableRow trow = it.current();

	    // Get object_id_d as myInt
	    Integer myInt = null;
	    String oid = trow.getColumn(1);
	    try {
		myInt = new Integer(oid);
	    } catch (NumberFormatException e) {
		System.out.println("Error: oid was '" + oid + "', which threw a NumberFormatException.");
		return false;
	    }
	    
	    // Get phrase type
	    boolean bDBErrorArray[] = new boolean[1];
	    String phrase_type = getPhraseTypeFromStringAsInt(trow.getColumn(2), bDBErrorArray);
	    if (!bDBErrorArray[0]) {
		System.out.println("Error: Could not get phrase-type due to DB error.");
		return false;
	    }
	    
	    // Get phrase object
	    Phrase phr = (Phrase) phraseHM.get(myInt);
	    
	    // Update phrase
	    phr.setPhraseType(phrase_type);
	    
	    // Advance iterator
	    it.next();
	}
	

	
	// If we got this far, there were no errors
	return true;
    }
    
    private static boolean fillWords(HashMap wordHM) {
	// Build query
	String query = "GET FEATURES surface, psp" + NEWLINE
	    + "FROM OBJECTS" + NEWLINE
	    + "WITH ID_DS = " + getID_DString(wordHM) + NEWLINE
	    + "[Word]" + NEWLINE
	    + "GO";

	// execute query
	if (!execute_query(query, false)) {
	    return false;
	}
	
	// Get statement and result
	CMQLResult result = env.takeOverResult();
	
	// Check that result is table
	if (!result.isTable()) {
	    System.out.println("Error: result wasn't a table.");
	    return false;
	}
	
	// Iterate over table
	CTable table = result.get_table();
	TableIterator it = table.iterator();
	while (it.hasNext()) {
	    // Get current table row
	    TableRow trow = it.current();

	    // Get object_id_d as myInt
	    Integer myInt = null;
	    String oid = trow.getColumn(1);
	    try {
		myInt = new Integer(oid);
	    } catch (NumberFormatException e) {
		System.out.println("Error: oid was '" + oid + "', which threw a NumberFormatException.");
		return false;
	    }
	    
	    
	    
	    // Get surface
	    String surface = trow.getColumn(2);
	    
	    // Get psp
	    String psp = trow.getColumn(3);
	    
	    // Get word object
	    Word w = (Word) wordHM.get(myInt);

	    // Update word
	    w.setSurface(surface);
	    w.setPsp(psp);

	    // Advance iterator
	    it.next();
	}



	// If we got this far, there were no errors
	return true;
    }

    private static boolean harvestSheaf(CSheaf sheaf, HashMap phraseHM, HashMap wordHM) {
	// Gather object id_ds and empty objects
	if (!gatherSheaf(sheaf, phraseHM, wordHM))
	    return false;

	// Test phraseHM
	if (phraseHM.size() == 0) {
	    System.out.println("Error: there were no phrases.");
	    return false;
	}

	// Test wordHM
	if (wordHM.size() == 0) {
	    System.out.println("Error: there were no words.");
	    return false;
	}

	// Fill phase objects with values
	if (!fillPhrases(phraseHM))
	    return false;

	// Fill word objects with values
	if (!fillWords(wordHM)) 
	    return false;

	return true;
    }

    private static boolean query_phrases(String phrase_type) {
	// Print opening message
	System.out.println("Attempting to get all phrases of type " + phrase_type);

	// Create query-string
	String query;
	query = "SELECT ALL OBJECTS" + NEWLINE
	    + "WHERE" + NEWLINE
	    + "[Phrase" + NEWLINE
	    + "  phrase_type = " + phrase_type + NEWLINE
	    + "  [Word]" + NEWLINE
	    + "]" + NEWLINE
	    + "GO";

	// Execute query
	execute_query(query, false);

	// Take over sheaf
	CSheaf sheaf = env.takeOverSheaf();

	// Harvest sheaf
	HashMap phraseHM = new HashMap();
	HashMap wordHM = new HashMap();
	harvestSheaf(sheaf, phraseHM, wordHM);

	// Print sheaf
	printSheaf(sheaf, phraseHM, wordHM);

	// If we got this far, there were no errors
	System.out.println("SUCCESS in querying phrase-type " + phrase_type);
	return true;
    }

    private static boolean query_enumeration(String enum) {
	// Print opening message
	System.out.println("Attempting to get table of enumeration constants in " + enum);
	// If we got this far, there were no errors
	return true;
    }

    private static boolean query_database() {
	if (!query_phrases("NP")) 
	    return false;

	if (!query_phrases("AP")) 
	    return false;

	if (!query_enumeration("psp_t")) {
	    return false;
	}

	// If we got this far, there were no errors
	return true;
    }

    private static boolean drop_database() {
	return execute_query("DROP DATABASE EMDF6 GO", "dropping database", false);
    }

    private static boolean fill_database_single_word(String surface, String psp, int monad) {
	Word wd = new Word(surface, psp);
	wd.addMonad(monad);
	wd.createObject();
	
	// If we got this far, there were no errors
	return true;
    }

    
    
    private static boolean fill_database(String words) {
	String[] word_array = words.split("[ \n\r\t]+");
	int monad = 1;
	Stack phrase_stack = new Stack();
	for(int i = 0; i < word_array.length; i += 2) {
	    String w1 = word_array[i];
	    String w2 = word_array[i+1];
	    if (w1.equals("[")) {
		// Open phrase
		Phrase phrase = new Phrase(w2);
		phrase_stack.push(phrase);
	    } else if (w1.equals("]")) {
		Phrase phrase = (Phrase) phrase_stack.pop();
		if (!phrase.getPhraseType().equals(w2)) {
		    System.out.println("Error in word string: Closing prase type was '" 
				       + w2 + "', expected '"
				       + phrase.getPhraseType() + "'");
		    return false;
		}
		if (!phrase.createObject()) {
		    return false;
		}
	    } else {
		// Make word
		if (!fill_database_single_word(w1, w2, monad))
		    return false;

		// Add monad to all phrases
		for (int phrindex = 0; 
		     phrindex < phrase_stack.size(); 
		     ++phrindex) {
		    Phrase phrase = (Phrase) phrase_stack.get(phrindex);
		    phrase.addMonad(monad);
		}

		// Increment monad
		++monad;
	    }
	}
	return true;
    }

    private static String getPhraseTypeFromStringAsInt(String ptin, boolean bDBErrorArray[]) {

	// Convert ptint to value integer
	int value;
	try {
	    value = Integer.parseInt(ptin);
	} catch (NumberFormatException e) {
	    System.out.println("Error: ptint '" + ptin + "', which threw a NumberFormatException.");
	    return "";
	}

	// Ask DB
	String ptout = env.GetEnumConstNameFromValue(value, "phrase_type_t", bDBErrorArray);

	// Return "" if there was a DB error
	if (!bDBErrorArray[0]) {
	    return "";
	}

	// Return result
	return ptout;
    }

    public static void main(String argv[]) {
	env = new CEmdrosEnv(jemdros.kOKConsole, jemdros.kCSISO_8859_1, "localhost", "emdf", "changeme", "emdf");
	boolean bConnectionOk = env.ConnectionOk();
	if (!bConnectionOk) {
	    show_DB_error();
	    System.out.println("Connection could not be established.");
	    return;
	}
	System.out.println("Connection established.");
	
	// Create database
	if (!create_database()) {
	    drop_database();
	    return;
	}
	
	// Fill database
	String words;
	words = "[ NP [ NP The Article door Noun ] NP "
	    + "[ PP on Preposition [ NP the Article East ProperNoun ] NP ] PP ] NP "
	    + "[ VP was Verb [ AP blue. Adjective ] AP ] VP ";
	
	if (!fill_database(words)) {
	    System.out.println("FAILURE in creating objects");
	    drop_database();
	    return;
	} else {
	    System.out.println("SUCCESS in creating objects");
	}
	
	// Attempt querying
	if (!query_database()) {
	    System.out.println("FAILURE in querying database");
	    drop_database();
	    return;
	} else {
	    System.out.println("SUCCESS in querying database");
	}
	
	
	// Drop database
	drop_database();
	
      // Print success message
	System.out.println("All tests completed successfully.");
    }
}

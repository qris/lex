<%@   page import="java.util.Map"
%><%@ page import="com.qwirx.lex.*"
%><%@ page import="com.qwirx.lex.emdros.*"
%><%@ page import="com.qwirx.crosswire.kjv.KJV"
%><%@ page import="jemdros.*"
%><%@ page import="org.crosswire.jsword.book.*"
%><%

	String pathInfo = request.getPathInfo();
	// String clauseIdString = pathInfo.substring(1, pathInfo.indexOf('/', 1));
	String clauseIdString = request.getParameter("clause");
	EmdrosDatabase emdros = null;
		
	String username = request.getRemoteUser();
	if (username == null)
	{
		username = "anonymous";
	}
	String hostname = request.getRemoteAddr();

	try
	{
		emdros = Lex.getEmdrosDatabase(username, hostname);
		int min_m = emdros.getMinM(), max_m = emdros.getMaxM();

        Sheaf sheaf = emdros.getSheaf
        (
            "SELECT ALL OBJECTS IN " +
            "{" + emdros.getMinM() + "-" + emdros.getMaxM() + "} " +
            "WHERE " +
            "[verse GET book, chapter, verse " +
			" [clause self = " + clauseIdString + " " +
            "  [word GET phrase_dependent_part_of_speech, person, gender, " +
            "            number, state, wordnet_gloss, lexeme, tense, stem, " +
            "            graphical_preformative, graphical_root_formation, " +
            "            graphical_lexeme, graphical_verbal_ending, " +
            "            graphical_nominal_ending, graphical_pron_suffix]" +
            " ]"+
            "]"
        );

        SheafConstIterator sci = sheaf.const_iterator();
		if (!sci.hasNext()) return;
        Straw straw = sci.next();
        StrawConstIterator swci = straw.const_iterator();
		if (!swci.hasNext()) return;
        MatchedObject verse = swci.next();
        
        Map bookNumToNameMap = emdros.getEnumerationConstants("book_name_e", 
            false);

        String bookName = (String)bookNumToNameMap.get(
            "" + verse.getEMdFValue("book").getEnum());

        BookData verseData = KJV.getVerse(emdros, bookName,
            verse.getEMdFValue("chapter").getInt(),
            verse.getEMdFValue("verse").getInt()); 

        sci = verse.getSheaf().const_iterator();
		if (!sci.hasNext()) return;
        straw = sci.next();
        swci = straw.const_iterator();
		if (!swci.hasNext()) return;
        MatchedObject clause = swci.next();

		response.setContentType("text/x-gen; charset=UTF-8");
		response.setHeader("Content-disposition", 
			"attachment; filename=export.gen");
		response.getWriter().print(
			new GenExporter().export(clause, verseData, sql));
	}
	finally
	{
		if (emdros != null)
		{
			Lex.putEmdrosDatabase(emdros);
		}
	}
%>
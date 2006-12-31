<%@ page import="java.util.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.ontology.*" %>
<%@ page import="com.qwirx.lex.wordnet.*" %>
<%@ page import="net.didion.jwnl.data.POS" %>
<%@ page import="net.didion.jwnl.data.Synset" %>
<html>
<head>
<title>Lex: Edit Clause</title>
<script type="text/javascript"><!--

	function enableEditButton()
	{
		var lsselect = document.forms.changels.lsid;
		var editform = document.forms.editls;
		if (editform == null || editform.submit == null)
		{
			return;
		}
			
		var sellsid  = -1;
		if (lsselect.selectedIndex > 0)
		{
			sellsid = lsselect.options[lsselect.selectedIndex].value;
		}	
		
		editform.submit.disabled = (sellsid != editform.lsid.value);
		return true;
	}

	function enableChangeButton(button, oldValue, selectBox)
	{
		var newValue = selectBox.options[selectBox.selectedIndex].value;
		button.disabled = (newValue == oldValue);
		return true;
	}
	
//--></script>
<link rel="stylesheet" href="style.css" />
</head>
<style type="text/css">
	TABLE.tree TD {
		text-align: center;
	}
	div.topmenu a.clause_jsp <%@ include file="hilite.inc" %>
</style>
<body onLoad="enableEditButton()">

<%@ include file="header.jsp" %>
<%@ include file="auth.jsp" %>

<%
	Wordnet wordnet = Wordnet.getInstance();
%>

<form name="nav" method="get">
<table>
<tr bgcolor="#FFCCCC"><th colspan=4>Navigator</th></tr>
<tr bgcolor="#FFEEEE">
  <th>Book</th>
  <th>Chapter</th>
  <th>Verse</th>
  <th>Clause</th>
</tr>
<tr bgcolor="#FFCCCC">
  <td>
	<select name="book" onChange="document.forms.nav.submit()">
<%

	int selBookNum = 1;
	
	{
		Integer sessionBookNum = (Integer)( session.getAttribute("bookNum") );
		if (sessionBookNum != null) {
			selBookNum = sessionBookNum.intValue();
		}
	}
	
	try { 
		int newBookNum = Integer.parseInt(request.getParameter("book"));
		selBookNum = newBookNum;
	} catch (Exception e) { /* ignore it and use default book */ }
	
	{
		boolean foundBook = false;
	
		Sheaf bookSheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " + 
			emdros.getMonadSet(userTextAccess, min_m, max_m) +
			" WHERE [book GET book, book_number]"
		);

		SheafConstIterator sci = bookSheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject book = straw.const_iterator().next();
			
			int thisBookNum = book.getEMdFValue("book_number").getInt();
			if (thisBookNum == selBookNum)
			{
				foundBook = true;
				SetOfMonads som = new SetOfMonads();
				book.getSOM(som, false);
				min_m = som.first();
				max_m = som.last();
			}
				
			%>
			<option <%=
				thisBookNum == selBookNum ? "SELECTED" : ""
			%> value="<%= 
				book.getEMdFValue("book_number").getInt() 
			%>"><%= 
				book.getEMdFValue("book").toString()
			%><%
		}

		if (foundBook)
			session.setAttribute("bookNum", new Integer(selBookNum));
	}
	
%>
	</select>
  </td>
  <td>
	<select name="chapter" onChange="document.forms.nav.submit()">
<%
	int selChapNum = 1;

	{
		Integer sessionChapterNum = 
			(Integer)( session.getAttribute("chapterNum") );
		if (sessionChapterNum != null)
			selChapNum = sessionChapterNum.intValue();
	}

	try 
	{
		int newChapNum = Integer.parseInt(request.getParameter("chapter"));
		selChapNum = newChapNum;
	} 
	catch (Exception e) { /* ignore it and use default chapter */ }
	
	{
		boolean foundChapter = false;

		Sheaf sheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " +
			emdros.getMonadSet(userTextAccess, min_m, max_m) +
			" WHERE [chapter book_number = "+selBookNum+" GET chapter]"
		);

		SheafConstIterator sci = sheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject chapter = straw.const_iterator().next();
			
			int thisChapNum = chapter.getEMdFValue("chapter").getInt();
			if (thisChapNum == selChapNum)
			{
				foundChapter = true;
				SetOfMonads som = new SetOfMonads();
				chapter.getSOM(som, false);
				min_m = som.first();
				max_m = som.last();
			}				
			%>
			<option<%=
				thisChapNum == selChapNum ? " SELECTED" : ""
			%>><%=
				thisChapNum
			%><%
		}
		
		if (foundChapter)
			session.setAttribute("chapterNum", new Integer(selChapNum));
	}

%>
	</select>
  </td>
  <td>
	<select name="verse" onChange="document.forms.nav.submit()">
<%
	int selVerseNum = 1;
	
	{
		Integer sessionVerseNum = 
			(Integer)( session.getAttribute("verseNum") );
		if (sessionVerseNum != null)
			selVerseNum = sessionVerseNum.intValue();
	}
	
	try { 
		int newVerseNum = Integer.parseInt(request.getParameter("verse")); 
		selVerseNum = newVerseNum;
	} catch (Exception e) { /* ignore it and use default chapter */ }
	
	{
		boolean foundVerse = false;
	
		Sheaf sheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " +
			emdros.getMonadSet(userTextAccess, min_m, max_m) +
			" WHERE [verse "+
			"        book_number = "+selBookNum+" AND "+
			"        chapter     = "+selChapNum+
			"        GET verse, verse_label]"
		);

		SheafConstIterator sci = sheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject verse = straw.const_iterator().next();
			
			int thisVerseNum = verse.getEMdFValue("verse").getInt();
			if (thisVerseNum == selVerseNum)
			{
				foundVerse = true;
				SetOfMonads som = new SetOfMonads();
				verse.getSOM(som, false);
				min_m = som.first();
				max_m = som.last();
			}
							
			%>
			<option value=<%=
				thisVerseNum
			%><%=
				thisVerseNum == selVerseNum ? " SELECTED" : ""
			%>><%=
				verse.getEMdFValue("verse_label").getString()
			%><%
		}
		
		if (foundVerse)
			session.setAttribute("verseNum", new Integer(selVerseNum));
	}

%>
	</select>
  </td>
  <td>
	<select name="clause" onChange="document.forms.nav.submit()">
<%
	int selClauseId = 0;
	
	{
		Integer sessionClauseId = 
			(Integer)( session.getAttribute("clauseId") );
		if (sessionClauseId != null)
			selClauseId = sessionClauseId.intValue();
	}
	
	try { 
		int newClauseId = Integer.parseInt(request.getParameter("clause"));
		selClauseId = newClauseId;
	} catch (Exception e) { /* ignore it and use default chapter */ }
	
	MatchedObject verse = null;
	
	{
		boolean foundSelectedClause = false;
		int defaultClauseId = 0;
	
		Sheaf sheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " +
			emdros.getMonadSet(userTextAccess, min_m, max_m) +
			" WHERE [verse "+
			"       book_number = "+selBookNum+" AND "+
			"       chapter     = "+selChapNum+" AND "+
			"       verse       = "+selVerseNum+
			"       GET bart_gloss "+
			"       [clause "+
			"        [word GET lexeme]"+
			"       ]"+
			"      ]");
			 
		SheafConstIterator sci = sheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			verse = straw.const_iterator().next();
			
			SheafConstIterator clause_iter =
				verse.getSheaf().const_iterator();
				
			while (clause_iter.hasNext()) {
				MatchedObject clause =
					clause_iter.next().const_iterator().next();

				String lexemes = "";
				
				SheafConstIterator word_iter =
					clause.getSheaf().const_iterator();
					
				while (word_iter.hasNext()) {
					MatchedObject word =
						word_iter.next().const_iterator().next();
					lexemes += word.getEMdFValue("lexeme").getString();
					if (word_iter.hasNext()) 
						lexemes += " ";
				}
				
				int thisClauseId = clause.getID_D();
				if (thisClauseId == selClauseId)
					foundSelectedClause = true;
					
				if (defaultClauseId == 0)
					defaultClauseId = thisClauseId;
					
				%>
				<option value=<%=
					thisClauseId
				%><%=
					thisClauseId == selClauseId ? " SELECTED" : ""
				%>><%=
					lexemes.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
				%><%
			}
		}
		
		if (!foundSelectedClause)
			selClauseId = defaultClauseId;
			
		session.setAttribute("clauseId", new Integer(selClauseId));
	}

%>
	</select>
  </td>
</tr>
</table>
</form>

<%
	if (verse != null && verse.getEMdFValue("bart_gloss") != null)
	{
%>
<p>
	Gloss for this verse:
	<em><%= verse.getEMdFValue("bart_gloss").getString() %></em>
</p>
<%
	}
%>

<%

	Map phrase_functions = emdros.getEnumerationConstants
		("phrase_function_t",false);

	Map phrase_types = emdros.getEnumerationConstants
		("phrase_type_t",false);
		
	Map parts_of_speech = emdros.getEnumerationConstants
		("psp_t",false);

	Map verbal_stems = emdros.getEnumerationConstants
		("verbal_stem_t",false);

	int clauseId = selClauseId;

	if (request.getParameter("savearg") != null)
	{
		String phraseIdString = request.getParameter("phraseid");
		String newArg         = request.getParameter("newarg");
		int phraseId          = Integer.parseInt(phraseIdString);
		Change ch = emdros.createChange(EmdrosChange.UPDATE,
			"phrase", new int[]{phraseId});
		ch.setString("argument_name", newArg);
		ch.execute();
	}

	if (request.getParameter("changemr") != null &&
		request.getParameter("mr") != null) 
	{
		String phraseIdString = request.getParameter("pid");
		String newMRString    = request.getParameter("mr");
		int phraseId          = Integer.parseInt(phraseIdString);
		int newMR             = Integer.parseInt(newMRString);
		Change ch = emdros.createChange(EmdrosChange.UPDATE,
			"phrase", new int[]{phraseId});
		ch.setInt("macrorole_number", newMR);
		ch.execute();
	}

	Sheaf sheaf = emdros.getSheaf
	(
		"SELECT ALL OBJECTS IN " +
		emdros.getMonadSet(userTextAccess, min_m, max_m) +
		" WHERE [clause self = "+clauseId+
		"       GET logical_struct_id, logical_structure "+
		"        [phrase GET phrase_type, function, argument_name, "+
		"                    type_id, macrorole_number "+
		"          [word GET lexeme, pdpsp, verbal_stem, wordnet_gloss, "+
		"                    wordnet_synset]"+
		"        ]"+
		"      ]"
	);

	MatchedObject clause = null;
	{
		SheafConstIterator sci = sheaf.const_iterator();
		if (sci.hasNext()) {
			Straw straw = sci.next();
			StrawConstIterator swci = straw.const_iterator();
			if (swci.hasNext()) {
				clause = swci.next();
			}
		}
	}
		 
	if (clause == null) {
		%><p><b>No clause found!</b></p><%
	} else {	
		String predicate_text = "";

		/* Prescan to find the predicate lexeme */
		{
			SheafConstIterator phrases = clause.getSheaf().const_iterator();
	
			while (phrases.hasNext()) {
				MatchedObject phrase =
					phrases.next().const_iterator().next();
	
				String function_name = (String)( phrase_functions.get(
					phrase.getEMdFValue("function").toString())
				);
	
				if (function_name == null)
					continue;
					
				if (! function_name.equals("Pred"))
					continue;
	
				SheafConstIterator words = phrase.getSheaf().const_iterator();
				while (words.hasNext()) {
					MatchedObject word =
						words.next().const_iterator().next();
					
					String psp = (String)( parts_of_speech.get(
						word.getEMdFValue("pdpsp").toString()) 
					);
						
					if (! psp.equals("verb"))
						continue;
					
					predicate_text = word.getEMdFValue("lexeme").getString();
				}
			}
		}
		
		/*
		%>
		<p>Predicate text is: <%= 
			predicate_text 
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;")
		%></p>
		<%
		*/
		
		%>
		<p>
		<%
	
		int currentLsId = clause.getEMdFValue("logical_struct_id").getInt();
				
		String selLsIdString = request.getParameter("lsid");
		String newLsString   = request.getParameter("newls");
		String lsSaveString  = request.getParameter("lssave");
		String structure = "";
		int selLsId = currentLsId;
		int numSyntacticMacroroles = -1;
		
		if (request.getParameter("create") != null && newLsString != null) 
		{
			try 
			{
				Change ch = sql.createChange(SqlChange.INSERT,
					"lexicon_entries", null);
				ch.setString("Lexeme",    predicate_text);
				ch.setString("Structure", newLsString);
				ch.execute();
				selLsId = ((SqlChange)ch).getInsertedRowId();
				selLsIdString = "" + selLsId;
				lsSaveString = "yes";
			} 
			catch (DatabaseException ex) 
			{
				%><%= ex %><%
			} 
			finally 
			{
				sql.finish();
			}
		} 
		else 
		{		
			try { selLsId = Integer.parseInt(selLsIdString); }
			catch (Exception e) { /* do nothing, use default */ }
		}
		
		if (selLsId != currentLsId && lsSaveString != null) 
		{
			Change ch = emdros.createChange(EmdrosChange.UPDATE,
				"clause", new int[]{clauseId});
			ch.setInt("logical_struct_id", selLsId);
			ch.execute();
		}
	
		if (!emdros.canWriteTo(clause))
		{
			// just show the current value
			try 
			{
				PreparedStatement stmt = sql.prepareSelect
					("SELECT ID,Structure,Syntactic_Args " +
					 "FROM lexicon_entries WHERE ID = ?");
				stmt.setInt(1, selLsId);
				
				ResultSet rs = sql.select();
				
				if (rs.next()) 
				{
					int    thisLsId      = rs.getInt("ID");
					String thisStructure = rs.getString("Structure");
					int    thisNumSMRs   = rs.getInt("Syntactic_Args");

					if (thisStructure != null)
					{
						structure = thisStructure;
					}
					numSyntacticMacroroles = thisNumSMRs;
				}
			} 
			catch (DatabaseException ex) 
			{
				%><%= ex %><%
			} 
			finally 
			{
				sql.finish();
			}
			
			%>
			Selected lexicon entry logical structure:
			<%= structure.equals("") ? "none" : structure %>
			<%
		}
		else if (selLsIdString != null && selLsIdString.equals("add"))
		{
			// Change button clicked and "Add..." selected, 
			// show LS entry box
			%>
		<form name="changels" method="POST">
		Enter new logical structure:
		<input name="newls"/>
		<input type="submit" name="create" value="Create"/>
		<input type="submit" name="cancel" value="Cancel"/>
		</form>
			<%
		}
		else
		{
			// Show LS selector box, Change button, and maybe Edit button

			%>
		<table><tr><td>
			<form name="changels" method="POST">
			Choose logical structure:
			<select name="lsid" onChange="enableEditButton(); 
			return enableChangeButton(lssave,<%= selLsId %>,lsid)">
			<option value="0" <%= (currentLsId == 0) ? " SELECTED" : "" %>>Not specified
			<%

			try 
			{
				PreparedStatement stmt = sql.prepareSelect
					("SELECT ID,Structure,Syntactic_Args " +
					 "FROM lexicon_entries WHERE Lexeme = ?");
				stmt.setString(1, predicate_text);
				ResultSet rs = sql.select();
				while (rs.next()) {
					int    thisLsId      = rs.getInt("ID");
					String thisStructure = rs.getString("Structure");
					int    thisNumSMRs   = rs.getInt("Syntactic_Args");
					
					if (thisLsId == selLsId)
					{
						if (thisStructure != null)
						{
							structure = thisStructure;
						}
						numSyntacticMacroroles = thisNumSMRs;
					}
					
					%>
			<option value="<%=
				thisLsId
			%>"<%=
				thisLsId == selLsId ? " SELECTED" : ""
			%>><%=
				thisStructure == null
				? "(undefined structure "+thisLsId+")"
				: (thisStructure
						.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;"))
			%>
					<%
				}
			} 
			catch (DatabaseException ex) 
			{
				%><%= ex %><%
			} 
			finally 
			{
				sql.finish();
			}
		
			%>
			<option value="add" <%=
				selLsIdString != null && selLsIdString.equals("add")
				? " SELECTED" : ""
			%>>Add new...
			</select>
			<input type="submit" name="lssave" value="Change">
			</form>
			<script type="text/javascript"><!--
			enableChangeButton(document.forms.changels.lssave,<%= selLsId %>,
							   document.forms.changels.lsid);
			//--></script>
		</td>
			<%
			
			if (selLsIdString == null || ! selLsIdString.equals("0")) 
			{
				%>
		<td>
			<form name="editls" method="get" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= selLsId %>">
			<input type="submit" name="submit" value="Edit...">
			</form>
		</td>
				<%
			}

			%>
		</table>
			<%
		}
		
		%>
		</p>
		<%
		
		Vector argNames = new Vector();
		Hashtable argNamesHash = new Hashtable();

		Pattern varPat = Pattern.compile
			("(?s)(?i)<([^>]*)>");
		Matcher m = varPat.matcher(structure);
		while (m.find()) {
			String arg = m.group(1);
			if (argNamesHash.get(arg) != null)
				continue;
			argNames.addElement(m.group(1));
			argNamesHash.put(arg, Boolean.TRUE);
		}
		
		DataType [] types = DataType.getAll();
		Hashtable variables = new Hashtable();
		
		OntologyDb ontology = Lex.getOntologyDb();
		
		%>
		<p>
		<table border>
		<%
		
		String [] object_types = new String [] {
			"word",
			"phrase"
		};
		
		class Cell {
			String label, link, format, html;
			int columns;
			Vector subcells = new Vector();
		}

		// "ewg" stands for "edit word gloss"
		String ewgString = request.getParameter("ewg");
		int ewgId = -1;
		if (ewgString != null) {
			ewgId = Integer.parseInt(ewgString);
		}

		// "ewng" stands for "edit WordNet gloss"
		String ewngString = request.getParameter("ewng");
		int ewngId = -1;
		if (ewngString != null) {
			ewngId = Integer.parseInt(ewngString);
		}

		// "swns" stands for "select WordNet sense"
		String swnsString = request.getParameter("swns");
		int swnsId = -1;
		if (swnsString != null) {
			swnsId = Integer.parseInt(swnsString);
		}
		
		for (int objectNum = 0; objectNum < object_types.length; 
			objectNum++) 
		{
			String type = object_types[objectNum];
			Vector word_row = new Vector(), struct_row = new Vector();
			int column = 0;
			
			SheafConstIterator phrases = clause.getSheaf().const_iterator();
			while (phrases.hasNext()) {
				MatchedObject phrase =
					phrases.next().const_iterator().next();
				int first_col = column;
				boolean canWriteToPhrase = emdros.canWriteTo(phrase);
	
				String function_name = (String)( phrase_functions.get(
					phrase.getEMdFValue("function").toString())
				);

				String phrase_type = (String)( phrase_types.get(
					phrase.getEMdFValue("phrase_type").toString())
				);
				
				SheafConstIterator words = phrase.getSheaf().const_iterator();
				while (words.hasNext()) {
					MatchedObject word =
						words.next().const_iterator().next();
					column++;
					boolean canWriteToWord = emdros.canWriteTo(word);
					
					if (type.equals("word")) {
						String lexeme = 
							word.getEMdFValue("lexeme").getString();
						String verbal_stem = 
							word.getEMdFValue("verbal_stem").toString();
						String part_of_speech = (String)
							parts_of_speech.get(word.getEMdFValue("pdpsp")
								.toString());
							
						Cell cell = new Cell();
						cell.label = lexeme;
						cell.columns = 1;
						word_row.addElement(cell);
						
						int wid = word.getID_D();
						
						PreparedStatement stmt = sql.prepareSelect(
							"SELECT ID, Gloss FROM lexicon_entries "+
							"WHERE Lexeme = ?");
						stmt.setString(1, lexeme);
						ResultSet rs = sql.select();
						String gloss = "";
						int lexId = -1;
						if (rs.next()) 
						{
							lexId = rs.getInt(1);
							gloss = rs.getString(2);
							if (gloss == null) gloss = "";
						}
						sql.finish();
						
						// lexicon gloss
						{
							Cell glossCell = new Cell();
							cell.subcells.add(glossCell);
	
							if (ewgId == wid &&
								request.getParameter("ewgs") != null) 
							{
								gloss = request.getParameter("gloss");
								Change ch;
								if (lexId == -1) 
								{
									ch = sql.createChange(
										SqlChange.INSERT, "lexicon_entries", null);
									ch.setString("Lexeme", lexeme);
								} 
								else 
								{
									ch = sql.createChange(
										SqlChange.UPDATE, "lexicon_entries", 
										"ID = "+lexId);
								}
								ch.setString("Gloss", gloss);
								ch.execute();
								ewgId = -1;
							}
							
							if (ewgId == wid) 
							{
								glossCell.html = "<form method=\"post\">\n" +
									"<input type=\"hidden\" name=\"ewg\"" +
									" value=\"" + wid + "\">\n" +
									"<input name=\"gloss\" size=\"10\" value=\"" +
									gloss.replaceAll("<", "&lt;")
										.replaceAll(">", "&gt;") +
									"\">\n" +
									"<input type=\"submit\" name=\"ewgs\""+
									" value=\"Save\">\n" +
									"</form>";
							} 
							else 
							{
								glossCell.html = "<a href=\"clause.jsp?ewg=" + 
									wid + "\">" + 
									(gloss.equals("") ? "(gloss)" : gloss) +
									"</a>";
							}
						}

						// wordnet gloss
						{
							String wordnetGloss = word
								.getEMdFValue("wordnet_gloss")
								.getString();
							Long wordnetSynset = new Long(word
								.getEMdFValue("wordnet_synset")
								.getInt());
								
							if (wordnetGloss == null ||
								wordnetGloss.equals(""))
							{
								OntologyDb.OntologyEntry entry = 
									ontology.getWordByLexeme(lexeme);
								if (entry != null)
								{
									wordnetGloss  = entry.m_EnglishGloss;
								}
								else
								{
									wordnetGloss  = null;
								}
								
								if (entry != null && 
									wordnetSynset.longValue() == 0)
								{
									wordnetSynset = entry.m_Synset;
								}
							}

							if (ewngId == wid &&
								request.getParameter("ewngs") != null) 
							{
								wordnetGloss = request.getParameter("gloss");
								Change ch = emdros.createChange(
										EmdrosChange.UPDATE, 
										"word", new int[] {wid});
								ch.setString("wordnet_gloss", wordnetGloss);
								ch.execute();
								ewngId = -1;
							}
							
							POS wordnetPos = null;
							
							if (part_of_speech.equals("verb"))
							{
								wordnetPos = POS.VERB;
							}
							else if (part_of_speech.equals("noun"))
							{
								wordnetPos = POS.NOUN;
							}
							else if (part_of_speech.equals("adjective"))
							{
								wordnetPos = POS.ADJECTIVE;
							}
							else if (part_of_speech.equals("adverb"))
							{
								wordnetPos = POS.ADVERB;
							}
								
							Synset [] senses = null;
							if (wordnetPos != null && wordnetGloss != null)
							{
								senses = wordnet.getSenses(wordnetPos, 
									wordnetGloss);
							}
							
							Cell wordnetCell = new Cell();
							cell.subcells.add(wordnetCell);
							
							if (wordnetPos == null)
							{
								wordnetCell.html = "";
							}
							else if (ewngId == wid && canWriteToWord) 
							{
								wordnetCell.html = "<form method=\"post\">\n" +
									"<input type=\"hidden\" name=\"ewng\"" +
									" value=\"" + wid + "\">\n" +
									"<input name=\"gloss\" size=\"10\" value=\"" +
									wordnetGloss.replaceAll("<", "&lt;")
										.replaceAll(">", "&gt;") +
									"\">\n" +
									"<input type=\"submit\" name=\"ewngs\""+
									" value=\"Save\">\n" +
									"</form>";
							} 
							else if (wordnetGloss == null)
							{
								wordnetCell.html = "[<a href=\"clause.jsp?" +
									"ewng=" + wid + "\">Add</a>]";
							}
							else
							{
								wordnetCell.html = wordnetGloss + " [";
								
								if (canWriteToWord)
								{
									wordnetCell.html += 
										"<a href=\"clause.jsp?" +
										"ewng=" + wid + "\">edit</a>|";
								}
								
								wordnetCell.html += "<a href=\"" +
									"http://www.wordreference.com/definition/" +
									URLEncoder.encode(wordnetGloss) +
									"\">lookup</a>]";
							}

							if (swnsId == wid &&
								request.getParameter("swnss") != null) 
							{
								wordnetSynset = new Long(
									request.getParameter("wns"));
								Change ch = emdros.createChange(
										EmdrosChange.UPDATE, 
										"word", new int[] {wid});
								ch.setInt("wordnet_synset", 
									wordnetSynset.longValue());
								ch.execute();
								swnsId = -1;
							}

							Cell glossCell = new Cell();
							cell.subcells.add(glossCell);
							
							if (wordnetPos == null)
							{
								glossCell.html = "";
							}
							else if (wordnetGloss == null)
							{	
								glossCell.html = "(no word to look up in Wordnet)";
							}
							else if (senses == null)
							{
								glossCell.html = "(no match in Wordnet)";
							}
							else if (swnsId == wid && canWriteToWord)
							{
								glossCell.html = "<form method=\"post\">\n" +
									"<input type=\"hidden\" name=\"swns\"" +
									" value=\"" + wid + "\">\n";
								
								for (int i = 0; i < senses.length; i++)
								{
									Long key = (Long)(senses[i].getKey());
									glossCell.html += 
										"<input type=\"radio\" name=\"wns\"" +
										" value=\"" + key.longValue() + "\"" +
										(key.longValue() ==
										wordnetSynset.longValue() ? " checked" : "") +
										" />" +
										senses[i].getGloss()
											.replaceAll("<", "&lt;")
											.replaceAll(">", "&gt;") +
										"<br>\n";
								}
								
								glossCell.html += 
									"<input type=\"submit\" name=\"swnss\""+
									" value=\"Save\">\n" +
									"</form>";
							}
							else if (wordnetSynset == null)
							{
								glossCell.html = "(no Wordnet sense selected)";
							}
							else
							{
								Synset sense = null;
								
								for (int i = 0; i < senses.length; i++)
								{
									Long key = (Long)(senses[i].getKey());
									if (key.longValue() ==
										wordnetSynset.longValue())
									{
										sense = senses[i];
										break;
									}
								}
								
								if (sense == null)
								{
									glossCell.html = 
										"(invalid Wordnet sense selected)";
								}
								else
								{
									glossCell.html = sense.getGloss();
								}
							}
							
							if (wordnetPos != null && swnsId != wid && 
								canWriteToWord)
							{
								glossCell.html += 
										" [<a href=\"clause.jsp?swns=" + wid +
										"\">change</a>]";
							}
						}
						
						{
							Cell stemCell = new Cell();
							cell.subcells.add(stemCell);
							
							stemCell.html = 
								(String)( verbal_stems.get(verbal_stem) );
							if (stemCell.html.equals("NA"))
								stemCell.html = "";
						}
					}
				}
				
				if (type.equals("phrase")) 
				{
					Cell pCell    = new Cell();
					pCell.label   = phrase.getEMdFValue("function").toString();
					pCell.columns = column - first_col;
					struct_row.addElement(pCell);

					if (function_name != null)
						pCell.label = phrase_type + " (" + function_name + ")";
					
					if (phrase_type == null)
						continue;
						
					Cell mrCell = new Cell();
					pCell.subcells.add(mrCell);
					mrCell.html = "";

					if (phrase_type.equals("VP"))
					{
						String html = "Macroroles: ";
						
						switch (numSyntacticMacroroles)
						{
							case -1: html += "MR? (unknown)"; break;
							case 0:  html += "MR0"; break;
							case 1:  html += "MR1"; break;
							case 2:  html += "MR2"; break;
							case 3:  html += "MR3"; break;
							default: html += "MR! (invalid)"; break;
						}
						
						mrCell.html = html;
					}
					else if (phrase_type.equals("NP") ||
							 phrase_type.equals("IrPronNP") ||
							 phrase_type.equals("PersPronNP") ||
							 phrase_type.equals("DemPronNP") ||
							 phrase_type.equals("PropNP") ||
							 phrase_type.equals("PP"))
					{
						int oldMR = phrase.getEMdFValue("macrorole_number")
							.getInt();
						String formName = "mr_" + phrase.getID_D();
							
						StringBuffer html = new StringBuffer();
						html.append("<form name=\"" + formName + "\" " +
							"method=\"POST\">\n");
						html.append("<input type=\"hidden\" name=\"pid\" " +
							"value=\"" + phrase.getID_D() + "\">\n");
						html.append("<input type=\"hidden\" name=\"prev\" " +
							"value=\"" + oldMR + "\">\n");
						html.append("<select name=\"mr\" " +
							"onChange=\"return enableChangeButton(" +
							"changemr, "+oldMR+", mr)\">\n");
						html.append("<option "+((oldMR==-1)?"SELECTED":"")+
							" value=\"-1\">Unknown\n");
						html.append("<option "+((oldMR==0) ?"SELECTED":"")+
							" value=\"0\">None\n");
						html.append("<option "+((oldMR==1) ?"SELECTED":"")+
							" value=\"1\">1 (Actor)\n");
						html.append("<option "+((oldMR==2) ?"SELECTED":"")+
							" value=\"2\">2 (Undergoer)\n");
						html.append("</select>\n");
						
						if (canWriteToPhrase)
						{
							html.append("<input type=\"submit\" "+
								"name=\"changemr\" value=\"Change\">\n");
						}
						
						html.append("</form>\n");

						html.append("<script type=\"text/javascript\"><!--\n");
						html.append("\tenableChangeButton(" +
							"document.forms."+formName+".changemr, "+
							oldMR+", "+
							"document.forms."+formName+".mr)\n");
						html.append("//--></script>\n");

						mrCell.html = html.toString();
					}
						
					if (! phrase_type.equals("NP") &&
						! phrase_type.equals("IrPronNP") &&
						! phrase_type.equals("PersPronNP") &&
						! phrase_type.equals("DemPronNP") &&
						! phrase_type.equals("PropNP") &&
						! phrase_type.equals("PP"))
						continue;

					Cell varCell = new Cell();
					pCell.subcells.add(varCell);

					StringBuffer editHtml = new StringBuffer();
					  
					String oldArg = phrase.getEMdFValue("argument_name")
						.toString();
					String newArg = "";

					if (oldArg.equals("")) 
					{
						// Argument not decided yet. Maybe we can guess
						// based on the "function" of the clause?
						
						if (
							function_name.equals("Subj") ||
							function_name.equals("PreS") ||
							function_name.equals("IrpS") ||
							function_name.equals("ModS"))
						{
							newArg = "x";
						}
						else if (
							function_name.equals("Objc") ||
							function_name.equals("PreC") ||
							function_name.equals("PreO") ||
							function_name.equals("PtcO") ||
							function_name.equals("IrpO"))
						{
							newArg = "y";
						}
						
						if (newArg != oldArg && 
							argNamesHash.get(newArg) != null) 
						{
							if (canWriteToPhrase)
							{
								Change ch = emdros.createChange(
									EmdrosChange.UPDATE,
									"phrase", new int[]{phrase.getID_D()});
								ch.setString("argument_name", newArg);
								ch.execute();
							}
							oldArg = newArg;
						}
					}
					else if (argNamesHash.get(oldArg) == null)
					{
						argNames.add(oldArg);
						argNamesHash.put(oldArg, Boolean.TRUE);
					}
						
					if (oldArg.equals(""))
					{
						if (! newArg.equals(""))
						{
							editHtml.append("<font color=\"red\">" +
								"Variable not set</font> " +
								"(defaults to <em>"+newArg+"</em>)");
						}
						else
						{
							editHtml.append("<font color=\"red\">" +
								"Variable not set</font>");
						}
					}
					else if (variables.get(oldArg) != null) 
					{
						editHtml.append("<font color=\"red\">" +
							"Duplicate variable!</font>");
					} 
					else 
					{
						variables.put(oldArg, phrase);
					}

					String formName = "sv_" + phrase.getID_D();

					editHtml.append(
						"<form method=\"post\" name=\""+formName+"\">\n" +
						"<input type=\"hidden\" name=\"phraseid\"" +
						" value=\"" + phrase.getID_D() + "\">\n" +
						"<select name=\"newarg\" onChange=\"return " +
						"enableChangeButton(savearg,'"+oldArg+"',newarg)" +
						"\">\n" +
						"<option value=\"\" " +
						(oldArg.equals("") ? " SELECTED" : "") +
						">Auto\n" +
						"<option value=\" \" " +
						(oldArg.equals(" ") ? " SELECTED" : "") +
						">None\n");

					for (int j = 0; j < argNames.size(); j++) {
						String arg = (String)( argNames.elementAt(j) );
						editHtml.append("<option value=\""+arg+
							"\""+(oldArg.equals(arg)?" SELECTED":"")+
							">"+arg+"\n");
					}

					editHtml.append("</select>\n");
					
					if (canWriteToPhrase)
					{
						editHtml.append
						(
							"<input type=\"submit\" name=\"savearg\" "+
							"value=\"Change\">\n"
						);
					}
					
					editHtml.append("</form>\n");

					editHtml.append("<script type=\"text/javascript\"><!--\n" +
						"enableChangeButton(" +
						"document.forms."+formName+".savearg,\""+oldArg+"\"," +
						"document.forms."+formName+".newarg)\n" +
						"//--></script>\n");
												
					varCell.html = editHtml.toString();

					/*
					Cell typeCell = new Cell();
					pCell.subcells.add(typeCell);
					editHtml = new StringBuffer();

					int oldType = phrase.getEMdFValue("type_id")
						.getInt();
						
					editHtml.append("<form method=\"post\">\n" +
						"<input type=\"hidden\" name=\"phraseid\"" +
						" value=\"" + phrase.getID_D() + "\">\n" +
						"<select name=\"newtype\">\n" +
						"<option value=\"\" " +
						(oldType == 0 ? " SELECTED" : "") +
						">None\n");

					for (int j = 0; j < types.length; j++) {
						DataType t = types[j];
						editHtml.append("<option value=\""+t.id+
							"\""+(oldType == t.id ? " SELECTED" : "")+
							">");
						while (t.depth-- > 0) {
							editHtml.append("&nbsp;");
						}
						editHtml.append(t.name+"\n");
					}

					editHtml.append("</select>\n" +
						"<input type=\"submit\" name=\"savetype\" "+
						"value=\"Save\">\n"+
						"</form>\n");
					
					typeCell.html = editHtml.toString();
					*/
				}
			}

			
			Vector bigRows = new Vector();
			if (word_row.size() > 0)
				bigRows.add(word_row);
			if (struct_row.size() > 0)
				bigRows.add(struct_row);

			Cell filler = new Cell();
			filler.label = "";

			for (int nBigRow = 0; nBigRow < bigRows.size(); nBigRow++) {
				Vector bigRow = (Vector)( bigRows.elementAt(nBigRow) );
				int numColumns = bigRow.size();
				int littleRowsThisBigRow = 0;
				
				for (Enumeration e = bigRow.elements(); 
					e.hasMoreElements(); ) 
				{
					Cell c = (Cell)(e.nextElement());
					int reqRows = c.subcells.size();
					if (reqRows > littleRowsThisBigRow) 
						littleRowsThisBigRow = reqRows;
				}
				
				// don't forget to count the top cell as well!
				littleRowsThisBigRow++;
				
				for (int r = 0; r < littleRowsThisBigRow; r++) {
					%><!-- <%= nBigRow %>/<%= r %> -->
					<tr>
					<%
					
					if (r == 0) {
						%>
						<th rowspan="<%= littleRowsThisBigRow %>">
							<%= type %>
						</th>
						<%
					}

					for (int c = 0; c < numColumns; c++) {
						Cell topCell  = (Cell)( bigRow.elementAt(c) );
						Cell thisCell = topCell;
						if (r > 0) {
							if (topCell.subcells != null && 
								r <= topCell.subcells.size()) 
							{
								thisCell = (Cell)( 
									topCell.subcells.elementAt(r - 1) );
							} else {
								thisCell = filler;
							}
						}

						%>
						<td colspan="<%= topCell.columns %>"><%

						if (thisCell.html != null) {
							%><%= thisCell.html %><%
						} else {
							String html = thisCell.label
								.replaceAll("<", "&lt;")
								.replaceAll(">", "&gt;");
							
							if (thisCell.link != null) {
								%><a href="<%= thisCell.link %>"><%
							}

							%><%= html %><%

							if (thisCell.link != null) {
								%></a>><%
							}
						}
						
						%>
						</td><%
					}
	
					%>
					</tr>
					<%
				}
			}
		}
	
		%>
		</table>
		</p>
		<%
		
		%>
		<p>Linked logical structure:
		<%

		for (Enumeration e = variables.keys(); e.hasMoreElements(); ) {
			String variable = (String)( e.nextElement() );
			MatchedObject value = (MatchedObject)
				( variables.get(variable) );
			String value_text = "";
			SheafConstIterator sci = value.getSheaf().const_iterator();
			
			while (sci.hasNext()) {
				MatchedObject word = sci.next().const_iterator().next();
				value_text += word.getEMdFValue("lexeme").getString();
				if (sci.hasNext())
					value_text += " ";
			}
			
			structure = structure.replaceAll
				("<" + variable + ">", value_text);
		}
		
		%>
		<%= structure.replaceAll("<","&lt;").replaceAll(">","&gt;") %></p>
		<%	

		String currentStruct = clause.getEMdFValue("logical_structure")
			.getString();
		if (! currentStruct.equals(structure) ) {
			Change ch = emdros.createChange(EmdrosChange.UPDATE,
				"clause", new int[]{clauseId});
			ch.setString("logical_structure", structure);
			ch.execute();
		}
	}

	TreeNode clauseNode  = new TreeNode("Clause");
	TreeNode coreNode    = null;
	TreeNode nucleusNode = null;
	TreeNode leftPeripheryNode  = null;
	TreeNode rightPeripheryNode = null;

	SheafConstIterator phrases = null;
	
	if (clause != null)
	{
		phrases = clause.getSheaf().const_iterator();
	}
	
	while (phrases != null && phrases.hasNext()) 
	{
		MatchedObject phrase =
			phrases.next().const_iterator().next();

		String function_name = (String)( phrase_functions.get(
			phrase.getEMdFValue("function").toString())
		);

		String phrase_type = (String)( phrase_types.get(
			phrase.getEMdFValue("phrase_type").toString())
		);
		
		TreeNode phraseNode = new TreeNode(phrase_type);
		TreeNode phraseFunc = phraseNode.createChild(function_name);
		
		SheafConstIterator words = phrase.getSheaf().const_iterator();
		while (words.hasNext()) 
		{
			MatchedObject word =
				words.next().const_iterator().next();
			
			String lexeme = 
				word.getEMdFValue("lexeme").getString()
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;");
			
			TreeNode wordNode = phraseFunc.createChild(lexeme);
		}
	
		if (phrase_type.equals("VP") && nucleusNode == null)
		{
			if (coreNode == null)
			{
				coreNode = clauseNode.createChild("Core");
			}
			
			nucleusNode = coreNode.createChild("Nucleus");
			nucleusNode.add(phraseNode);
		}
		else if (phrase_type.equals("NP") ||
				 phrase_type.equals("IrPronNP") ||
				 phrase_type.equals("PersPronNP") ||
				 phrase_type.equals("DemPronNP") ||
				 phrase_type.equals("PropNP") ||
				 (phrase_type.equals("PP") && ! function_name.equals("Time")))
		{
			if (coreNode == null)
			{
				coreNode = clauseNode.createChild("Core");
			}
			
			TreeNode argument = coreNode.createChild("Argument");
			argument.add(phraseNode);
		}
		else
		{
			TreeNode periphery;
			
			if (coreNode == null)
			{
				if (leftPeripheryNode == null)
				{
					leftPeripheryNode = clauseNode.createChild("Periphery");
				}
				periphery = leftPeripheryNode;
			}
			else
			{
				if (rightPeripheryNode == null)
				{
					rightPeripheryNode = clauseNode.createChild("Periphery");
				}
				periphery = rightPeripheryNode;
			}
			
			TreeNode other = periphery.createChild("");
			other.add(phraseNode);
		}
	}
	
	class BorderTableRenderer extends TableRenderer
	{
	    public String getTable(String contents)
	    {
	        return "<table class=\"tree\" border>" + contents + "</table>";
	    }
	}
	
%>

<%= clauseNode.toHtml(new BorderTableRenderer()) %>

<h2>Notes</h2>

<%
	if (request.getParameter("nc") != null &&
		request.getParameter("nt") != null)
	{
		String newNoteText = request.getParameter("nt");
		
		EmdrosChange ch = (EmdrosChange)(
			emdros.createChange(EmdrosChange.CREATE,
				"note", null));
		ch.setString("text", newNoteText);
		ch.setMonadsFromObjects(new int[]{selClauseId});
		ch.execute();
	}

	if (request.getParameter("nu") != null &&
		request.getParameter("ni") != null &&
		request.getParameter("nt") != null)
	{
		String updateNoteIdString = request.getParameter("ni");
		int updateNoteId = Integer.parseInt(updateNoteIdString);
		String newNoteText = request.getParameter("nt");
		
		EmdrosChange ch = (EmdrosChange)(
			emdros.createChange(EmdrosChange.UPDATE,
				"note", new int [] {updateNoteId}));
		ch.setString("text", newNoteText);
		ch.execute();
	}

	if (request.getParameter("nd") != null &&
		request.getParameter("ni") != null)
	{
		String deleteNoteIdString = request.getParameter("ni");
		int deleteNoteId = Integer.parseInt(deleteNoteIdString);
		
		EmdrosChange ch = (EmdrosChange)(
			emdros.createChange(EmdrosChange.DELETE,
				"note", new int[]{deleteNoteId}));
		ch.execute();
	}
%>

<table border>
<%
	{
		boolean foundNotes = false;

		String editNoteIdString = request.getParameter("ni");
		int editNoteId = -1;
		if (editNoteIdString != null)
		{
			editNoteId = Integer.parseInt(editNoteIdString);
		}
	
		Sheaf clauseSheaf = emdros.getSheaf
			("SELECT ALL OBJECTS IN { "+min_m+" - "+max_m+"} "+
			 "WHERE [clause self = "+clauseId+
			 " [note GET text]"+
			 "]");

		SheafConstIterator sci = clauseSheaf.const_iterator();
		while (sci.hasNext()) 
		{
			Straw straw = sci.next();
			clause = straw.const_iterator().next();
			
			Sheaf noteSheaf = clause.getSheaf();
			SheafConstIterator nsci = noteSheaf.const_iterator();
			while (nsci.hasNext()) 
			{
				Straw ns = nsci.next();
				MatchedObject note = ns.const_iterator().next();
			
				String noteText = note.getEMdFValue("text").getString();
				if (!foundNotes)
				{
					foundNotes = true;
				}
				
				%>
<tr>
				<%

				if (request.getParameter("ne") != null &&
					editNoteId == note.getID_D())
				{
					%>
	<form method="POST" action="clause.jsp">
	<input type="hidden" name="ni" value="<%= note.getID_D() %>" />
	<td>
		<input name="nt" size="80" value="<%= 
		noteText.replaceAll("<", "&lt;").replaceAll(">", "&gt;") 
	%>" />
	</td>
	<td>
		<input type="submit" name="nu" value="Update" />
		<input type="submit" name="nd" value="Delete" />
	</td>
	</form>
					<%
				}
				else
				{
					%>					
	<td><%= noteText.replaceAll("<", "&lt;").replaceAll(">", "&gt;") %></td>
	<td>
		<a href="clause.jsp?ni=<%= note.getID_D() %>&ne=1">Edit</a>
	</td>
					<%
				}

				%>
</tr>
				<%
			}
		}
		
		if (!foundNotes)
		{
			%>
<tr><td>No notes for this clause</td></tr>
			<%
		}
	}
%>
</table>

<p><form method="POST">
<table>
<tr>
<td>Add note:</td>
<td><input name="nt" size="80"></td>
<td><input type="submit" name="nc" value="Create"></td>
</tr>
</table>
</form></p>

<hr>
Book    <%= session.getAttribute("bookNum") %>
Chapter <%= session.getAttribute("chapterNum") %>
Verse   <%= session.getAttribute("verseNum") %>
Clause  <%= session.getAttribute("clauseId") %>
</form>
</body></html>	

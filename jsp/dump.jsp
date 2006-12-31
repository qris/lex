<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Vector" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="java.util.SortedSet" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.lex.sql.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<html>
<head>
	<title>Lex: Emdros Data Dump</title>
	<link rel="stylesheet" href="style.css" />
	<style>
		div.topmenu a.dump_jsp <%@ include file="hilite.inc" %>
	</style>
</head>
<body>
	
<%@ include file="header.jsp" %>
<h2>Emdros Data Dump</h2>

<%@ include file="auth.jsp" %>

<form name="nav" method="get">
<table>
<tr bgcolor="#FFCCCC"><th colspan=4>Navigator</th></tr>
<tr>
<td>
 	Book
	<select name="book" onChange="document.forms.nav.submit()">
<%

	int selBookNum = 1;
	
	{
		Integer sessionBookNum = (Integer)( session.getAttribute("bookNum") );
		if (sessionBookNum != null) {
			selBookNum = sessionBookNum.intValue();
		}
	}
	
	try { selBookNum = Integer.parseInt(request.getParameter("book")); }
	catch (Exception e) { /* ignore it and use default book */ }
	
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
				foundBook = true;
				
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
 	Chapter
	<select name="chapter" onChange="document.forms.nav.submit()">
<%
	int currChapNum = 1;

	{
		Integer sessionChapterNum = 
			(Integer)( session.getAttribute("chapterNum") );
		if (sessionChapterNum != null)
			currChapNum = sessionChapterNum.intValue();
	}

	try { 
		int selChapNum = Integer.parseInt(request.getParameter("chapter"));
		currChapNum = selChapNum;
	} catch (Exception e) {
		/* ignore it and use default chapter */
	}
	
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
			if (thisChapNum == currChapNum)
				foundChapter = true;
				
			%>
			<option<%=
				thisChapNum == currChapNum ? " SELECTED" : ""
			%>><%=
				thisChapNum
			%><%
		}
		
		if (foundChapter)
			session.setAttribute("chapterNum", new Integer(currChapNum));
	}

%>
	</select>
</td>
<td>
 	Verse
	<select name="verse" onChange="document.forms.nav.submit()">
<%
	int selVerseNum = 1;
	
	{
		Integer sessionVerseNum = 
			(Integer)( session.getAttribute("verseNum") );
		if (sessionVerseNum != null)
			selVerseNum = sessionVerseNum.intValue();
	}
	
	try { selVerseNum = Integer.parseInt(request.getParameter("verse")); }
	catch (Exception e) { /* ignore it and use default chapter */ }
	
	{
		boolean foundVerse = false;
	
		Sheaf sheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " +
			emdros.getMonadSet(userTextAccess, min_m, max_m) +
			" WHERE [verse "+
			"       book_number = "+selBookNum+" AND "+
			"       chapter     = "+currChapNum+
			"       GET verse, verse_label]"
		);

		SheafConstIterator sci = sheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject verse = straw.const_iterator().next();
			
			int thisVerseNum = verse.getEMdFValue("verse").getInt();
			if (thisVerseNum == selVerseNum)
				foundVerse = true;
				
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
 	Clause
	<select name="clause" onChange="document.forms.nav.submit()">
<%
	int selClauseId = 0;
	
	{
		Integer sessionClauseId = 
			(Integer)( session.getAttribute("clauseId") );
		if (sessionClauseId != null)
			selClauseId = sessionClauseId.intValue();
	}
	
	try { selClauseId = Integer.parseInt(request.getParameter("clause")); }
	catch (Exception e) { /* ignore it and use default chapter */ }

	{
		boolean foundSelectedClause = false;
		int defaultClauseId = 0;
	
		Sheaf sheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " +
			emdros.getMonadSet(userTextAccess, min_m, max_m) +
			" WHERE [verse "+
			"       book_number = "+selBookNum+" AND "+
			"       chapter     = "+currChapNum+" AND "+
			"       verse       = "+selVerseNum+
			"       [clause "+
			"        [word GET lexeme]"+
			"       ]"+
			"      ]"
		);
			 
		SheafConstIterator sci = sheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject verse = straw.const_iterator().next();
			
			SheafConstIterator clause_iter =
				verse.getSheaf().const_iterator();
				
			while (clause_iter.hasNext()) {
				MatchedObject this_clause =
					clause_iter.next().const_iterator().next();

				String lexemes = "";
				
				SheafConstIterator word_iter =
					this_clause.getSheaf().const_iterator();
					
				while (word_iter.hasNext()) {
					MatchedObject word =
						word_iter.next().const_iterator().next();
					lexemes += word.getEMdFValue("lexeme").getString();
					if (word_iter.hasNext()) 
						lexemes += " ";
				}
				
				int thisClauseId = this_clause.getID_D();
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
					lexemes
						.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;")
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

<%

	class MonadRange {
		int first, last;
	}
	
	class DbObject {
		String type;
		int id;
		int firstMonad, lastMonad;
		MonadRange [] monadRanges;
		Hashtable features;
	}

	int left = 728, right = 734;

	int clauseId = selClauseId;

	Sheaf sheaf = emdros.getSheaf
	(
		"SELECT ALL OBJECTS IN " +
		emdros.getMonadSet(userTextAccess, min_m, max_m) +
		" WHERE [clause self = "+clauseId+"]"
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

	if (clause != null) {
		Table monads = emdros.getTable
			("GET MONADS FROM OBJECT WITH ID_D = "+clause.getID_D()+" "+
			 "[clause]");
		TableRow tr = monads.iterator().next();
		left  = Integer.parseInt(tr.getColumn(2));
		right = Integer.parseInt(tr.getColumn(3));
	} else {
		%>Null clause!<%
	}
	
	Table objects = emdros.getTable("SELECT OBJECTS HAVING MONADS IN " +
		"{" + left + "-" + right + "} [ALL] GO");

	int numObjects = objects.size();
	DbObject [] dbObjects = new DbObject [numObjects];
	TableIterator ti = objects.iterator();

	// collect information about each object, specifically the set of monads
	// which fall within our range (between left and right borders)

	for (int objectNum = 0; objectNum < numObjects; objectNum++) {
		TableRowIterator tri = ti.next().iterator();

		DbObject object = new DbObject();
		object.type = tri.next();
		object.firstMonad = Integer.parseInt(tri.next());

		String objectIdString = tri.next();
		object.id = Integer.parseInt(objectIdString);
		
		Table monad_ranges = emdros.getTable("GET MONADS FROM OBJECTS " +
			"WITH ID_Ds = "+objectIdString+" ["+object.type+"] GO");
		
		int numRanges = monad_ranges.size();
		Vector monadRangeVector = new Vector();
		
		TableIterator mi = monad_ranges.iterator();
		for (int rangeNum = 0; rangeNum < numRanges; rangeNum++) {
			TableRowIterator mri = mi.next().iterator();
			mri.next(); // skip the object id_d, we know what it is
			MonadRange range = new MonadRange();
			range.first = Integer.parseInt(mri.next());
			range.last = Integer.parseInt(mri.next());
			if (range.last >= left && range.first <= right) {
				if (range.first < left)
					range.first = left;
				if (range.last > right)
					range.last = right;
				if (range.last - range.first >= 0)
					monadRangeVector.add(range);
			}
		}

		if (monadRangeVector.size() == 0) {
			throw new RuntimeException("no valid ranges in object ["+
				object.type+" "+object.id+"]");
		}
		
		object.monadRanges = new MonadRange[monadRangeVector.size()];
		monadRangeVector.copyInto(object.monadRanges);

		String query = "SELECT FEATURES FROM [" + 
			object.type + "]";

		%>
		<!-- <p><%= object.type %> '<%= query %>'</p> -->
		<%
		
		Table features = emdros.getTable(query);
		int numFeatures = features.size();
		String [] featureNames = new String [numFeatures];
		StringBuffer featureValueQuery = new StringBuffer("GET FEATURES ");

		{
			TableIterator ti2 = features.iterator();
			while (ti2.hasNext()) {
				TableRow tr = ti2.next();
				%>
				<!--<p><%= object.type %> <%= tr.getColumn(1) %></p>-->
				<%
			}
		}
		
		{
			TableIterator fni = features.iterator();
			int index = 0;
			for (int i = 0; i < numFeatures; i++) 
			{
				TableRowIterator fnri = fni.next().iterator();
				String name = fnri.next();
				if (i < numFeatures - 1)
				{
					featureValueQuery.append(name+",");
				}
				else
				{
					featureValueQuery.append(name);
				}
			}
		}
		
		featureValueQuery.append(" FROM OBJECTS WITH ID_DS = "+object.id+
			" ["+object.type+"] GO");
		object.features = new Hashtable();
		Table featureValues = emdros.getTable(featureValueQuery.toString());

		TableIterator fvi;
		TableIterator fni = features.iterator();
		fvi = featureValues.iterator();
		TableRowIterator fvri = fvi.next().iterator();
		fvri.next(); // skip the id_d column of GET FEATURES result
		
		while (fni.hasNext()) {
			TableRowIterator fnri = fni.next().iterator();
			String name = fnri.next();
			String value = fvri.next();
			object.features.put(name, value);
		}
		
		dbObjects[objectNum] = object;
	}
	
	if (dbObjects.length > 0) {
		%>
		<table border>
		<tr>
		<th>type</th>
		<%
		
		for (int i = left; i <= right; i++) {
			%>
			<th><%= i %></th>
			<%
		}
		
		%>
		</tr>
		<%
		
		for (int objectNum = 0; objectNum < dbObjects.length; objectNum++) {
			DbObject object = dbObjects[objectNum];
			Object [] featureNames = new TreeSet(object.features.keySet())
				.toArray();
			MonadRange [] ranges = object.monadRanges;
			
			for (int featureNum = -1; featureNum < featureNames.length; featureNum++) {
				String featureName;

				if (featureNum == -1) {
					featureName = null;
				} else {
					featureName = (String)( featureNames[featureNum] );
				}
				
				/*
				if (featureName != null &&
					! featureName.equals("lexeme")) {
					continue;
				}
				*/
				
				if (featureName == null) {
					%>
					<tr>
					<td><b><%= object.type %></b></td>
					<%
				} else {
					%>
					<tr>
					<td>&nbsp;&nbsp;<i><%= featureName %></i></td>
					<%
				}
				
				String featureValue = null;
				if (featureNum >= 0)
					featureValue = 
						((String)( object.features.get(featureName) ))
						.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;");
		
				/* pad up to the first monad */		
				MonadRange firstRange = ranges[0];
				if (firstRange.first > left) {
					%>
					<td colspan="<%= firstRange.first - left %>"/>
					<%
				}
				
				for (int rangeNum = 0; rangeNum < ranges.length; rangeNum++) {
					MonadRange range = ranges[rangeNum];
					
					%>
					<td bgcolor="#E0E0E0" colspan="<%= range.last - range.first + 1 %>"><%
					if (featureNum >= 0) {
						%><%= featureValue %><%
					} else {
						%><%= object.id %><%
					}
					%></td>
					<%
					
					if (rangeNum < ranges.length - 1) {
						%>
						<td colspan="<%= ranges[rangeNum+1].first - range.last - 1 %>"/>
						<%
					}
				}
				
				MonadRange lastRange = ranges[ranges.length-1];
				if (lastRange.last < right) {
					%>
					<td colspan="<%= right - lastRange.last %>"/>
					<%
				}
			}
			
			%>
			</tr>
			<%
		}
		
		%>
		</table>
		<%
	} else {
		%>No objects found<%
	}
%>
</body></html>

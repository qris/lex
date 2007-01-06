<%@ page import="jemdros.*" %>
<form name="nav" method="get">
<table>
<tr bgcolor="#ffcccc"><th colspan=4>Navigator</th></tr>
<tr bgcolor="#ffeeee">
  <th>Book</th>
  <th>Chapter</th>
  <th>Verse</th>
  <th>Clause</th>
</tr>
<tr bgcolor="#ffcccc">
  <td>
	<select name="book" onChange="document.forms.nav.submit()">
<%

	int selBookNum = 1;
	
	{
		Integer sessionBookNum = (Integer)( session.getAttribute("bookNum") );
		if (sessionBookNum != null) 
		{
			selBookNum = sessionBookNum.intValue();
		}
	}
	
	try 
	{ 
		int newBookNum = Integer.parseInt(request.getParameter("book"));
		selBookNum = newBookNum;
	} 
	catch (Exception e) { /* ignore it and use default book */ }
	
	{
		boolean foundBook = false;
	
		Table bookTable = emdros.getTable
		(
			"SELECT OBJECTS HAVING MONADS IN " + 
			emdros.getMonadSet(userTextAccess, min_m, max_m) + " [book]"
		);
		
		StringBuffer id_dList = new StringBuffer();
		
		TableIterator rows = bookTable.iterator();
        while (rows.hasNext()) 
        {
            TableRow row = rows.next();
            id_dList.append(row.getColumn(3));
            if (rows.hasNext())
            {
            	id_dList.append(",");
            }
        }
        
		Table featureTable = emdros.getTable
		(
			"GET FEATURES book, book_number FROM OBJECTS WITH ID_DS = " + 
			id_dList.toString() + " [book]"
		);

		rows = featureTable.iterator();
        while (rows.hasNext()) 
        {
        	TableRow row = rows.next();
			int thisBookNum = Integer.parseInt(row.getColumn(3)); 
			
			if (thisBookNum == selBookNum)
			{
				foundBook = true;
				Table monadTable = emdros.getTable
				(
					"GET MONADS FROM OBJECT WITH ID_D = " + row.getColumn(1) +
					" [book]"
				);
				TableRow monad_row = monadTable.iterator().next();
				int new_min_m = Integer.parseInt(monad_row.getColumn(2)); 	
				int new_max_m = Integer.parseInt(monad_row.getColumn(3)); 	
				if (min_m < new_min_m) min_m = new_min_m;
				if (max_m > new_max_m) max_m = new_max_m;
			}
				
			%>
			<option <%=
				thisBookNum == selBookNum ? "SELECTED" : ""
			%> value="<%= 
				row.getColumn(3) 
			%>"><%= 
				row.getColumn(2)
			%><%
        }

		if (foundBook)
		{
			session.setAttribute("bookNum", new Integer(selBookNum));
		}
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
		{
			selChapNum = sessionChapterNum.intValue();
		}
	}

	try 
	{
		int newChapNum = Integer.parseInt(request.getParameter("chapter"));
		selChapNum = newChapNum;
	} 
	catch (Exception e) { /* ignore it and use default chapter */ }
	
	{
		boolean foundChapter = false;

		Table chapterTable = emdros.getTable
		(
			"SELECT OBJECTS HAVING MONADS IN " + 
			emdros.getMonadSet(userTextAccess, min_m, max_m) + " [chapter]"
		);

		StringBuffer id_dList = new StringBuffer();
		
		TableIterator rows = chapterTable.iterator();
        while (rows.hasNext()) 
        {
            TableRow row = rows.next();
            id_dList.append(row.getColumn(3));
            if (rows.hasNext())
            {
            	id_dList.append(",");
            }
        }
        
		Table featureTable = emdros.getTable
		(
			"GET FEATURES chapter FROM OBJECTS WITH ID_DS = " + 
			id_dList.toString() + " [chapter]"
		);

		rows = featureTable.iterator();
        while (rows.hasNext()) 
        {
        	TableRow row = rows.next();
			int thisChapNum = Integer.parseInt(row.getColumn(2)); 
			
			if (thisChapNum == selChapNum)
			{
				foundChapter = true;
				Table monadTable = emdros.getTable
				(
					"GET MONADS FROM OBJECT WITH ID_D = " + row.getColumn(1) +
					" [chapter]"
				);
				TableRow monad_row = monadTable.iterator().next();
				int new_min_m = Integer.parseInt(monad_row.getColumn(2)); 	
				int new_max_m = Integer.parseInt(monad_row.getColumn(3)); 	
				if (min_m < new_min_m) min_m = new_min_m;
				if (max_m > new_max_m) max_m = new_max_m;
			}
			
			%>	
			<option<%=
				thisChapNum == selChapNum ? " SELECTED" : ""
			%>><%=
				thisChapNum
			%><%
        }
		
		if (foundChapter)
		{
			session.setAttribute("chapterNum", new Integer(selChapNum));
		}
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
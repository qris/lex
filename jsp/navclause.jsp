<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.hebrew.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>
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

	String selBook = "Genesis";
	
	{
		String selBook2 = (String)( session.getAttribute("book") );
		if (selBook2 != null) 
		{
			selBook = selBook2;
		}
	}
	
	{ 
		String selBook2 = request.getParameter("book");
		if (selBook2 != null) 
		{
			selBook = selBook2;
		}
	} 

	{
		boolean foundBook = false;
	
		Table bookTable = emdros.getTable
		(
			"SELECT OBJECTS HAVING MONADS IN " + 
			emdros.intersect(userTextAccessSet, min_m, max_m) +
			" [book]"
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
			"GET FEATURES book FROM OBJECTS WITH ID_DS = " + 
			id_dList.toString() + " [book]"
		);

		rows = featureTable.iterator();
        while (rows.hasNext()) 
        {
        	TableRow row = rows.next();
			String thisBook = row.getColumn(2);
			
			if (thisBook.equals(selBook))
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
				if (min_m > max_m) max_m = min_m + 1;
				// System.out.println("book restricts to " + min_m + "-" + max_m);
			}
				
			%>
			<option <%=
				thisBook.equals(selBook) ? "SELECTED" : ""
			%> value="<%= 
				row.getColumn(2) 
			%>"><%= 
				row.getColumn(2)
			%><%
        }

		if (foundBook)
		{
			session.setAttribute("book", selBook);
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
		selChapNum = Integer.parseInt(request.getParameter("chapter"));
	} 
	catch (Exception e) { /* ignore it and use default chapter */ }
	
	{
		boolean foundChapter = false;

		Table chapterTable = emdros.getTable
		(
			"SELECT OBJECTS HAVING MONADS IN " + 
			emdros.intersect(userTextAccessSet, min_m, max_m) +
			" [chapter]"
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
				System.out.println("before chapter was " + min_m + "-" + max_m);
				if (min_m < new_min_m) min_m = new_min_m;
				if (max_m > new_max_m) max_m = new_max_m;
				if (min_m > max_m) max_m = min_m + 1;
				System.out.println("chapter restricts to " + min_m + "-" + max_m);
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
	System.out.println("4");
	
	{
		Integer sessionVerseNum = 
			(Integer)( session.getAttribute("verseNum") );
		if (sessionVerseNum != null)
			selVerseNum = sessionVerseNum.intValue();
	}
	
	try
	{ 
		selVerseNum = Integer.parseInt(request.getParameter("verse")); 
	}
	catch (Exception e) { /* ignore it and use default chapter */ }
	
	{
		boolean foundVerse = false;
	
		FlatSheaf sheaf = emdros.getFlatSheaf
		(
			"GET OBJECTS HAVING MONADS IN " +
			emdros.intersect(userTextAccessSet, min_m, max_m) +
			"[verse GET verse, verse_label]"
		);

		FlatStrawConstIterator sci = 
			sheaf.const_iterator().next().const_iterator();
			
		while (sci.hasNext())
		{
			MatchedObject verse = sci.next();
			
			int thisVerseNum = verse.getEMdFValue("verse").getInt();
			if (thisVerseNum == selVerseNum)
			{
				foundVerse = true;
				SetOfMonads som = new SetOfMonads();
				verse.getSOM(som, false);
				min_m = som.first();
				max_m = som.last();
				// System.out.println("verse restricts to " + min_m + "-" + max_m);
			}
							
			%>
			<option value="<%=
				thisVerseNum
			%>"<%=
				thisVerseNum == selVerseNum ? " selected=\"selected\"" : ""
			%>><%=
				verse.getEMdFValue("verse_label").getString()
			%></option><%
		}
		
		if (foundVerse)
		{
			session.setAttribute("verseNum", new Integer(selVerseNum));
		}
	}

%>
	</select>
  </td>
  <td>
	<select name="clause" onChange="document.forms.nav.submit()">
<%
	int selClauseId = 0;
		System.out.println("5");
	
	{
		Integer sessionClauseId = 
			(Integer)( session.getAttribute("clauseId") );
		if (sessionClauseId != null)
			selClauseId = sessionClauseId.intValue();
	}
	
	try
	{ 
		selClauseId = Integer.parseInt(request.getParameter("clause"));
	}
	catch (Exception e) { /* ignore it and use default chapter */ }
	
	MatchedObject verse = null;
	
	{
		boolean foundSelectedClause = false;
		int defaultClauseId = 0;
		HebrewMorphemeGenerator generator = new HebrewMorphemeGenerator();
	
		Sheaf sheaf = emdros.getSheaf
		(
			"SELECT ALL OBJECTS IN " +
			emdros.intersect(userTextAccessSet, min_m, max_m) +
			" WHERE " +
			"[clause "+
			" [word GET phrase_dependent_part_of_speech, " +
            "  graphical_preformative, " +
            "  graphical_root_formation, " +
            "  graphical_lexeme, " +
            "  graphical_verbal_ending, " +
            "  graphical_nominal_ending, " +
            "  graphical_pron_suffix" +
			" ]"+
			"]");
			 
		SheafConstIterator clause_iter =
			sheaf.const_iterator();
				
		while (clause_iter.hasNext())
		{
			MatchedObject clause =
				clause_iter.next().const_iterator().next();

			String lexemes = "";
				
			SheafConstIterator word_iter =
				clause.getSheaf().const_iterator();
					
			while (word_iter.hasNext())
			{
				MatchedObject word = word_iter.next().const_iterator().next();
						
				lexemes += HebrewConverter.wordTranslitToHtml(word, generator);
					
				if (word_iter.hasNext()) 
				{
					lexemes += " ";
				}
			}
				
			int thisClauseId = clause.getID_D();
			if (thisClauseId == selClauseId)
			{
				foundSelectedClause = true;
			}
					
			if (defaultClauseId == 0)
			{
				defaultClauseId = thisClauseId;
			}
					
			%>
			<option value=<%=
				thisClauseId
			%><%=
				thisClauseId == selClauseId ? " SELECTED" : ""
			%>><%=
				lexemes
			%><%
		}
		
		if (!foundSelectedClause)
		{
			selClauseId = defaultClauseId;
		}
			
		session.setAttribute("clauseId", new Integer(selClauseId));
	}

%>
	</select>
  </td>
</tr>
</table>
</form>
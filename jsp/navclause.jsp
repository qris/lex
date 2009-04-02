<%@ page import="com.qwirx.lex.controller.*" %>
<%@ page import="com.qwirx.lex.translit.*" %>

<%
DatabaseTransliterator transliterator = new DatabaseTransliterator(sql);
Navigator navigator = new Navigator(request, session, emdros,
	userTextAccessSet, transliterator);
%>
	
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
	<td><%= navigator.getObjectNavigator("book", "book") %></td>
	<td><%= navigator.getObjectNavigator("chapter", "chapter") %></td>
	<td><%= navigator.getObjectNavigator("verse",
		new String[]{"verse_label", "verse"})%></td>
	<td><%= navigator.getClauseNavigator()%></td>
</tr>
</table>
</form>
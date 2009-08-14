<%@ page import="com.qwirx.lex.controller.*" %>
<%@ page import="com.qwirx.lex.translit.*" %>
<%@ page import="org.aptivate.web.controls.SelectBox" %>
<%
DatabaseTransliterator transliterator = new DatabaseTransliterator(sql);
Navigator navigator = new Navigator(request, session, emdros,
	userTextAccessSet, transliterator);
SelectBox [] navControls = new SelectBox[]{
	navigator.getObjectNavigator("book", "book"),
	navigator.getObjectNavigator("chapter", "chapter"),
	navigator.getObjectNavigator("verse", new String[]{"verse_label", "verse"}),
	navigator.getClauseNavigator()
};
%>
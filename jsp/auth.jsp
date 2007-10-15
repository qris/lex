<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<%@ page import="com.qwirx.lex.sql.*" %>
<%@ page import="jemdros.SetOfMonads" %>
<%	
	SetOfMonads userTextAccessSet = emdros.getVisibleMonads();
	
	if (userTextAccessSet == null)
	{
		if (username.equals("anonymous"))
		{
			%>
	Sorry, you must log in to access this database.
			<%
		}
		else
		{
			%>
	Sorry, you do not have access to this database.
			<%
		}

		return;
	}
%>
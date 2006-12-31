<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<%@ page import="com.qwirx.lex.sql.*" %>
<%

	String username = request.getRemoteUser();
	if (username == null)
	{
		username = "anonymous";
	}
	String hostname = request.getRemoteAddr();
	
	String userhost = username + "@" + hostname;
	SqlDatabase    sql     = Lex.getSqlDatabase   (userhost);
	EmdrosDatabase emdros  = Lex.getEmdrosDatabase(username, hostname);
	int min_m = emdros.getMinM(), max_m = emdros.getMaxM();
	
	String userTextAccess = emdros.getVisibleMonadString();
	
	if (userTextAccess == null)
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
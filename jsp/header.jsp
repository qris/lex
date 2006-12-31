<%@ page import="java.net.URLEncoder" %>
<h1>Lex</h1>
<%
	String ownUrl = request.getRequestURI();
	{
		String query = request.getQueryString();
		if (query != null)
		{
			ownUrl += "?" + query;
		}
	}	
%>
<div class="topmenu">
	<a 	class="index_jsp"  href="index.jsp">Home</a><a 
		class="db_jsp"     >Databases</a><a 
		class="clause_jsp" href="clause.jsp">Text</a><a 
		class="lsedit_jsp" href="lsedit.jsp">Lexicon</a><a  
		class="dump_jsp"   href="dump.jsp"  >Database Dump</a><a  
		class="login_jsp"   
	<%
	if (request.getRemoteUser() != null)
	{
		%>>Logged in as <strong><%= request.getRemoteUser() %></strong><%
	}
	else
	{
		%>href="login.jsp?next=<%= URLEncoder.encode(ownUrl) %>">Login<%
	}
	%></a><div class="clearer"></div>
</div>
<%@ page import="java.net.URLEncoder" %>
<h1>Lex <font style="font-size: medium">by <a href="http://www.qwirx.com">Chris Wilson</a></font></h1>
<h2>RLM for BH: 
	<a href="http://www.winthernielsen.dk">Nicolai Winther-Nielsen</a></h2>
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
		class="parse_jsp"  href="parse.jsp" >Parser</a><a  
		class="rules_jsp"  href="rules.jsp" >Rules</a><a  
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
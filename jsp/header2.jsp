<%
	response.setCharacterEncoding("UTF-8");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>

<head>
	<title>Lex: <%= pageTitle %></title>
	<link rel="stylesheet" href="style.css"/>
	<style type="text/css">
		div.topmenu a.<%= 
			request.getServletPath().substring(1).replaceAll("\\.", "_")
		%> <%@ include file="hilite.inc" %>
	</style>
</head>

<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<%@ page import="com.qwirx.db.*" %>
<%@ page import="com.qwirx.db.sql.*" %>

<body>
<h1>Lex <font style="font-size: medium">by 
	<a href="http://www.qwirx.com">Chris Wilson</a></font></h1>
<h2>RLM for BH: 
	<a href="http://www.winthernielsen.dk">Nicolai Winther-Nielsen</a></h2>
<div class="topmenu">
	<a 	class="index_jsp"  href="index.jsp">Home</a><a 
		class="db_jsp"     >Databases</a><a 
		class="published_jsp" href="published.jsp">Published</a><a 
		class="clause_jsp" href="clause.jsp">Browse</a><a 
		class="search_jsp" href="search.jsp">Search</a><a 
		class="lsedit_jsp" href="lsedit.jsp">Lexicon</a><a  
		class="parse_jsp"  href="parse.jsp" >Parser</a><a  
		class="rules_jsp"  href="rules.jsp" >Rules</a><%
		
		if (request.getRemoteUser() != null)
		{
			%><a class="dump_jsp" href="dump.jsp">Database Dump</a><%
		}
	
		%><a class="login_jsp" <%
		
		if (request.getRemoteUser() != null)
		{
			%>>Logged in as <strong><%= request.getRemoteUser() %></strong><%
		}
		else
		{
			String ownUrl = request.getRequestURI().replaceFirst(".*/", "");
			String query  = request.getQueryString();
			if (query != null)
			{
				ownUrl += "?" + query;
			}
			%>href="login.jsp?next=<%= URLEncoder.encode(ownUrl) %>">Login<%
		}
	
	%></a><div class="clearer"></div>
</div>
<%

	String username = request.getRemoteUser();
	if (username == null)
	{
		username = "anonymous";
	}

	String hostname = request.getRemoteAddr();
	String userhost = username + "@" + hostname;

	SqlDatabase    sql    = Lex.getSqlDatabase(userhost);
	EmdrosDatabase emdros = null;
		
	try
	{
		emdros = Lex.getEmdrosDatabase(username, hostname);
		int min_m = emdros.getMinM(), max_m = emdros.getMaxM();
		int real_min_m = min_m, real_max_m = max_m;
		
%>
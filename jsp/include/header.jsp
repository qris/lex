<%
	response.setCharacterEncoding("UTF-8");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
	<title>Lex: <%= pageTitle %></title>
	<link rel="stylesheet" href="css/style.css"/>
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
<h1>Lex <span style="font-size: medium">by 
	<a href="http://www.qwirx.com">Chris Wilson</a></span></h1>
<h2>RLM for BH: 
	<a href="http://3bm.dk/index.php?p=3">Nicolai Winther-Nielsen</a></h2>
<div class="topmenu">
	<a 	class="index_jsp"     href="index.jsp"    >Home</a><a 
		class="db_jsp"                            >Databases</a><a 
		class="published_jsp" href="published.jsp">Published</a><a 
		class="clause_jsp"    href="clause.jsp"   >Browse</a><a 
		class="search_jsp"    href="search.jsp"   >Search</a><a 
		class="lexicon_jsp"   href="lexicon.jsp"  >Lexicon</a><a  
		class="parse_jsp"     href="parse.jsp"    >Parser</a><a  
		class="rules_jsp"     href="rules.jsp"    >Rules</a><!--<a  
		class="wordnet_jsp"   href="wordnet.jsp"  >Wordnet</a>--><%
		
		if (request.getRemoteUser() != null)
		{
			%><a class="dump_jsp" href="dump.jsp">Database Dump</a><%
		}
	
		%><a 
		class="login_jsp" <%
		
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
	
	%></a><div 
	class="clearer"></div>
</div>

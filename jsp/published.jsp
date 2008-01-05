<% String pageTitle = "Published Data"; %>
<%@ include file="header2.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="com.qwirx.lex.Search" %>
<%@ page import="com.qwirx.lex.Search.SearchResult" %>
<%@ page import="com.qwirx.lex.hebrew.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="org.aptivate.webutils.EditField" %>

<%
	EditField field = new EditField(request);
	List<String> books = emdros.getEnumerationConstantNames("book_name_e");
	
	int maxResults = 100;
	
	try
	{
		maxResults = Integer.parseInt(request.getParameter("max_results"));
	}
	catch (Exception e)
	{
		// do nothing
	}
	
	Search search = new Search(emdros);
	search.setMaxResults(maxResults);
	List<SearchResult> results = search.advanced("published = 1");
%>

<h3>Published Clauses</h3>

<%
	if (results.size() == 0)
	{
		%><h4>No matches found</h4><%
	}
	else
	{
		int numShown = search.getResultCount();
		%><h4>Displaying first <%= results.size() %> of
		<%= search.getResultCount() %> results.</h4><%
		%>
		<table class="search_results">
			<tr>
				<th>Verb</th>
				<th>Logical Structure</th>
				<th>Reference</th>
			</tr>
		<%
		
		for (Iterator<SearchResult> i = results.iterator(); i.hasNext();)
		{
			SearchResult result = i.next();
			
			%>
			<tr>
				<td><%=
					HebrewConverter.toHebrew(result.getPredicate())
				%></td>
				<td><%=
					HebrewConverter.toHtml(result.getLogicalStructure())
				%></td>
				<td><a href="<%= result.getLinkUrl() %>"><%=
					result.getLocation()
				%></a></td>
			</tr>
			<%
		}
		
		%>
		</table>
		<%
	}
%>

<%@ include file="footer.jsp" %>
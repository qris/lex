<%@ include file="include/setup.jsp" %>

<% String pageTitle = "Search"; %>
<%@ include file="include/header.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="com.qwirx.lex.Search" %>
<%@ page import="com.qwirx.lex.Search.SearchResult" %>
<%@ page import="com.qwirx.lex.hebrew.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>
<%@ page import="com.qwirx.lex.translit.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="org.aptivate.web.utils.EditField" %>

<script type="text/javascript"><!--

function enableLimitControls()
{
	var form = document.forms.simple;
	form.book.disabled        = !form.limit_loc.checked;
	// form.max_results.disabled = !form.limit_num.checked;
}

function toggle(button, divid)
{
    div = document.getElementById(divid);
    if (div.style.display == "block")
	{
		div.style.display = "none";
    	button.value = button.value.replace("«", "»");
	}
	else
	{
		div.style.display = "block";
    	button.value = button.value.replace("»", "«");
	}
}

//--></script>

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
	
	String query  = null;
	Search search = null;
	DatabaseException exception = null;
	boolean simpleSearch   = (request.getParameter("simple")   != null);
	boolean advancedSearch = (request.getParameter("advanced") != null);
	List<SearchResult> results = null;
	DatabaseTransliterator transliterator;
	
	if (simpleSearch || advancedSearch)
	{
		transliterator = new DatabaseTransliterator(sql);
		search = new Search(emdros, transliterator);
		search.setMaxResults(maxResults);
		
		if (simpleSearch)
		{
			query = request.getParameter("q");
			results = search.basic(query);
		}
		else if (advancedSearch)
		{
			query = request.getParameter("aq");
			// results = search.advanced(query);
			
			try
			{
				results = search.advanced(query);
			}
			catch (DatabaseException e)
			{
				exception = e;
			}
		}
	}
%>

<form name="simple" method="GET" class="bigborder">
	<p>
		Simple search (enter surface consonants for a Hebrew word):
	</p>
	<p>
		<%= field.text("q", "", true, 40) %>
		<input type="submit" name="simple" value="Search" />
		<%
		if (simpleSearch && request.getParameter("q").equals(""))
		{
			results = null;
			%><div class="errormsg">Please enter a word to search for.</div><%
		}
		%>
	</p>
	<hr />
	<p>
		Advanced search (enter an MQL query to nest within [clause]):
	</p>
	<p>
		<%= field.text("aq").setAttribute("size", "40") %>
		<input type="submit" name="advanced" value="Search" />
		<%
		if (advancedSearch)
		{
			if (request.getParameter("aq").equals(""))
			{
				results = null;
				%><div class="errormsg">Please enter an MQL query.</div><%
			}
			else if (exception != null)
			{
				%>
				<div class="errormsg">
				<%= exception.getCause().getMessage()
					.replaceAll("\n(.)", ": $1") %>
				</div>
				<%
			}
		}
		%>
	</p>
	<hr />
	<div id="simple_adv_div" class="advanced">
	<p>
		<%= field.checkbox("limit_loc").setAttribute("onclick", 
			"return enableLimitControls()") %>
		Limit search to <%= field.select("book", books) %>
	</p>
	<p>
		Return only the first
		<%= field.text("max_results", "100", true, 5) %>
		clauses		
	</p>
	<hr />
	<p>
		Three Latin consonants are expected, in upper or lower case,
		from the set:
	</p>
	<table class="grid">
		<tr>
			<th>Consonants</th>
			<%
				String latin  = ">BGDHWZXVJKLMNS<PYQRFCT";
				
				for (int i = 0; i < latin.length()	; i++)
				{
					String c = latin.substring(i, i + 1).replaceAll("<", "&lt;");
					%>
					<td><%= c %></td>
					<%
				}
			%>
		</tr>
	</table>
	</div>
	<div>
		<input type="button" name="simple_adv_btn" 
			value="Advanced Options &raquo;"
			onclick="toggle(this, 'simple_adv_div')" />
	</div>
</form>

<script type="text/javascript"><!--
	enableLimitControls();
//--></script>

<%
	if (search != null && results != null)
	{		
		%>
		<h3>Search Results for <em><%= query %></em></h3>
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
					<th>Clause Text</th>
					<th>Reference</th>
				</tr>
			<%
		
			for (Iterator<SearchResult> i = results.iterator(); i.hasNext();)
			{
				SearchResult result = i.next();
				
				%>
				<tr>
					<td class="translit"><%= result.getDescription() %></td>
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
	}	
%>

<%@ include file="include/footer.jsp" %>

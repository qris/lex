<% String pageTitle = "Search"; %>
<%@ include file="header2.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="com.qwirx.lex.Search" %>
<%@ page import="com.qwirx.lex.Search.SearchResult" %>
<%@ page import="com.qwirx.lex.hebrew.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="org.aptivate.webutils.EditField" %>

<script type="text/javascript"><!--

function enableLimitControls()
{
	var form = document.forms.simple;
	form.book.disabled        = !form.limit_loc.checked;
	// form.max_results.disabled = !form.limit_num.checked;
}

//--></script>

<%
	EditField field = new EditField(request);
	List<String> books = emdros.getEnumerationConstantNames("book_name_e");
%>

<form name="simple" method="GET" class="bigborder">
	<p>
		Search for Hebrew word by root (surface consonants):
	</p>
	<p>
		<%= field.text("q", "", true, 40) %>
		<input type="submit" value="Search" />
	</p>
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
	<%
		String latin  = ">BGDHWZXVJKLMNS<PYQRFCT";
		String hebrew = HebrewConverter.toHebrew(latin);
		String trans  = HebrewConverter.toTranslit(latin);
	%>
	<table class="grid">
		<tr>
			<th>Consonants</th>
			<%
				for (int i = 0; i < latin.length()	; i++)
				{
					String c = latin.substring(i, i + 1).replaceAll("<", "&lt;");
					%>
					<td><%= c %></td>
					<%
				}
			%>
		</tr>
		<tr>
			<th>Hebrew</th>
			<%
				for (int i = 0; i < hebrew.length(); i++)
				{
					String c = hebrew.substring(i, i + 1);
					%>
					<td class="hebrew"><%= c %></td>
					<%
				}
			%>
		</tr>
		<tr>
			<th>Transliteration</th>
			<%
				for (int i = 0; i < trans.length(); i++)
				{
					String c = trans.substring(i, i + 1);
					%>
					<td><%= c %></td>
					<%
				}
			%>
		</tr>
	</table>
</form>

<script type="text/javascript"><!--
	enableLimitControls();
//--></script>

<%
	String query = request.getParameter("q");
	if (query != null)
	{
		List<SearchResult> results = new Search(query, emdros).run();
		
		%>
		<table class="grid">
		<%
	
		for (Iterator<SearchResult> i = results.iterator(); i.hasNext();)
		{
			SearchResult result = i.next();
			
			%>
			<tr>
				<th class="verse"><%= result.getLocation() %></th>
				<td class="translit"><%= result.getDescription() %></td>
				<td><a href="<%= result.getLinkUrl() %>">Open</a></td>
			</tr>
			<%
		}
	
		%>
		</table>
		<%
	}	
%>
<%@ include file="footer.jsp" %>

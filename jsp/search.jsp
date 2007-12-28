<% String pageTitle = "Search"; %>
<%@ include file="header2.jsp" %>
<%@ page import="com.qwirx.lex.hebrew.HebrewConverter" %>

<form method="POST" class="bigborder">
	<p>
		Search for Hebrew word by root (surface consonants):
	</p>
	<p>
		<input name="q" size="60" />
		<input type="submit" value="Search" />
	</p>
	<p>
		Three Latin consonants are expected, in upper or lower case,
		from the set:
	</p>
	<%
		String latin = ">BGDHWZXVJKLMNS<PYQRFCT";
		String hebrew = HebrewConverter.toHebrew(latin);
	%>
	<table border="1">
		<tr>
			<%
				for (int i = 0; i < latin.length()	; i++)
				{
					String c = latin.substring(i, i + 1).replaceAll("<", "&lt;");
					%>
					<th><%= c %></th>
					<%
				}
			%>
		</tr>
		<tr class="hebrew">
			<%
				for (int i = 0; i < hebrew.length(); i++)
				{
					String c = hebrew.substring(i, i + 1);
					%>
					<th><%= c %></th>
					<%
				}
			%>
		</tr>
	</table>
</form>

<%@ include file="footer.jsp" %>

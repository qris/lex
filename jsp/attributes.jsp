<%@ page import="java.util.Enumeration" %>
<html><body>
<h1>Session Attributes</h1>
<table>
<%
	Enumeration e = session.getAttributeNames();
	while (e.hasMoreElements()) {
		String name = (String)(e.nextElement());
		Object value = session.getAttribute(name);
		%>
		<tr>
			<td><%= name  %></td>
			<td><%= value %></td>
		</tr>
		<%
	}
%>
</table>
</body></html>
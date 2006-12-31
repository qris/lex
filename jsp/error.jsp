<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<html>
<head>
<title>Lex: Error</title>
<body>
<h1>Lex: Error</h1>
<p>
<a href="http://lex.qwirx.com/lex/"><b>Lex</b></a> | 
<a href="clause.jsp">Clause Editor</a> | 
<a href="lsedit.jsp">Logical Structure Editor</a>
</p>
<p>
Sorry, you found a bug in Lex. Please report it 
to the author by sending an email to Chris Wilson at
<a href="mailto:lex_error@qwirx.com">lex_error@qwirx.com</a>.
</p>
<p><%
	Throwable exception = (Throwable)(
		request.getAttribute(
			"javax.servlet.jsp.jspException"));
%></p>

<p>The error was: <b><%= exception == null ? "null" : exception.toString() %></b>.
</p>

<%
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	if (exception != null) exception.printStackTrace(pw);
%>
<pre>
<%= sw.toString() %>
</pre>
</body>
</html>
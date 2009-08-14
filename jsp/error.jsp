<% String pageTitle = "Error"; %>
<%@ include file="header2.jsp" %>

<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>

<p>
Sorry, you found a bug in Lex. Please report it 
to the author by sending an email to Chris Wilson at
<a href="mailto:chris-lexerror@qwirx.com">chris-lexerror@qwirx.com</a>.
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

<%@ include file="include/footer.jsp" %>
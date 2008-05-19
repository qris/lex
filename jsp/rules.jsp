<% String pageTitle = "Parser Rules"; %>
<%@ include file="header2.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="com.qwirx.lex.ontology.*" %>
<%@ page import="com.qwirx.lex.wordnet.*" %>
<%@ page import="com.qwirx.lex.parser.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>

<script language="javascript">
<!--
	
	function confirm_delete()
	{
		return confirm("Are you sure you want to delete this rule?");
	}
	
//-->
</script>

<%@ include file="auth.jsp" %>

<%

    int editRuleId = -1;
    
    {
    	String editRuleIdParam = request.getParameter("erid");
    	if (editRuleIdParam != null)
    	{
    		editRuleId = Integer.parseInt(editRuleIdParam);
    	}
    }
    
	if (request.getParameter("save") != null)
	{
		Change ch = sql.createChange(SqlChange.UPDATE, 
				"lexicon_entries", "ID = " + editRuleId);
		ch.setString("Symbol", request.getParameter("lhs"));
		ch.setString("Lexeme", request.getParameter("rhs"));
		ch.execute();		
	}
	else if (request.getParameter("delete") != null)
	{
		Change ch = sql.createChange(SqlChange.DELETE, 
				"lexicon_entries", "ID = " + editRuleId);
		ch.execute();		
	}

    sql.prepareSelect
    	("SELECT ID,Symbol,Lexeme FROM lexicon_entries " +
         "WHERE Symbol IS NOT NULL " +
         "AND   Lexeme IS NOT NULL " +
         "ORDER BY Symbol, Lexeme");
         
    ResultSet rs = sql.select();
    
    if (rs.isLast())
    {
    	%><h3>No rules found in database!</h3><%
    }
    else
	{ 
		%>
		<form name="edit_rule_form" method="post"
			action="rules.jsp#rule_<%= editRuleId %>">
		<table border="1">
		<th>ID</th>
		<th>Left-Hand Side</th>
		<th>Right-Hand Side</th>
		<th>Actions</th>
		<%
		
	    while (rs.next()) 
    	{
    		int ruleId = rs.getInt(1);
    		
    		if (ruleId == editRuleId)
    		{
				%>
				<tr class="hilite">
				<%
    		}
    		else
    		{			
				%>
				<tr>
				<%
    		}

			%>			
			<th id="rule_<%= ruleId %>"><%= ruleId %></th>
			<%
			
			if (ruleId == editRuleId)
			{
				%>
				<td><input name="lhs" size="10" value="<%= rs.getString(2) %>" /></td>
				<td><input name="rhs" size="40" value="<%= rs.getString(3) %>" /></td>
				<td>
					<input type="hidden" name="erid" value="<%= ruleId %>" />
					<input type="submit" name="save" value="Save" />
					<input type="submit" name="delete" value="Delete" 
						onClick="return confirm_delete()" />
				</td>
				<%
			}
			else
			{
				%>
				<td><%= rs.getString(2) %></td>
				<td><%= rs.getString(3) %></td>
				<td><a href="rules.jsp?erid=<%= rs.getInt(1) %>#rule_<%= 
					ruleId %>">Edit</a></td>
				<%
			}
			
			%>			
			</tr>
			<%
		}
		
		%>
		</table>
		</form>
		<%
    }	
%>

<%@ include file="footer.jsp" %>
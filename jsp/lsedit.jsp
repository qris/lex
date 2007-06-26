<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Vector" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.io.OutputStream" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.lex.sql.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<%@ page import="com.qwirx.lex.lexicon.*" %>
<%@ page import="com.qwirx.lex.lexicon.Lexeme.Variable" %>
<% long startTime = System.currentTimeMillis(); %>
<html>
<head>
<title>Lex: Edit Logical Structure</title>
<link rel="stylesheet" href="style.css"/>
<style>
	h4 { margin-top: 8pt; margin-bottom: 0pt; }
	div.topmenu a.lsedit_jsp <%@ include file="hilite.inc" %>
</style>
</head>
<body>

<%@ include file="header.jsp" %>
<%@ include file="auth.jsp" %>

<%	
	class LogicalStructureList extends Lexeme.Visitor
	{
		private JspWriter out;
		private int selected_id, exclude_domain_id;
		
		LogicalStructureList(JspWriter out, 
			int selected_id, int exclude_domain_id, 
			Lexeme root)
		{
			super(root);
			this.out = out;
			this.selected_id = selected_id;
			this.exclude_domain_id = exclude_domain_id;
		}
		
        protected void visit(Lexeme e, String parentPath) throws IOException 
        {
			if (e.id == exclude_domain_id) return;
			super.visit(e, parentPath);
        }
        
		protected void output(Lexeme e, String fullPath, String desc)
		throws IOException
		{
			if (desc.length() > 60)
			{
				desc = desc.substring(0, 60) + "...";
			}
			
			if (e.surface != null)
			{
				desc += ": " + e.surface + "";
			}
			
			String ls = e.getLogicalStructure();
			if (ls != null) 
			{
				desc += ": " + ls;
			}
			
			desc = desc.replaceAll("<", "&lt;").replaceAll(">", "&gt;");	
		
			%>
			<option <%= selected_id == e.id ? "SELECTED" : ""
			%> value=<%= e.id == 0 ? "BadID" : (e.id+"") %>>
			<%= fullPath %> <%= desc %>
			<%
		}		
	}

	class LogicalStructureArray extends Lexeme.Visitor
	{
		private JspWriter out;
		private int selected_id, exclude_domain_id;

		LogicalStructureArray(JspWriter out, 
			int selected_id, Lexeme root)
		{
			super(root);
			this.out = out;
			this.selected_id = selected_id;
		}

		protected void output(Lexeme e, String fullPath, String desc)
		throws IOException
		{
			String ls = e.getLogicalStructure();

			if (e.surface != null && ! e.surface.equals(""))
			{
				desc += ": " + e.surface + "";
			}
						
			if (ls != null && ! ls.equals("")) 
			{
				desc += ": " + ls;
			}

			%>
			[ <%= e.id %>, "<%= fullPath %> <%= desc %>" ],
			<%
		}		
	}

	int lsId = -1;
	try { lsId = Integer.parseInt(request.getParameter("lsid")); }
	catch (Exception e) { /* do nothing, use default */ }
	
	// String current.surface = "", current.logic = "";
	// int domain_parent_id = 0;
	// String current.label = "", current.desc = "";
	
	%><p><em><%
	
	if (request.getParameter("savecopy") != null)
	{
		throw new AssertionError("broken");
		/*
		boolean createNew = false;
		
		if (lsId <= 0) 
			createNew = true;
		if (request.getParameter("savecopy") != null)
			createNew = true;
			
		try {

			if (request.getParameter("surface") != null)
			{
				ch.setString("Lexeme",       request.getParameter("surface"));
			}
			ch.setString("Structure",        request.getParameter("ls"));
			ch.setInt   ("Domain_Parent_ID", domain_parent_id);
			ch.setString("Domain_Label",     request.getParameter("dl"));
			ch.setString("Domain_Desc",      request.getParameter("dd"));
			ch.setString("Syntactic_Args",   request.getParameter("sa"));
			ch.execute();
			
			if (createNew) {
				lsId = ((SqlChange)ch).getInsertedRowId();
			}
		} catch (SQLException sqlEx) {
			%><%= sqlEx %><%
		}
		*/
	}
	else if (request.getParameter("delrepl") != null)
	{
		try {
			int newLsId = Integer.parseInt(request.getParameter("newlsid"));
			
			Sheaf sheaf = emdros.getSheaf
				("SELECT ALL OBJECTS "+
				 "IN {" + emdros.getMinM() + "-" + emdros.getMaxM() + "} "+
				 "WHERE [clause logical_struct_id = "+lsId+"]");
			
			SheafConstIterator sci = sheaf.const_iterator();
			if (sci.hasNext()) {
				Vector objectIds = new Vector();
	
				while (sci.hasNext()) {
					Straw straw = sci.next();
					MatchedObject clause = straw.const_iterator().next();
					objectIds.add(new Integer(clause.getID_D()));
				}

				int [] objectIdArray = new int[objectIds.size()];
				for (int i = 0; i < objectIds.size(); i++) {
					objectIdArray[i] = 
						((Integer)( objectIds.get(i) )).intValue();
				}
				
				Change ch = emdros.createChange(
					EmdrosChange.UPDATE, "clause", objectIdArray);
				ch.setInt("logical_struct_id", newLsId);
				ch.execute();
			}

			sql.createChange(SqlChange.DELETE,
				"lexicon_entries", "ID = "+lsId).execute();
			lsId = newLsId;
		} 
		catch (Exception e) 
		{
			%><%= e %><%
		}
	} 
	else if (request.getParameter("vcu") != null) 
	{
		// variable create or update
		
		boolean createVar = false;
		int vid = 0;
		if (request.getParameter("vid") != null) {
			try {
				vid = new Integer(request.getParameter("vid")).intValue();
			} catch (NumberFormatException e) {
				vid = 0;
			}
		}
		if (vid == 0) createVar = true;
		
		try {
			String query = 
				"SELECT ID FROM lexicon_variables WHERE Name = ? "+
				"AND Lexeme_ID = ?";

			if (!createVar)
				query += " AND ID <> ?";

			PreparedStatement stmt = sql.prepareSelect(query);
			stmt.setString(1, request.getParameter("vn"));
			stmt.setInt   (2, lsId);
			if (!createVar)
				stmt.setInt(3, vid);

			ResultSet rs = sql.select();
			boolean alreadyExists = rs.next();
			sql.finish();
			
			if (alreadyExists) {
				%>
					Duplicate variable name 
					"<%= request.getParameter("vn") %>"
				<%
			} else {
				Change ch;
				
				if (createVar) {
					ch = sql.createChange(SqlChange.INSERT,
						"lexicon_variables", null);
				} else {
					ch = sql.createChange(SqlChange.UPDATE,
						"lexicon_variables", "ID = "+vid);
				}

				ch.setString("Name",      request.getParameter("vn"));
				ch.setString("Value",     request.getParameter("vv"));
				ch.setInt   ("Lexeme_ID", lsId);
				ch.execute();
			}
		} catch (DatabaseException sqlEx) {
			%><%= sqlEx %><%
		}
	} 
	else if (request.getParameter("vd") != null) 
	{
		// variable delete 
		
		int vid = 0;

		if (request.getParameter("vid") != null) {
			try {
				vid = new Integer(request.getParameter("vid")).intValue();
			} catch (NumberFormatException e) {
				vid = 0;
			}
		}

		if (vid != 0) {
			try {
				sql.createChange(SqlChange.DELETE,
					"lexicon_variables", "ID = "+vid).execute();
			} catch (DatabaseException sqlEx) {
				%><%= sqlEx %><%
			}
		}
	}
	else if (request.getParameter("dpid") != null)
	{
		int domain_parent_id = new Integer(request.getParameter("dpid"))
			.intValue();
				
		// parent hierarchy loop check

		if (domain_parent_id == lsId) 
		{
			domain_parent_id = 0;
		}
		
		if (domain_parent_id > 0) 
		{
			int maxDepth = 20;
			int thisAncestor = domain_parent_id;
			
			while (maxDepth > 0) 
			{
				try 
				{
					PreparedStatement stmt = sql.prepareSelect
						("SELECT Domain_Parent_ID "+
						"FROM lexicon_entries "+
						"WHERE ID = ?");
					stmt.setInt(1, thisAncestor);
					ResultSet rs = sql.select();
					if (!rs.next()) {
						// parent tree has no path to root?
						domain_parent_id = 0;
						break;
					}
					thisAncestor = rs.getInt(1);
					if (thisAncestor == 0) {
						// reached the root
						break;
					}
					if (thisAncestor == lsId) {
						// loop detected
						domain_parent_id = 0;
						break;
					}								
					maxDepth--;
				} finally {
					sql.finish();
				}
			}

			if (maxDepth == 0) {
				%>
				You cannot set the domain parent to one of this
				object's children: that would create a loop!
				<%
				domain_parent_id = 0;
			}
		}
		
		Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
			"ID = " + lsId);
		ch.setInt("Domain_Parent_ID", domain_parent_id);
		ch.execute();
	}
	else if (request.getParameter("dl") != null)
	{
		Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
			"ID = " + lsId);
		ch.setString("Domain_Label", request.getParameter("dl"));
		ch.execute();
	}
	else if (request.getParameter("dd") != null)
	{
		Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
			"ID = " + lsId);
		ch.setString("Domain_Desc", request.getParameter("dd"));
		ch.execute();
	}
	else if (request.getParameter("sa") != null)
	{
		Change ch = sql.createChange(SqlChange.UPDATE, "lexicon_entries", 
			"ID = " + lsId);
		ch.setString("Syntactic_Args",   request.getParameter("sa"));
		ch.execute();
	}
	else if (request.getParameter("ls_save") != null)
	{
		Lexeme lexeme = new Lexeme(sql);
		
		if (lsId != -1)
		{
			lexeme = Lexeme.load(sql, lsId);
		}
		
		lexeme.setCaused          (request.getParameter("ls_caused") != null);
		lexeme.setPunctual        (request.getParameter("ls_punct")  != null);
		lexeme.setHasResultState  (request.getParameter("ls_punct_result") != null &&
		                           request.getParameter("ls_punct_result").equals("1"));
		lexeme.setTelic           (request.getParameter("ls_telic") != null);
		lexeme.setDynamic         (request.getParameter("ls_dynamic") != null &&
		                           request.getParameter("ls_dynamic").equals("1"));
		lexeme.setHasEndpoint     (request.getParameter("ls_endpoint") != null &&
		                           request.getParameter("ls_endpoint").equals("1"));
		
		String pred = request.getParameter("ls_pred");
		if (pred != null && pred.equals(""))
		{
			pred = null;
		}
		lexeme.setPredicate(pred);
		
		pred = request.getParameter("ls_pred_2");
		if (pred != null && pred.equals(""))
		{
			pred = null;
		}
		lexeme.setResultPredicate(pred);

		String arg2 = request.getParameter("ls_arg_2");
		if (arg2 != null && arg2.equals(""))
		{
			arg2 = null;
		}
		lexeme.setResultPredicateArg(arg2);
		
		if (request.getParameter("ls_trel") != null)
		{
			if (request.getParameter("ls_trel").equals(""))
			{
				lexeme.setThematicRelation(null);
			}
			else
			{
				int i = Integer.parseInt(request.getParameter("ls_trel"));
				lexeme.setThematicRelation(ThematicRelation.list()[i]);
			}
		}
		
		lexeme.save();
		lsId = lexeme.id;
	}	
	
	%></em></p><%

	Lexeme root = Lexeme.getTreeRoot(sql);
	Lexeme.Finder finder = new Lexeme.Finder(root, lsId);
	finder.visit();
	Lexeme current = finder.getFoundLexeme();
	root.sortInPlace();	
	
	if (current == null) 
	{
		current = new Lexeme(sql);
		current.surface = request.getParameter("surface");
	}
%>

<script type="text/javascript"><!--

	var logics = [
		[ "BadID", "New Structure..." ],
	<%
		new LogicalStructureArray(out, lsId, root).visit();
	%>
	];

//--></script>
<script type="text/javascript" src="lsedit.js" />

<form name="nav" method="GET">
<table>
<tr bgcolor="#FFCCCC"><th colspan=4>Navigator</th></tr>
<tr bgcolor="#FFEEEE">
  <th>Filter</th>
  <th>Lexicon Entry</th>
  <th>Action</th>
</tr>
<tr bgcolor="#FFEEEE">
	<td><input name="filter" value="<%=
		request.getParameter("filter") != null
		? request.getParameter("filter") : ""
	%>" onKeyUp="doFilter(filter, lsid, logics)"></td>
	<td>
		<select name="lsid">
		<option value="">New Structure...
		<% new LogicalStructureList(out, lsId, -1, root).visit(); %>
		</select>
	</td>
	<td>
		<input type="submit" value="Go">
	</td>
</tr>
</table>
</form>

<script type="text/javascript"><!--

	var nav = document.forms.nav;
	doFilter(nav.filter, nav.lsid, logics);
	
//--></script>

<%
if (request.getParameter("lsid") == null)
{
	%>
	Please select a lexicon entry from the list above.
	<%
}
else if (lsId <= 0)
{
	%>
	The selected lexicon entry does not exist or is invalid.
	<%
}
else
{
	%>
<table border
	<tr>
		<td>ID</td>
		<td><%= lsId <= 0 ? "New" : (lsId + "") %></td>
	</tr>
	<tr>
		<td>Predicate Lexeme(s)</td>
		<td>
			<% 
				String surface = current.surface;
				if (surface != null)
				{
					surface = surface.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;");
					%><%= surface %><%
				}
			%>
		</td>
	</tr>
	<tr>
		<td>Syntactic Macroroles</td>
		<td>
			<form method="POST" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= lsId %>">
			<select name="sa">
				<option <%= 
					(current.numSyntacticArgs==-1)?"SELECTED":"" 
				%> value="-1">Unknown/Not Specified
				<option <%= 
					(current.numSyntacticArgs==0)?"SELECTED":"" 
				%> value="0">None
				<option <%= 
					(current.numSyntacticArgs==1)?"SELECTED":"" 
				%> value="1">MR1
				<option <%= 
					(current.numSyntacticArgs==2)?"SELECTED":"" 
				%> value="2">MR2
				<option <%= 
					(current.numSyntacticArgs==3)?"SELECTED":"" 
				%> value="3">MR3
			</select>
			<input type="submit" value="Save"/>
			</form>
		</td>
	</tr>
	<tr>
		<td>Inherited Logical Structure</td>
		<% 
			String activeLogic = null;
			{
		%>
		<td>
			<%
				Lexeme l = current.parent;
				
				while (l != null && (l.getLogicalStructure() == null || 
					l.getLogicalStructure().equals("")))
				{
					l = l.parent;
				}
					
				if (l != null)
				{
					activeLogic = l.getLogicalStructure();
				}
					
				if (l == null || l.getLogicalStructure() == null) 
				{
					%>none<%
				} 
				else 
				{
					out.print(
						l.getLogicalStructure()
							.replaceAll("<", "&lt;")
							.replaceAll(">", "&gt;") +
						" (from <a href=\"lsedit.jsp?lsid=" + l.id + 
						"\">" + l.id + "</a>)\n");
				}
			%>
		</td>
		<% 
			}
		%>
	</tr>
	<% Map allVars = new Hashtable(); %>
	<tr>
		<td>Inherited Variables</td>
		<td>
			<%
			{
				Map vars = null;
				boolean haveShownVars = false;

				if (current.parent != null) {
					try 
					{
						vars = current.parent.getAllVariables(sql);
	
						if (vars.size() > 0)
						{
							%>
			<table border>
			<tr>
				<th>Name</th>
				<th>Value</th>
				<th>From</th>
			</tr>
							<%
							haveShownVars = true;
						}
	
						for (Iterator vi = vars.keySet().iterator(); vi.hasNext(); ) 
						{
							String name  = (String)( vi.next() );
							Variable v   = (Variable)( vars.get(name) );
							
							%>
			<tr>
				<td><%= v.name %></td>
				<td><%= v.value %></td>
				<td><a href="lsedit.jsp?lsid=<%= v.lexemeId %>"><%= v.lexemeId %></a></td>
			</tr>
							<%
						}
					} catch (SQLException sqlEx) {
						%><%= sqlEx %><%
					}
				}
				
				if (vars != null) allVars = vars;
				
				if (haveShownVars)
				{
					%></table><%
				}
				else
				{
					%>none<%
				}
			}
			%>
		</td>
	</tr>
	<tr>
		<td>New Variables</td>
		<td>
			<%
			if (lsId <= 0) {
				%>Save this new structure before creating variables.<%
			} else {
				%>
			<table border>
			<tr>
				<th>Name</th>
				<th>Value</th>
			</tr>
				<%
		
				Map vars = null;
				
				try {
					vars = current.getVariables(sql);
					
					for (Iterator vi = vars.keySet().iterator(); 
						vi.hasNext(); ) 
					{
						String name = (String)( vi.next() );
						Variable v  = (Variable)( vars.get(name) );
						allVars.put(name, v);
						
						%>
			<tr>
				<form method="POST" action="lsedit.jsp">
				<input type="hidden" name="vid" value="<%= v.id %>">
				<input type="hidden" name="lsid" value="<%= lsId %>">
				<td><input name="vn" value="<%= v.name %>"></td>
				<td>
					<input name="vv" value="<%= v.value %>">
					<input type="submit" name="vcu" value="Update">
					<input type="submit" name="vd" value="Delete">
				</td>
				</form>
			</tr>
						<%
					}
				} catch (SQLException sqlEx) {
					%><%= sqlEx %><%
				}

				%>
			<tr>
				<form method="POST" action="lsedit.jsp">
				<input type="hidden" name="lsid" value="<%= lsId %>">
				<td><input name="vn"></td>
				<td>
					<input name="vv">
					<input type="submit" name="vcu" value="Create">
				</td>
				</form>
			</tr>
			</table>
				<%
			}
			%>
		</td>
	</tr>
	<tr>
		<td>New Logical Structure</td>
		<td>
			<form name="oslash">
				<input type="hidden" name="oslash" value="&Oslash;" />
			</form>
			
			<form name="lsform" method="POST" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= lsId %>" />
			<table>
			<tr>
				<td colspan="3">
					<h4>Causativity</h4>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<input type="checkbox" name="ls_caused" value="1"
						onClick="return updateLS()" 
						<%= current.isCaused() ? "CHECKED" : "" %>
						/>
				</td>
				<td>
					There is a controlling agent (&alpha; CAUSE &beta;)
				</td>
				<td>
					<strong>do</strong>'(&lt;x&gt;, &Oslash;) CAUSE [...]
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<h4>Punctuality</h4>
				</td>
			</tr>
			<tr>
				<td rowspan="3" valign="top">
					<input type="checkbox" name="ls_punct" value="1"
						onClick="return updateLS()" 
						<%= current.isPunctual() ? "CHECKED" : "" %>
						/>
				</td>
				<td>
					<p>This must be done in an instant (punctual)</p>
				</td>
			</tr>
			<tr>
				<td>
					<input type="radio" name="ls_punct_result" value="1"
						onClick="return updateLS()" 
						<%= current.hasResultState() ? "CHECKED" : "" %>
						/>
						It has a result state
				</td>
				<td>
					INGR
				</td>
			</tr>
			<tr>
				<td>
					<input type="radio" name="ls_punct_result" value="0"
						onClick="return updateLS()" 
						<%= current.hasResultState() ? "" : "CHECKED" %>
						/>
						It has <strong>no</strong> result state
				</td>
				<td>
					SEMEL
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<h4>Non-punctuality</h4>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<input type="checkbox" name="ls_telic" value="1"
						onClick="return updateLS()" 
						<%= current.isTelic() ? "CHECKED" : "" %>
						/>
				</td>
				<td>
					This must be done as a process reaching an endpoint (... in an hour)
				</td>
				<td>
					BECOME
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<h4>Dynamicity (Change, Activity)</h4>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<input type="radio" name="ls_dynamic" value="0"
						onClick="return updateLS()" 
						<%= current.isDynamic() ? "" : "CHECKED" %>
						/>
				</td>
				<td>
					This is a lasting condition (state)
				</td>
				<td></td>
			</tr>
			<tr>
				<td valign="top">
					<input type="radio" name="ls_dynamic" value="1"
						onClick="return updateLS()" 
						<%= current.isDynamic() ? "CHECKED" : "" %>
						/>
				</td>
				<td>
					This is something that can be done <strong>actively</strong>
					(<em>activity</em>)
				</td>
				<td>
					<strong>do</strong>'(&lt;x&gt;, [...])
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<h4>Predicate</h4>
				</td>
				<td valign="bottom">
					<input name="ls_pred" value="<%= 
						current.getPredicate() == null ? "" :
						current.getPredicate()
					%>" onKeyUp="return updateLS()" />
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<h4>Endpoint (Achievement)</h4>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<input type="radio" name="ls_endpoint" value="0"
						onClick="return updateLS()" 
						<%= current.hasEndpoint() ? "" : "CHECKED" %> />
				</td>
				<td>
					This <em>activity</em> has no endpoint
				</td>
				<td>
				</td>
			</tr>
			<tr>
				<td valign="top">
					<input type="radio" name="ls_endpoint" value="1"
						onClick="return updateLS()" 
						<%= current.hasEndpoint() ? "CHECKED" : "" %> /> 
				</td>
				<td>
					This <em>activity</em> has an endpoint
					(<em>Active Achievement</em>)
				</td>
				<td>
					&amp; INGR
				</td>
			</tr>
			<tr>
				<td></td>
				<td>Predicate:</td>
				<td>
					<input name="ls_pred_2" value="<%= 
						current.getResultPredicate() == null ? "" :
						current.getResultPredicate()
					%>" onKeyUp="return updateLS()" />
				</td>
			</tr>
			<tr>
				<td></td>
				<td>Argument:</td>
				<td>
					<select name="ls_arg_2" onChange="return updateLS()">
					<option value="x" <%=
						current.getResultPredicateArg() != null &&
						current.getResultPredicateArg().equals("x") 
						? "SELECTED " : ""
					%>>&lt;x&gt;</option>
					<option value="y" <%= 
						current.getResultPredicateArg() != null &&
						current.getResultPredicateArg().equals("y") 
						? "SELECTED " : ""
					%>>&lt;y&gt;</option>
					</select>
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<h4>Thematic Relation</h4>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<select name="ls_trel" onChange="return updateLS()">
					<option value="" <%=
						current.getThematicRelation() == null ? "SELECTED" : ""
					%>>None/Unknown</option>
					<% 
						ThematicRelation[] rels = ThematicRelation.list();
						for (int i = 0; i < rels.length; i++)
						{
							ThematicRelation rel = rels[i];
							%><option value="<%= i %>" <%=
								current.getThematicRelation() == rels[i]
								? "SELECTED" : ""
							%>><%= 
								rel.getPrompt()
								.replaceAll("<", "&lt;")
								.replaceAll(">", "&gt;")
							%></option><%
						}
					%>
					</select>
				</td>
				<td>
					<input name="ls_trel_text" readonly="1" value="" />
				</td>
			</tr>
			<tr>
				<td colspan="3">
					<h4>Logical Structure</h4>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<input name="ls" width="80" size="60" value="<%= 
						current.getLogicalStructure() == null ? "" :
						current.getLogicalStructure() 
					%>" readonly="1" /> 
				</td>
				<td>
					<input type="submit" name="ls_save" value="Save" />
				</td>
			</tr>
			</table>
			</form>
			
			<script type="text/javascript"><!--
			
			var them_rels = [
			<%
				for (int i = 0; i < rels.length; i++)
				{
					ThematicRelation rel = rels[i];
					%>
						["<%= rel.getLabel()   %>",
					     "<%= rel.getPrompt()  %>",
					     "<%= rel.getArgText() %>"],
					<%
				}
			%>								   
			];
											   			
			updateLS();
			
			//--></script>
		</td>
	</tr>
	<tr>
		<td>Evaluated Logical Structure</td>
		<td>
			<%
				activeLogic = current.getEvaluatedLogicalStructure(allVars);
					
				if (activeLogic == null) activeLogic = "none";
				
				activeLogic = activeLogic.replaceAll("<", "&lt;")
					.replaceAll(">", "&gt;");
			%>
			<%= activeLogic %>
		</td>
	</tr>
	<tr>
		<td>Domain Parent</td>
		<td>
			<script type="text/javascript"><!--
				var parents = [
					[ "0", "None" ],
					<% 
						new LogicalStructureArray(out, current.parentId, root).visit(); 
					%>
				];
			//--></script>
			
			<form name="dpform" method="POST" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= lsId %>" />
			<input name="filter" onKeyUp="doFilter(filter, dpid, parents)">
			<select name="dpid">
			<option <%= current.parentId == 0 ? "SELECTED" : "" %> value="0">
				None
			<%
				new LogicalStructureList(out, current.parentId, -1, root)
					.visit();
			%>
			</select>
			<input type="submit" value="Save" />
			</form>
		</td>
	</tr>
	<tr>
		<td>Domain Label</td>
		<td>
			<form name="dlform" method="POST" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= lsId %>">
			<input name="dl" value="<%= 
				current.label == null ? "" : current.label 
			%>">
			<input type="submit" value="Save" />
			</form>
		</td>
	</tr>
	<tr>
		<td>Domain Description</td>
		<td>
			<form name="ddform" method="POST" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= lsId %>">
			<input name="dd" width="80" size="80" value="<%= 
				current.desc == null ? "" : current.desc 
			%>">
			<input type="submit" value="Save">
			</form>
		</td>
	</tr>
	<!--
	<tr>
		<td>Save changes</td>
		<td>
			<form name="cpform" method="POST" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%= lsId %>" />
			<input type="hidden" name="surface" value="<%= surface %>" />
			<input type="submit" name="savecopy" value="Save Copy" />
			</form>
		</td>
	</tr>
	-->
</table>

<h2><font color="red">Delete</font></h2>

<p>
	Delete this lexicon entry, and change referring clauses to:
</p>

<form name="delform" method="POST" action="lsedit.jsp">
<input type="hidden" name="lsid" value="<%= lsId %>">
<select name="newlsid">
<option value="0">Not specified
<% new LogicalStructureList(out, 0, lsId, root).visit(); %>
</select>
<input type="submit" name="delrepl" value="Delete">
</form>

	<%
} // end if (lsId <= 0)
%>

</body></html>	
<% 
	System.out.println("lsedit.jsp rendered in "+
		(System.currentTimeMillis()-startTime)+" ms"); 
%>
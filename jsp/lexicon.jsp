<%@ include file="include/setup.jsp" %>
<%@ include file="include/auth.jsp" %>

<% String pageTitle = "Edit Logical Structure"; %>
<%@ include file="include/header.jsp" %>

<%@ page import="java.util.Arrays" %>
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
<%@ page import="org.aptivate.web.controls.*" %>
<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.db.sql.*" %>
<%@ page import="com.qwirx.lex.controller.LexiconController" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<%@ page import="com.qwirx.lex.lexicon.*" %>
<%@ page import="com.qwirx.lex.lexicon.Lexeme.Variable" %>

<style type="text/css">
	h4 { margin-top: 8pt; margin-bottom: 0pt; }
	td { vertical-align: top; }
</style>

<%	
	LexiconController controller = new LexiconController(request, emdros, sql);	
	
	List<String> errors = controller.getErrorMessages();
	if (errors.size() > 0)
	{
		%>
		<div id="error_message">
			<ul>
				<% for (String error : errors) { %>
					<li><%= error %></li>
				<% } %>
			</ul>
		</div>
		<%
	}

	Lexeme root = Lexeme.getTreeRoot(sql);
	Lexeme.Finder finder = new Lexeme.Finder(root,
		controller.getLexeme().getID());
	finder.visit();
	Lexeme current = finder.getFoundLexeme();
	root.sortInPlace();	
%>

<script type="text/javascript"><!--

	var logics = [
		[ "BadID", "New Structure..." ],
	<%
		controller.new LogicalStructureArray(out, root).visit();
	%>
	];

//--></script>
<script type="text/javascript" src="js/lsedit.js"></script>

<form name="nav" method="get" action="lexicon.jsp">
<table>
<tr class="nav1"><th colspan="4">Navigator</th></tr>
<tr class="nav2">
	<th>Filter</th>
	<th>Lexicon Entry</th>
	<th>Action</th>
</tr>
<tr class="nav1">
	<td><input name="filter" value="<%=
		request.getParameter("filter") != null
		? request.getParameter("filter") : ""
	%>" onKeyUp="doFilter(filter, lsid, logics,
		<%= controller.getLexeme().getID() %>)" /></td>
	<td>
		<%
		LexiconController.LogicalStructureList navigateStructureLister =
			controller.new LogicalStructureList(-1, root);
		navigateStructureLister.visit();
		List<String[]> navigateStructures = navigateStructureLister.getValues();
		navigateStructures.add(0, new String[]{"", "New Structure..."});
		%>
		<%= new SelectBox("lsid", navigateStructures, request).toString() %>
	</td>
	<td>
		<input type="submit" value="Go" />
	</td>
</tr>
</table>
</form>

<script type="text/javascript"><!--

	var nav = document.forms.nav;
	doFilter(nav.filter, nav.lsid, logics, <%= controller.getLexeme().getID() %>);
	
//--></script>

<%
if (controller.getLexeme().getID() == -1)
{
	%>
	<p>Please select a lexicon entry from the list above.</p>
	<%
}
else if (controller.getLexeme().getID() == 0)
{
	%>
	<p>You cannot edit the root lexeme. Please select another lexicon entry
		from the list above.</p>
	<%
}
else if (current == null)
{
	%>
	<p>The selected lexicon entry does not exist or is invalid.</p>
	<%
		
	current = new Lexeme(sql);
	current.surface = request.getParameter("surface");
}
else
{
	%>
<table border
	<tr>
		<td>ID</td>
		<td><%= controller.getLexeme().getID() <= 0 ? "New" :
			(controller.getLexeme().getID() + "") %></td>
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
			<form method="POST" action="lexicon.jsp">
			<input type="hidden" name="lsid" value="<%= 
				controller.getLexeme().getID() %>">
			<%
			List<String[]> options = Arrays.asList(new String[][]{
				new String[]{"-1", "Unknown/Not Specified"},
				new String[]{"0",  "None"},
				new String[]{"1",  "MR1"},
				new String[]{"2",  "MR2"},
				new String[]{"3",  "MR3"}
			});
			SelectBox sb = new SelectBox("sa", options, request);
			sb.setDefaultValue("" + current.getNumSyntacticArgs());
			%>
			<%= sb.toString() %>
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
						" (from <a href=\"lexicon.jsp?lsid=" + l.getID() + 
						"\">" + l.getID() + "</a>)\n");
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
				<td><a href="lexicon.jsp?lsid=<%= v.lexemeId %>"><%= v.lexemeId %></a></td>
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
			if (controller.getLexeme().getID() <= 0) {
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
				<form method="POST" action="lexicon.jsp">
				<input type="hidden" name="vid" value="<%= v.id %>">
				<input type="hidden" name="lsid" value="<%=
					controller.getLexeme().getID() %>">
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
				<form method="POST" action="lexicon.jsp">
				<input type="hidden" name="lsid" value="<%=
					controller.getLexeme().getID() %>">
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
			
			<form name="lsform" method="POST" action="lexicon.jsp">
			<input type="hidden" name="lsid" value="<%=
				controller.getLexeme().getID() %>" />
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
				<td valign="top">
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
				<td>This must be done in an instant (punctual)
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
					<option value="x,y" <%= 
						current.getResultPredicateArg() != null &&
						current.getResultPredicateArg().equals("x,y") 
						? "SELECTED " : ""
					%>>&lt;x&gt;, &lt;y&gt;</option>
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
					controller.new LogicalStructureArray(out, root).visit(); 
					%>
				];
			//--></script>
			
			<form name="dpform" method="POST" action="lexicon.jsp">
			<input type="hidden" name="lsid" value="<%=
				controller.getLexeme().getID() %>" />
			<input name="filter" onKeyUp="doFilter(filter, dpid, parents,
				<%= controller.getLexeme().getID() %>)" />
			<%
			LexiconController.LogicalStructureList parentStructureLister =
				controller.new LogicalStructureList(
					controller.getLexeme().getID(), root);
			parentStructureLister.visit();
			List<String[]> parentStructures = parentStructureLister.getValues();
			parentStructures.add(0, new String[]{"0", "None"});
			%>
			<%= new SelectBox("dpid", parentStructures, request).toString() %>
			<input type="submit" value="Save" />
			</form>
		</td>
	</tr>
	<tr>
		<td>Domain Label</td>
		<td>
			<form name="dlform" method="POST" action="lexicon.jsp">
			<input type="hidden" name="lsid" value="<%=
				controller.getLexeme().getID() %>">
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
			<form name="ddform" method="POST" action="lexicon.jsp">
			<input type="hidden" name="lsid" value="<%=
				controller.getLexeme().getID() %>">
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
			<form name="cpform" method="POST" action="lexicon.jsp">
			<input type="hidden" name="lsid" value="<%=
				controller.getLexeme().getID() %>" />
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

<form name="delform" method="POST" action="lexicon.jsp">
<input type="hidden" name="lsid" value="<%= controller.getLexeme().getID() %>">
<%
LexiconController.LogicalStructureList replacementStructureLister =
	controller.new LogicalStructureList(0, root);
replacementStructureLister.visit();
List<String[]> replacementStructures = replacementStructureLister.getValues();
replacementStructures.add(0, new String[]{"0", "Not specified"});
%>
<%= new SelectBox("newlsid", replacementStructures, request).toString() %>
<input type="submit" name="delrepl" value="Delete">
</form>

	<%
} // end if (controller.getLexeme().getID() <= 0)
%>

<form name="addform" method="POST" action="lexicon.jsp">
	<h2 style="color: red">Create</h2>
	<p>
		Create a new lexicon entry:
		<input type="submit" name="createnew" value="Create" />
	</p>
</form>

<%@ include file="include/footer.jsp" %>
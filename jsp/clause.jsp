<% String pageTitle = "Text Browser"; %>
<%@ include file="header2.jsp" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.controller.*" %>
<%@ page import="com.qwirx.lex.hebrew.*" %>
<%@ page import="com.qwirx.lex.lexicon.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>
<%@ page import="com.qwirx.lex.parser.*" %>
<%@ page import="com.qwirx.crosswire.kjv.KJV" %>
<%@ page import="org.aptivate.web.utils.EditField" %>
<%@ page import="org.crosswire.jsword.book.*" %>
<%@ page import="net.didion.jwnl.data.POS" %>
<%@ page import="net.didion.jwnl.data.Synset" %>

<script type="text/javascript"><!--

	function enableEditButton()
	{
		if (document.forms.changels == null)
		{
			return;
		}
		
		var lsselect = document.forms.changels.lsid;
		var editform = document.forms.editls;
		if (editform == null || editform.submit == null)
		{
			return;
		}
			
		var sellsid  = -1;
		if (lsselect.selectedIndex > 0)
		{
			sellsid = lsselect.options[lsselect.selectedIndex].value;
		}	
		
		editform.submit.disabled = (sellsid != editform.lsid.value);
		return true;
	}

	function enableChangeButton(button, oldValue, selectBox)
	{
		if (button == null) return;
		var newValue = selectBox.options[selectBox.selectedIndex].value;
		button.disabled = (newValue == oldValue);
		return true;
	}
	
//--></script>

<style type="text/css">
	TABLE.tree TD
	{
		text-align: center;
	}
</style>

<%@ include file="auth.jsp" %>

<%@ include file="navclause.jsp" %>

<%
	ClauseController controller = new ClauseController(request, emdros, sql,
		navigator);	
	MatchedObject clause = controller.getClause();
		 
	if (clause == null) 
	{
		%><p>Selected clause not found or access denied.</p><%
	} 
	else 
	{
		%>
		<span class="hebrew"><%= controller.getHebrewText() %></span>
		<table><%= controller.getGlossTable() %></table>
		<p><%= controller.getKingJamesVerse() %></p>

		<p>Predicate text is: 
		<%
		String predicate = controller.getPredicateText();
		if (predicate != null)
		{
			%><%= controller.getPredicateText()
				.replaceAll("<", "&lt;")
				.replaceAll(">", "&gt;") %><%
		}
		else
		{
			%>Not found<%
		}
		%>
		</p>
		<p>
		<table border>
			<%= controller.getWordTable() %>
		</table>
		</p>
		<%
		
		{
			Parser p = new Parser(sql);
			p.setVerbose(true);
			List sentences = p.parseFor(controller.getMorphEdges(), "SENTENCE");
			
			if (sentences.size() == 0)
			{
				%>
				<p>
				Parse failed. Click <a href="parse.jsp">here</a> to fix it.
				</p> 
				<%
			}
			else
			{
				%>
				<p>
				Parse finished with <%= sentences.size() %> possible trees.
				Showing the first 
				<%= sentences.size() > 3 ? 3 : sentences.size() %>:
				</p> 
				<%
				for (int i = 0; i < sentences.size() && i < 3; i++)
				{
					Edge sentence = (Edge)( sentences.get(i) );
					
					%><%= sentence.toTree().toHtml(new
						ClauseController.BorderTableRenderer()) %><%
				}
				
				%><p>
				Click 
				<a href="parse.jsp">here</a>
				to see and edit all <%= sentences.size() %> parses.
				</p><%
			}
		}
		
		%>

		<p>
		<%
		String selLsIdString = request.getParameter("lsid");
		
		if (!emdros.canWriteTo(clause))
		{
			String structure = controller.getUnlinkedLogicalStructure();
			%>
			Selected lexicon entry logical structure:
			<%=
				structure.equals("") ? "none" : structure
						.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;")
			%>
			<%
		}
		else if (selLsIdString != null && selLsIdString.equals("add"))
		{
			// Change button clicked and "Add..." selected, 
			// show LS entry box
			%>
		<form name="changels" method="POST">
		Enter new logical structure:
		<input name="newls"/>
		<input type="submit" name="create" value="Create"/>
		<input type="submit" name="cancel" value="Cancel"/>
		</form>
			<%
		}
		else
		{
			// Show LS selector box, Change button, and maybe Edit button

			%>
		<table><tr><td>
			<form name="changels" method="POST">
			Choose logical structure:
			<select name="lsid" onChange="enableEditButton(); 
			return enableChangeButton(lssave,<%=
				controller.getSelectedLogicalStructureId()
			%>,lsid)">
			<option value="0" <%=
				(controller.getSelectedLogicalStructureId() == 0)
					? " SELECTED" : "" %>>
				Not specified
			</option>
			<%

			try 
			{
				PreparedStatement stmt = sql.prepareSelect
					("SELECT ID,Structure,Syntactic_Args " +
					 "FROM lexicon_entries WHERE Lexeme = ?");
				stmt.setString(1, controller.getPredicateText());
				
				ResultSet rs = sql.select();
				while (rs.next()) 
				{
					int    thisLsId      = rs.getInt("ID");
					String thisStructure = rs.getString("Structure");
					int    thisNumSMRs   = rs.getInt("Syntactic_Args");
					
					%>
			<option value="<%=
				thisLsId
			%>"<%=
				thisLsId == controller.getSelectedLogicalStructureId()
					? " SELECTED" : ""
			%>><%=
				thisStructure == null
				? "(undefined structure "+thisLsId+")"
				: (thisStructure
						.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;"))
			%>
					<%
				}
			} 
			catch (DatabaseException ex) 
			{
				%><%= ex %><%
			} 
			finally 
			{
				sql.finish();
			}
		
			%>
			<option value="add" <%=
				selLsIdString != null && selLsIdString.equals("add")
				? " SELECTED" : ""
			%>>Add new...
			</select>
			<input type="submit" name="lssave" value="Change">
			</form>
			<script type="text/javascript"><!--
			enableChangeButton(document.forms.changels.lssave,<%=
				controller.getSelectedLogicalStructureId()
			%>, document.forms.changels.lsid);
			//--></script>
		</td>
			<%
			
			if (controller.getSelectedLogicalStructureId() == 0)
			{
				%>
		<td>
			<form name="editls" method="get" action="lsedit.jsp">
			<input type="hidden" name="lsid" value="<%=
				controller.getSelectedLogicalStructureId()
			%>" />
			<input type="submit" name="submit" value="Edit..." />
			</form>
		</td>
				<%
			}

			%>
		</table>
			<%
		}
		%>
		</p>
				
		<script type="text/javascript"><!--
			enableEditButton();
		//--></script>
		
		<% EditField form = new EditField(request); %>
		
		<p>Linked logical structure: <%=
			form.escapeEntities(controller.getLinkedLogicalStructure())
		%></p>

		<h3>Notes</h3>
		
		<%
		
		if (request.getParameter("nd") != null &&
			request.getParameter("ni") != null)
		{
			%><table border><%
			
			boolean foundNotes = false;
	
			String editNoteIdString = request.getParameter("ni");
			int editNoteId = -1;
			if (editNoteIdString != null)
			{
				editNoteId = Integer.parseInt(editNoteIdString);
			}
		
			Sheaf clauseSheaf = emdros.getSheaf
				("SELECT ALL OBJECTS IN { "+min_m+" - "+max_m+"} "+
				 "WHERE [clause self = "+navigator.getClauseId()+
				 " [note GET text]"+
				 "]");
	
			SheafConstIterator sci = clauseSheaf.const_iterator();
			while (sci.hasNext()) 
			{
				Straw straw = sci.next();
				clause = straw.const_iterator().next();
				
				Sheaf noteSheaf = clause.getSheaf();
				SheafConstIterator nsci = noteSheaf.const_iterator();
				while (nsci.hasNext()) 
				{
					Straw ns = nsci.next();
					MatchedObject note = ns.const_iterator().next();
				
					String noteText = note.getEMdFValue("text").getString();
					if (!foundNotes)
					{
						foundNotes = true;
					}
					
					%><tr><%
	
					if (request.getParameter("ne") != null &&
						editNoteId == note.getID_D())
					{
						%>
	<form method="POST" action="clause.jsp">
	<input type="hidden" name="ni" value="<%= note.getID_D() %>" />
	<td>
		<input name="nt" size="80" value="<%= form.escapeEntities(noteText) %>" />
	</td>
	<td>
		<input type="submit" name="nu" value="Update" />
		<input type="submit" name="nd" value="Delete" />
	</td>
	</form>
						<%
					}
					else
					{
						%>					
	<td><%= form.escapeEntities(noteText) %></td>
	<td>
		<a href="clause.jsp?ni=<%= note.getID_D() %>&ne=1">Edit</a>
	</td>
						<%
					}
	
					%></tr><%
				}
			}
			
			if (!foundNotes)
			{
				%><tr><td>No notes for this clause</td></tr><%
			}
		}
			
		%>
</table>

<p><form method="POST">
<table>
<tr>
<td>Add note:</td>
<td><input name="nt" size="80"></td>
<td><input type="submit" name="nc" value="Create"></td>
</tr>
</table>
</form></p>
	
<p>Download clause in GEN format for LTC:</p>
<ul>
	<li><a href="gen-export.jsp?clause=<%= navigator.getClauseId() %>&hebrew=y">With
		Hebrew (right-to-left)</a></li>
	<li><a href="gen-export.jsp?clause=<%= navigator.getClauseId() %>&hebrew=n">Without
		Hebrew (left-to-right)</a></li>
</ul>

<%
		if (emdros.canWriteTo(clause))
		{
			%>
			<form method="POST">
				<% if (controller.isPublished(clause)) { %>
				<input type="submit" name="unpublish" value="Unpublish" />
				<% } else { %>
				<input type="submit" name="publish" value="Publish" />
				<% } %>
			</form>
			<%
		}
	}
%>
	
<hr>
        <%= navigator.getLabel("book") %>,
Chapter <%= navigator.getLabel("chapter") %>,
Verse   <%= navigator.getLabel("verse") %>,
Clause  <%= navigator.getClauseId() %>
</form>

<%@ include file="footer.jsp" %>
<% String pageTitle = "Parser Testing"; %>
<%@ include file="header2.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.hebrew.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>
<%@ page import="com.qwirx.lex.ontology.*" %>
<%@ page import="com.qwirx.lex.parser.*" %>
<%@ page import="com.qwirx.lex.wordnet.*" %>
<%@ page import="com.qwirx.lex.controller.ParseController.HebrewFeatureConverter" %>

<%@ include file="auth.jsp" %>
<%@ include file="navclause.jsp" %>

<script language="javascript" src="js/parsetree.js"></script>
	
<%
	ParseController controller = new ParseController(request, emdros, sql,
		navigator);
	MatchedObject clause = controller.getClause();
	
	if (clause == null)
	{
		%><p>Selected clause not found or access denied.</p><%
	}
	else
	{
		List morphEdges = new ArrayList();
		
		/* Prescan to list morpheme edges */
		
		{
			TreeNode root = new TreeNode("root");
			HebrewMorphemeGenerator gen = new HebrewMorphemeGenerator();
			
			{
				SheafConstIterator phrases = clause.getSheaf().const_iterator();
	
				while (phrases.hasNext()) 
				{
					MatchedObject phrase =
						phrases.next().const_iterator().next();
	
					SheafConstIterator words = phrase.getSheaf().const_iterator();
					while (words.hasNext()) 
					{
						MatchedObject word = words.next().const_iterator().next();
	
						HebrewFeatureConverter hfc = 
							new HebrewFeatureConverter(root, word, morphEdges,
								transliterator);
						
						gen.parse(word, hfc, true, sql);
					}
				}
			}
		
			%>
			<p>
				Hebrew text:
				<span class="hebrew"><%= controller.getHebrewText() %></span>
			</p>
			<%
		}
		
		if (request.getParameter("rule_add") != null)
		{
			String symbol = request.getParameter("new_rule_sym");
			String parts  = request.getParameter("new_rule_parts");
			
			if (symbol == null || symbol.equals("") ||
				parts  == null || parts.equals(""))
			{
				%>
				<h2>Error: some required parameters were missing or empty</h2>
				<%
			}
			else
			{
				Change ch = sql.createChange(SqlChange.INSERT, 
					"lexicon_entries", null);
				ch.setString("Symbol", symbol);
				ch.setString("Lexeme", parts);
				ch.execute();
			}
		}

		{
			Parser p = new Parser(sql);
			p.setVerbose(true);
			Chart chart = p.parse(morphEdges);
			List sentences = chart.filter("SENTENCE", morphEdges, false);

			if (sentences.size() == 0)
			{
				%>
				<h3>Parse failed</h3>
				<%
			}
			else
			{
				%>
				<h3>Parse succeeded</h3>
				<%
			}
			
			%>
			<p>Complete sentences found: <%= sentences.size() %></p>
			<p>The edges found were:</p> 
			<form name="rulecmd" method="post">
			<%
			
				Map  edgeIds   = new Hashtable();
				Map  idEdges   = new Hashtable();
				Map  fakeChild = new Hashtable();
				List fakeEdges = new ArrayList();
				List depths    = new ArrayList();
				List edges     = chart.getEdges();
				int  maxDepth  = 0;
				
				for (ListIterator i = edges.listIterator(); i.hasNext(); )
				{
					Edge e = (Edge)( i.next() );
					
					int depth = e.getDepth();
					
					if (maxDepth < depth)
					{
						maxDepth = depth;
					}
					
					if (e instanceof MorphEdge)
					{
						MorphEdge me = (MorphEdge)e;
						WordEdge  we = new WordEdge(me.getHtmlSurface(), 
							me.getLeftPosition());
						i.add(we);
						fakeChild.put(me, we);
						fakeEdges.add(we);
					}
				}

				for (ListIterator i = edges.listIterator(); i.hasNext(); )
				{
					Edge e = (Edge)( i.next() );
					
					int uniqueNum = 1;
					String base = e.symbol().replaceAll("[^A-Za-z0-9]","_");
					String id = base + "_" + uniqueNum;
					
					while (idEdges.get(id) != null)
					{
						uniqueNum++;
						id = base + "_" + uniqueNum;
					}
					
					edgeIds.put(e, id);
					idEdges.put(id, e);
				}

				/*
				%>
				<ul>
				<%
				
				for (Iterator i = edges.iterator(); i.hasNext(); )
				{
					Edge e = (Edge)( i.next() );
					%>
					<ul>
						[<%= e.getDepth() %>] <%= edgeIds.get(e) %>
						<%= e.toString() %>
					</ul>
					<%
				}
				
				%>
				</ul>
				<%
				*/
				
				for (int i = 0; i <= maxDepth; i++)
				{
					depths.add(new ArrayList());
				}
				
				for (Iterator i = edges.iterator(); i.hasNext(); )
				{
					Edge e = (Edge)( i.next() );
					int  depth = e.getDepth();
					List edgesAtDepth = (List)( depths.get(depth) );
					edgesAtDepth.add(e);
				}
				
				%>
				<table border>
				<%
				
				class EdgeComparator implements Comparator
				{
					public int compare(Object o1, Object o2) 
					{
						Edge e1 = (Edge)o1;
						Edge e2 = (Edge)o2;
						int diff = e1.getLeftPosition() - e2.getLeftPosition();
						if (diff != 0) return diff;
						return e2.getRightPosition() - e1.getRightPosition();
					}
				}
				
				Comparator comp = new EdgeComparator();
				
				for (int i = maxDepth; i >= 0; i--)
				{
					List edgesAtDepth = (List)( depths.get(i) );
					List remaining = new ArrayList();
					remaining.addAll(edgesAtDepth);
					Collections.sort(remaining, comp);
					
					if (remaining.size() != edgesAtDepth.size())
					{
						throw new AssertionError("lost elements");
					}
					
			
							
					while (remaining.size() > 0)
					{
						List added    = new ArrayList();
						List leftOver = new ArrayList();
						
						%>
						<tr>
						<%
						
						int pos = 0;
						
						for (Iterator n = remaining.iterator(); n.hasNext(); )
						{
							Edge next = (Edge)( n.next() );
							boolean overlaps = false;
						
							for (Iterator a = added.iterator(); a.hasNext(); )
							{
								Edge addedEdge = (Edge)( a.next() );
								if (next.overlaps(addedEdge))
								{
									overlaps = true;
									break;
								}
							}
							
							if (overlaps)
							{
								leftOver.add(next);
								continue;
							}
						
							int left = next.getLeftPosition();
							if (pos < left)
							{
								%>
								<td colspan="<%= left - pos %>" />
								<%
								pos = left;
							}
							
							int right = next.getRightPosition();
							int colspan = right - left + 1;

							%>
							<td <%= colspan>1 ? "colspan="+colspan : "" %>
								id="<%= edgeIds.get(next) %>"
								onMouseOver="return highlight  (this);"
								onMouseOut=" return unhighlight_all();"
								>
								
							<% /* %>
							[<%= edgeIds.get(next) %>] 
							<% */
								
							if (!fakeEdges.contains(next))
							{
								%>
								<span class="cb">
									<input name="<%= edgeIds.get(next) %>"
										type="checkbox" 
										onClick="return on_checkbox_click(this);" />
								</span>
								<%
							}
							
							if (next instanceof RuleEdge)
							{
								RuleEdge re = (RuleEdge)next;
								%><a href="rules.jsp?erid=<%= 
									re.rule().id()
								%>#rule_<%=
									re.rule().id()
								%>"><%=
									next.getHtmlLabel() 
								%></a><%
							}
							else
							{
								%><%= next.getHtmlLabel() %><%
							}
							
							%>	
							</td>
							<%
							
							added.add(next);
							pos += colspan;
						}
						
						remaining = leftOver;
					}
					
					%>
					</tr>
					<%
				}
				
				Map parentEdges = new Hashtable();
				
				%>
				</table>
				<script><!--
				var children_by_id = Array();
				<%
				
				for (Iterator i = edges.iterator(); i.hasNext(); )
				{
					Edge e = (Edge)( i.next() );
					%>children_by_id["<%= edgeIds.get(e) %>"] = new Array(<%
					Edge [] children = e.parts();
					List childList = new ArrayList();
					for (int j = 0; j < children.length; j++)
					{
						childList.add(children[j].getUnboundOriginal());
					}
					if (fakeChild.get(e) != null)
					{
						childList.add(fakeChild.get(e));
					}
					for (Iterator j = childList.iterator(); j.hasNext(); )
					{
						Edge child = (Edge)( j.next() );
						String id = (String)( edgeIds.get(child) );
						%>"<%= id %>"<%
						if (j.hasNext())
						{
							%>, <%
						}
							
						List parentEdgeList = (List)( parentEdges.get(id) );
						if (parentEdgeList == null)
						{
							parentEdgeList = new ArrayList();
						}
						parentEdgeList.add(e);
						parentEdges.put(id, parentEdgeList);
					}
					%>);
					<%
				}
							
				%>				
				var parents_by_id = Array();
				<%
				for (Iterator i = parentEdges.keySet().iterator(); i.hasNext(); )
				{
					String childId = (String)( i.next() );
					List parentList = (List)( parentEdges.get(childId) );
					
					%>parents_by_id["<%= childId %>"] = new Array(<%
					for (Iterator j = parentList.iterator(); j.hasNext(); )
					{
						Edge parent = (Edge)( j.next() );
						String id = (String)( edgeIds.get(parent) );
						%>"<%= id %>"<%
						if (j.hasNext())
						{
							%>, <%
						}
					}
					%>);
					<%
				}		
				%>				
							
				var edges = Array(
				<%
				for (Iterator i = edges.iterator(); i.hasNext(); )
				{
					Edge e = (Edge)( i.next() );
					String id = (String)( edgeIds.get(e) );
					%>
					new edge("<%= id %>", <%= 
						e.getLeftPosition() %>, <%=
						e.getRightPosition() %>, "<%=
						e.symbol() %>", <%=
						e.isTerminal() %>, <%=
						fakeEdges.contains(e) %>)<%
					if (i.hasNext())
					{
						%>,
						<%
					}
				}
				%>
				);
				
				//--></script>
										
				<p></p>
				
				<table>
					<tr bgcolor="#ffcccc">
						<th colspan="3">Create a new rule</th>
					</tr>
					<tr bgcolor="#ffeeee">
						<td>Top node name (symbol)</td>
						<td>Component node names (parts)</td>
						<td>Command</td>
					</tr>
					<tr bgcolor="#ffcccc">
						<td>
							<input name="new_rule_sym" />
						</td>
						<td>
							<input name="new_rule_parts" size="30"/>
						</td>
						<td>
							<input name="rule_add" type="submit" value="Create" 
								onClick="return on_rule_add_click()" />
						</td>
					</tr>
				</table>
				
				</form>

			<%
		}
	}
	
%>

<%@ include file="footer.jsp" %>
<% String pageTitle = "Parser Testing"; %>
<%@ include file="header2.jsp" %>

<%@ page import="java.util.*" %>
<%@ page import="com.qwirx.lex.ontology.*" %>
<%@ page import="com.qwirx.lex.wordnet.*" %>
<%@ page import="com.qwirx.lex.parser.*" %>
<%@ page import="com.qwirx.lex.morph.*" %>

<script language="javascript">
<!--
	
	var old_hilites = null;
	var selected_state = new Array();
	
	function selected_class(state)
	{
		return state ? "selected" : "";
	}

	function object_default_class(object)
	{
		var state = selected_state[object.id];
	
		if (state == null)
		{
			state = false;
		}
	
		return selected_class(state);
	}
		
	function highlight_recursive(object)
	{
		old_hilites[old_hilites.length] = object;
		object.className = "hilite";
	
		var children = children_by_id[object.id];
	
		if (children == null)
		{
			alert("object has no id: " + object);
			return;
		}
	
		for (var i = 0; i < children.length; i++)
		{
			var child = document.getElementById(children[i]);
			if (child == null)
			{
				alert("no such child: " + children[i]);
			}
			else
			{
				highlight_recursive(child);
			}
		}
	}
	
	function highlight(object)
	{
		unhighlight_all();
	
		highlight_recursive(object);

		return true;
	}

	function unhighlight_all()
	{
		if (old_hilites != null)
		{
			for (var i = 0; i < old_hilites.length; i++)
			{
				old_hilites[i].className = object_default_class(old_hilites[i]);
			}
		}
	
		old_hilites = new Array();
	
		return true;
	}
	
	function select_children_recursive(object, state)
	{
		selected_state[object.id] = state;
		object.className = selected_class(state);
	
		var children = children_by_id[object.id];
	
		if (children == null)
		{
			alert("object has no id: " + object);
			return;
		}
	
		for (var i = 0; i < children.length; i++)
		{
			var child = document.getElementById(children[i]);
			if (child == null)
			{
				alert("no such child: " + children[i]);
			}
			else
			{
				select_children_recursive(child, state);
			}
		}
	}
	
	function on_checkbox_click(target_checkbox)
	{
		var table_cell = document.getElementById(target_checkbox.name);
		select_children_recursive(table_cell, target_checkbox.checked);
	
		var sel_left  = -1;
		var sel_right = -1;
		var any_checked = false;
		var checked_list = new Array();
	
		for (var i = 0; i < edges.length; i++)
		{
			var edge = edges[i];
			var checkbox = document.forms.rulecmd[edge.id];
	
			if (checkbox != null && checkbox.checked)
			{
				checked_list[checked_list.length] = edge;
	
				if (sel_left == -1 || sel_left > edge.left)
				{
					sel_left = edge.left;
				}
				if (sel_right == -1 || sel_right < edge.right)
				{
					sel_right = edge.right;
				}
			}
		}
	
		for (var i = 0; i < edges.length; i++)
		{
			var edge = edges[i];
			var enabled = false;
			var checkbox = document.forms.rulecmd[edge.id];
	
			if (checkbox == null)
			{
				// fake edges don't have checkboxes
				continue;
			}
			
			if (checked_list.length == 0)
			{
				// if none are checked yet, allow any to be checked
				enabled = true;
			}
			else if (edge.left == sel_right + 1 || edge.right == sel_left - 1)
			{
				// allow expansion at the edges
				enabled = true;
			}
			else if (!checkbox.checked)
			{
				enabled = false;
			}
			else if (edge.left == sel_left || edge.right == sel_right)
			{
				// allow deselection at the left and right edges only
				enabled = true;
			}
			
			checkbox.disabled = !enabled;
		}
	
		var parts = document.forms.rulecmd.new_rule_parts;
		parts.value = "";
	
		for (var i = 0; i < checked_list.length; i++)
		{
			var edge = checked_list[i];
	
			if (edge.terminal)
			{
				parts.value += "\"" + edge.symbol + "\"";
			}
			else
			{
				parts.value += "{" + edge.symbol + "}";
			}
	
			if (i < checked_list.length - 1)
			{
				parts.value += " ";
			}
		}
	
		return true;
	}
	
	function edge (id, left, right, symbol, terminal, fake)
	{
		this.id = id;
		this.left = left;
		this.right = right;
		this.symbol = symbol;
		this.terminal = terminal;
		this.fake = fake;
	}
	
	function on_rule_add_click()
	{
		var form = document.forms.rulecmd;
		
		if (form.new_rule_sym.value == "")
		{
			alert("You must enter a top symbol for the new rule!");
			return false;
		}
	
		if (form.new_rule_parts.value == "")
		{
			alert("You must select some edges for the new rule!");
			return false;
		}
	
		return true;
	}
	
//-->
</script>
	
<%@ include file="auth.jsp" %>

<%@ include file="navclause.jsp" %>

<%

	Map phrase_types = emdros.getEnumerationConstants
		("phrase_type_e",false);
	
	OntologyDb ontology = Lex.getOntologyDb();

	Wordnet wordnet = Wordnet.getInstance();
	
	class BorderTableRenderer extends TableRenderer
	{
	    public String getTable(String contents)
	    {
	        return "<table class=\"tree\" border>" + contents + "</table>\n";
	    }
	}

	BorderTableRenderer rend = new BorderTableRenderer();
	
	Sheaf sheaf = emdros.getSheaf
	(
		"SELECT ALL OBJECTS IN " +
		emdros.intersect(userTextAccessSet, min_m, max_m) +
		" WHERE [clause self = "+selClauseId+
		"       GET logical_struct_id, logical_structure "+
		"        [phrase GET phrase_type, argument_name, "+
		"                    type_id, macrorole_number "+
		"          [word GET lexeme, phrase_dependent_part_of_speech, "+
		"                    tense, wordnet_gloss, wordnet_synset, " +
		"                    graphical_preformative, " +
		"                    graphical_locative, " +
		"                    graphical_lexeme, " +
		"                    graphical_pron_suffix, " +
		"                    graphical_verbal_ending, " +
		"                    graphical_root_formation, " +
		"                    graphical_nominal_ending, " +
		"                    person, number, gender, state " +
		"          ]"+
		"        ]"+
		"      ]"
	);

	MatchedObject clause = null;
	
	{
		SheafConstIterator sci = sheaf.const_iterator();
		if (sci.hasNext()) 
		{
			Straw straw = sci.next();
			StrawConstIterator swci = straw.const_iterator();
			if (swci.hasNext()) 
			{
				clause = swci.next();
			}
		}
	}

	if (clause == null)
	{
		%><p>Selected clause not found or access denied.</p><%
	}
	else
	{
		StringBuffer hebrewText = new StringBuffer();
		List morphEdges = new ArrayList();
		
		/* Prescan to list morpheme edges */
		
		{
			TreeNode root = new TreeNode("root");
			HebrewMorphemeGenerator gen = new HebrewMorphemeGenerator(emdros);
			
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
	
						class HebrewFeatureConverter implements MorphemeHandler
						{
							private TreeNode m_root;
							private MatchedObject m_word;
							private StringBuffer m_hebrew;
							private List m_morphs;
							
							public HebrewFeatureConverter(TreeNode root,
								MatchedObject word, StringBuffer hebrew,
								List morphs)
							{
								m_root   = root;
								m_word   = word;
								m_hebrew = hebrew;
								m_morphs = morphs;
							}
							
							public void convert(String surface, 
								boolean lastMorpheme, String desc,
								String morphNode)
							{
								String raw = m_word.getEMdFValue(surface).getString();

								String hebrew = HebrewConverter.toHebrew(raw);
								m_hebrew.append(hebrew);

								String translit = HebrewConverter.toTranslit(raw);
								translit = HebrewConverter.toHtml(translit);
								if (translit.equals("")) translit = "&Oslash;";
								if (!lastMorpheme) translit += "-";
								TreeNode node = m_root.createChild(translit);

								node = node.createChild(raw);
								node.createChild(desc);
								
								m_morphs.add(new MorphEdge(morphNode, 
									translit, m_morphs.size()));
							}
						}

						HebrewFeatureConverter hfc = 
							new HebrewFeatureConverter(root, word, hebrewText,
							morphEdges);
						
						gen.parse(word, hfc, true);
						
						hebrewText.append(" ");						
					}
				}
			}
		
			%><p>Hebrew text: <span class="hebrew"><%= 
				HebrewConverter.toHtml(hebrewText.toString())
			%></span></p><%
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
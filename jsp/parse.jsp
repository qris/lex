<%@ page import="java.util.*" %>
<%@ page import="com.qwirx.lex.ontology.*" %>
<%@ page import="com.qwirx.lex.wordnet.*" %>
<%@ page import="com.qwirx.lex.parser.*" %>

<html>
<head>
	<title>Lex: Parser Testing</title>
	<link rel="stylesheet" href="style.css"/>
	<style>
		div.topmenu a.parse_jsp <%@ include file="hilite.inc" %>
		.hilite   { background-color: #fdd; }
		.selected { background-color: #fee; }
		.cb { float: right; }
	</style>
</head>
	
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
	
		for (var i = 0; i < edges.length; i++)
		{
			var edge = edges[i];
			var checkbox = document.forms.rulecmd[edge[0]];
	
			if (checkbox.checked)
			{
				any_checked = true;
	
				if (sel_left == -1 || sel_left > edge[1])
				{
					sel_left = edge[1];
				}
				if (sel_right == -1 || sel_right < edge[2])
				{
					sel_right = edge[2];
				}
			}
		}
	
		for (var i = 0; i < edges.length; i++)
		{
			var edge = edges[i];
			var enabled = false;
			var checkbox = document.forms.rulecmd[edge[0]];
	
			if (!any_checked)
			{
				// allow the first checkbox to be checked
				enabled = true;
			}
			else if (edge[1] == sel_right + 1 || edge[2] == sel_left - 1)
			{
				// allow expansion at the edges
				enabled = true;
			}
			else if (!checkbox.checked)
			{
				enabled = false;
			}
			else if (edge[1] == sel_left || edge[2] == sel_right)
			{
				// allow deselection at the left and right edges only
				enabled = true;
			}
			
			checkbox.disabled = !enabled;
		}
	
		return true;
	}
	
//-->
</script>
	
<body>

<%@ include file="header.jsp" %>
<%@ include file="auth.jsp" %>
<%@ include file="navclause.jsp" %>

<%

	Map phrase_functions = emdros.getEnumerationConstants
		("phrase_function_t",false);

	Map phrase_types = emdros.getEnumerationConstants
		("phrase_type_t",false);
		
	Map parts_of_speech = emdros.getEnumerationConstants
		("psp_t",false);

	Map verbal_stems = emdros.getEnumerationConstants
		("verbal_stem_t",false);

	Map persons = emdros.getEnumerationConstants
		("person_t",false);

	Map numbers = emdros.getEnumerationConstants
		("number_t",false);

	Map genders = emdros.getEnumerationConstants
		("gender_t",false);

	Map states = emdros.getEnumerationConstants
		("state_t",false);

	Map tenses = emdros.getEnumerationConstants
		("verbal_tense_t",false);

	Map stems = emdros.getEnumerationConstants
		("verbal_stem_t",false);

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
		emdros.getMonadSet(userTextAccess, min_m, max_m) +
		" WHERE [clause self = "+selClauseId+
		"       GET logical_struct_id, logical_structure "+
		"        [phrase GET phrase_type, function, argument_name, "+
		"                    type_id, macrorole_number "+
		"          [word GET lexeme, pdpsp, verbal_stem, verbal_tense, " +
		"                    wordnet_gloss, wordnet_synset, " +
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
			
			{
				SheafConstIterator phrases = clause.getSheaf().const_iterator();
	
				while (phrases.hasNext()) 
				{
					MatchedObject phrase =
						phrases.next().const_iterator().next();
	
					String function_name = (String)( phrase_functions.get(
						phrase.getEMdFValue("function").toString())
					);
	
					SheafConstIterator words = phrase.getSheaf().const_iterator();
					while (words.hasNext()) 
					{
						MatchedObject word = words.next().const_iterator().next();
	
						String psp = (String)( parts_of_speech.get(
							word.getEMdFValue("pdpsp").toString()) 
						);
						
						class HebrewFeatureConverter
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
						
						String person = (String)persons.get(
							word.getEMdFValue("person").toString());
						if      (person.equals("pers_first"))  person = "1";
						else if (person.equals("pers_second")) person = "2";
						else if (person.equals("pers_third"))  person = "3";
						
						String gender = ((String)genders.get(
							word.getEMdFValue("gender").toString()
							)).substring(0, 1);
						
						String number = ((String)numbers.get(
							word.getEMdFValue("number").toString()
							)).substring(0, 1);
						
						String state = (String)states.get(
							word.getEMdFValue("state").toString());
						
						String gloss = word.getEMdFValue("wordnet_gloss")
							.getString();
						
						if (gloss.equals(""))
						{
							String lexeme = word.getEMdFValue("lexeme")
								.getString();
								
							OntologyDb.OntologyEntry entry = 
								ontology.getWordByLexeme(lexeme);
								
							if (entry != null)
							{
								gloss = entry.m_EnglishGloss;
							}
							else
							{
								gloss = null;
							}
						}
	
						if (psp.equals("verb"))
						{
							hfc.convert("graphical_preformative", false,
								(String)tenses.get(word
								.getEMdFValue("verbal_tense").toString()),
								"V/TNS");
							hfc.convert("graphical_root_formation", false,
								(String)stems.get(word
								.getEMdFValue("verbal_stem").toString()),
								"V/STM");
							hfc.convert("graphical_lexeme", false, gloss,
								"V/LEX");
							hfc.convert("graphical_verbal_ending", true,
								person + gender + number, "V/PGN");
						}
						else if (psp.equals("noun")
							|| psp.equals("proper_noun"))
						{
							String type = "HEAD/NCOM";
							
							if (psp.equals("proper_noun"))
							{
								type = "HEAD/NPROP";
							}
							
							hfc.convert("graphical_lexeme", false, gloss, type);
							hfc.convert("graphical_nominal_ending", true,
								gender + number + "." + state, "MARK/N");
						}
						else
						{
							String type;

							if (psp.equals("adjective"))
							{
								type = "ADJ";
							}
							else if (psp.equals("adverb"))
							{
								type = "ADV";
							}
							else if (psp.equals("article"))
							{
								type = "DET";
							}
							else if (psp.equals("conjunction"))
							{
								type = "CONJ";
							}
							else if (psp.equals("demonstrative_pronoun"))
							{
								type = "PRON/DEM";
							}
							else if (psp.equals("interjection"))
							{
								type = "INTJ";
							}
							else if (psp.equals("interrogative"))
							{
								type = "INTR";
							}
							else if (psp.equals("interrogative_pronoun"))
							{
								type = "PRON/INT";
							}
							else if (psp.equals("negative"))
							{
								type = "NEG";
							}
							else if (psp.equals("personal_pronoun"))
							{
								type = "PRON/PERS";
							}
							else if (psp.equals("preposition"))
							{
								type = "P";
							}
							else
							{
								throw new IllegalArgumentException("Unknown " +
									"part of speech: " + psp);
							}
							
							hfc.convert("graphical_lexeme", true, psp, type);
						}	
						
						hebrewText.append(" ");						
					}
				}
			}
		
			%><p>Hebrew text: <span class="hebrew"><%= 
				HebrewConverter.toHtml(hebrewText.toString())
			%></span></p><%
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
				<p>The edges found were:</p> 
				<form name="rulecmd">
				<%
				
				Map  edgeIds   = new Hashtable();
				Map  idEdges   = new Hashtable();
				Map  fakeChild = new Hashtable();
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
				
				class LeftComparator implements Comparator
				{
					public int compare(Object o1, Object o2) 
					{
						Edge e1 = (Edge)o1;
						Edge e2 = (Edge)o2;
						return e1.getLeftPosition() - e2.getLeftPosition();
					}
				}
				
				Comparator comp = new LeftComparator();
				
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
								><% /*
								[<%= edgeIds.get(next) %>] <% */ %>
								<span class="cb">
									<input name="<%= edgeIds.get(next) %>"
										type="checkbox" 
										onClick="return on_checkbox_click(this);" />
								</span>
								<%= next.getHtmlLabel() %>
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
					new Array("<%= id %>", <%= 
						e.getLeftPosition() %>, <%=
						e.getRightPosition() %>, "<%=
						e.symbol() %>", <%=
						e.isTerminal() %>)<%
					if (i.hasNext())
					{
						%>,
						<%
					}
				}
				%>
				);
				
				//--></script>
										
				<p>Add a new rule:</p>
				
				<table>
					<tr bgcolor="#ffcccc">
						<th>Top node name (symbol)</th>
						<th>Component node names (parts)</th>
					</tr>
					<tr bgcolor="#ffeeee">
						<td>
							<input name="new_rule_sym" />
						</td>
						<td>
							<input name="new_rule_parts" />
						</td>
					</tr>
				</table>
				
				</form>
				<%
			}
			else
			{
				%>
				<h3>Parse finished</h3>
				<p>The trees found were:</p> 
				<%
				for (int i = 0; i < sentences.size(); i++)
				{
					Edge sentence = (Edge)( sentences.get(i) );
					
					%>
					<h4>Tree <%= i + 1 %></h4>
					<%= sentence.toTree().toHtml(rend) %>
					<%
				}
			}
		}
	}
	
%>

</body></html>	

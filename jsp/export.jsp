<%@ page import="java.util.*" %>
<%@ page import="java.util.regex.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
<%@ page import="com.qwirx.lex.sql.*" %>
<% response.setContentType("text/plain"); %>
<%

	/*
	if (request.getParameter("features") != null) {
		List features = Lex.getNewFeatures();
		for (Iterator i = features.iterator(); i.hasNext(); ) {
			FeatureInfo f = (FeatureInfo)( i.next() );
%>
UPDATE OBJECT TYPE [ 
	<%= f.objectType %> 
	ADD <%= f.featureName %> : <%= f.featureType %>;
] GO
<%
		}
	}
	*/
	
	String user = request.getRemoteUser() + "@" + request.getRemoteAddr();
	SqlDatabase    db     = Lex.getSqlDatabase   (user);
	EmdrosDatabase emdros = Lex.getEmdrosDatabase(user);
	int min_m = emdros.getMinM(), max_m = emdros.getMaxM();

	{
		Sheaf clauseSheaf = emdros.getSheaf
			("SELECT ALL OBJECTS IN {1-"+max_m+"} "+
			 "WHERE [clause GET logical_struct_id]");
			 
	 	SheafConstIterator sci = clauseSheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject clause = straw.const_iterator().next();
				
			int thisLogicalStructId = 
				clause.getEMdFValue("logical_struct_id").getInt();
			
%>
UPDATE OBJECT BY ID_D = <%= clause.getID_D() %>
[clause logical_struct_id := <%= thisLogicalStructId %>;] 
GO
<%
		}
	}

	{	 
		Sheaf phraseSheaf = emdros.getSheaf
			("SELECT ALL OBJECTS IN {1-"+max_m+"} "+
			 "WHERE [phrase GET argument_name, type_id]");
		
	 	SheafConstIterator sci = phraseSheaf.const_iterator();
		while (sci.hasNext()) {
			Straw straw = sci.next();
			MatchedObject phrase = straw.const_iterator().next();
				
			String argName = 
				phrase.getEMdFValue("argument_name").getString();
			int typeId = 
				phrase.getEMdFValue("type_id").getInt();
			
%>
UPDATE OBJECT BY ID_D = <%= phrase.getID_D() %>
[phrase
	argument_name := "<%= argName %>";
	type_id       := <%= typeId %>;
] 
GO
<%
		}
	}
%>

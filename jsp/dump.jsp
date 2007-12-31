<% String pageTitle = "Emdros Database Dump"; %>
<%@ include file="header2.jsp" %>

<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Vector" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="java.util.SortedSet" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="jemdros.*" %>
<%@ page import="com.qwirx.lex.*" %>
<%@ page import="com.qwirx.db.sql.*" %>
<%@ page import="com.qwirx.lex.emdros.*" %>
	
<%@ include file="auth.jsp" %>

<%@ include file="navclause.jsp" %>

<%
	if (username.equals("anonymous"))
	{
		%>Sorry, you must log in to access this feature.<%
		return;
	}

	class MonadRange 
	{
		int first, last;
	}
	
	class DbObject 
	{
		String type;
		int id;
		int firstMonad, lastMonad;
		MonadRange [] monadRanges;
		Hashtable features;
	}

	int left = 728, right = 734;

	int clauseId = selClauseId;

	Sheaf sheaf = emdros.getSheaf
	(
		"SELECT ALL OBJECTS IN " +
		emdros.intersect(userTextAccessSet, min_m, max_m) +
		" WHERE [clause self = "+clauseId+"]"
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

	if (clause != null) 
	{
		Table monads = emdros.getTable
			("GET MONADS FROM OBJECT WITH ID_D = "+clause.getID_D()+" "+
			 "[clause]");
		TableRow tr = monads.iterator().next();
		left  = Integer.parseInt(tr.getColumn(2));
		right = Integer.parseInt(tr.getColumn(3));
	} 
	else 
	{
		%>Null clause!<%
	}
	
	Table objects = emdros.getTable("SELECT OBJECTS HAVING MONADS IN " +
		"{" + left + "-" + right + "} [ALL] GO");

	int numObjects = objects.size();
	DbObject [] dbObjects = new DbObject [numObjects];
	TableIterator ti = objects.iterator();

	// collect information about each object, specifically the set of monads
	// which fall within our range (between left and right borders)

	for (int objectNum = 0; objectNum < numObjects; objectNum++) 
	{
		TableRowIterator tri = ti.next().iterator();

		DbObject object = new DbObject();
		object.type = tri.next();
		object.firstMonad = Integer.parseInt(tri.next());

		String objectIdString = tri.next();
		object.id = Integer.parseInt(objectIdString);
		
		Table monad_ranges = emdros.getTable("GET MONADS FROM OBJECTS " +
			"WITH ID_Ds = "+objectIdString+" ["+object.type+"] GO");
		
		int numRanges = monad_ranges.size();
		Vector monadRangeVector = new Vector();
		
		TableIterator mi = monad_ranges.iterator();
		for (int rangeNum = 0; rangeNum < numRanges; rangeNum++) 
		{
			TableRowIterator mri = mi.next().iterator();
			mri.next(); // skip the object id_d, we know what it is
			MonadRange range = new MonadRange();
			range.first = Integer.parseInt(mri.next());
			range.last = Integer.parseInt(mri.next());
			if (range.last >= left && range.first <= right) 
			{
				if (range.first < left)
					range.first = left;
				if (range.last > right)
					range.last = right;
				if (range.last - range.first >= 0)
					monadRangeVector.add(range);
			}
		}

		if (monadRangeVector.size() == 0) 
		{
			throw new RuntimeException("no valid ranges in object ["+
				object.type+" "+object.id+"]");
		}
		
		object.monadRanges = new MonadRange[monadRangeVector.size()];
		monadRangeVector.copyInto(object.monadRanges);

		String query = "SELECT FEATURES FROM [" + 
			object.type + "]";

		%>
		<!-- <p><%= object.type %> '<%= query %>'</p> -->
		<%
		
		Table features = emdros.getTable(query);
		int numFeatures = features.size();
		String [] featureNames = new String [numFeatures];
		StringBuffer featureValueQuery = new StringBuffer("GET FEATURES ");

		{
			TableIterator ti2 = features.iterator();
			while (ti2.hasNext()) 
			{
				TableRow tr = ti2.next();
				%>
				<!--<p><%= object.type %> <%= tr.getColumn(1) %></p>-->
				<%
			}
		}
		
		{
			TableIterator fni = features.iterator();
			int index = 0;
			for (int i = 0; i < numFeatures; i++) 
			{
				TableRowIterator fnri = fni.next().iterator();
				String name = fnri.next();
				if (i < numFeatures - 1)
				{
					featureValueQuery.append(name+",");
				}
				else
				{
					featureValueQuery.append(name);
				}
			}
		}
		
		featureValueQuery.append(" FROM OBJECTS WITH ID_DS = "+object.id+
			" ["+object.type+"] GO");
		object.features = new Hashtable();
		Table featureValues = emdros.getTable(featureValueQuery.toString());

		TableIterator fvi;
		TableIterator fni = features.iterator();
		fvi = featureValues.iterator();
		TableRowIterator fvri = fvi.next().iterator();
		fvri.next(); // skip the id_d column of GET FEATURES result
		
		while (fni.hasNext()) 
		{
			TableRowIterator fnri = fni.next().iterator();
			String name = fnri.next();
			String value = fvri.next();
			object.features.put(name, value);
		}
		
		dbObjects[objectNum] = object;
	}
	
	if (dbObjects.length > 0) 
	{
		%>
		<table border>
		<tr>
		<th>type</th>
		<%
		
		for (int i = left; i <= right; i++) 
		{
			%>
			<th><%= i %></th>
			<%
		}
		
		%>
		</tr>
		<%
		
		for (int objectNum = 0; objectNum < dbObjects.length; objectNum++) 
		{
			DbObject object = dbObjects[objectNum];
			Object [] featureNames = new TreeSet(object.features.keySet())
				.toArray();
			MonadRange [] ranges = object.monadRanges;
			
			for (int featureNum = -1; featureNum < featureNames.length; featureNum++) 
			{
				String featureName;

				if (featureNum == -1) 
				{
					featureName = null;
				} 
				else 
				{
					featureName = (String)( featureNames[featureNum] );
				}
				
				/*
				if (featureName != null &&
					! featureName.equals("lexeme")) {
					continue;
				}
				*/
				
				if (featureName == null) 
				{
					%>
					<tr>
					<td><b><%= object.type %></b></td>
					<%
				} 
				else 
				{
					%>
					<tr>
					<td>&nbsp;&nbsp;<i><%= featureName %></i></td>
					<%
				}
				
				String featureValue = null;
				if (featureNum >= 0)
					featureValue = 
						((String)( object.features.get(featureName) ))
						.replaceAll("<", "&lt;")
						.replaceAll(">", "&gt;");
		
				/* pad up to the first monad */		
				MonadRange firstRange = ranges[0];
				if (firstRange.first > left) 
				{
					%>
					<td colspan="<%= firstRange.first - left %>"/>
					<%
				}
				
				for (int rangeNum = 0; rangeNum < ranges.length; rangeNum++) 
				{
					MonadRange range = ranges[rangeNum];
					
					%>
					<td bgcolor="#E0E0E0" colspan="<%= range.last - range.first + 1 %>"><%
					if (featureNum >= 0) 
					{
						%><%= featureValue %><%
					} 
					else 
					{
						%><%= object.id %><%
					}
					%></td>
					<%
					
					if (rangeNum < ranges.length - 1) 
					{
						%>
						<td colspan="<%= ranges[rangeNum+1].first - range.last - 1 %>"/>
						<%
					}
				}
				
				MonadRange lastRange = ranges[ranges.length-1];
				if (lastRange.last < right) 
				{
					%>
					<td colspan="<%= right - lastRange.last %>"/>
					<%
				}
			}
			
			%>
			</tr>
			<%
		}
		
		%>
		</table>
		<%
	} 
	else 
	{
		%>No objects found<%
	}
%>
</body></html>
<%@ include file="cleanup.jsp" %>
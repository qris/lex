<%
	}
	finally
	{
		sql.finish();
		sql.close();
		
		if (emdros != null)
		{
			Lex.putEmdrosDatabase(emdros);
		}
	}
%>
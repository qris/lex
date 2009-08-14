<%

	String username = request.getRemoteUser();
	if (username == null)
	{
		username = "anonymous";
	}

	String hostname = request.getRemoteAddr();
	String userhost = username + "@" + hostname;

	SqlDatabase    sql    = Lex.getSqlDatabase(userhost);
	EmdrosDatabase emdros = null;
		
	try
	{
		emdros = Lex.getEmdrosDatabase(username, hostname, sql);
		int min_m = emdros.getMinM(), max_m = emdros.getMaxM();
		int real_min_m = min_m, real_max_m = max_m;
		
%>
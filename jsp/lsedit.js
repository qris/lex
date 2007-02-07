function doFilter(inputbox, list, source) 
{
	if (source == null) return;
	
	var query = inputbox.value;
	var oldid = -1;
	
	if (list.selectedIndex >= 0)
	{
		oldid = list.options[list.selectedIndex].value;
	}
	
	list.options.length = 0;
	var optIndex = 0;
	
	for (var i = 0; i < source.length; i++)
	{
		if (source[i] == null) return;
		
		var id    = source[i][0];
		var label = source[i][1];
		if (label.indexOf(query) >= 0)
		{
			var shortLabel = label;
			
			if (shortLabel.length > 60)
			{
				shortLabel = shortLabel.substring(0, 60) + "...";
			}
			 
			list.options[optIndex] = new Option(shortLabel, id);
			if (id == oldid)
			{
				list.selectedIndex = optIndex;
			}
			
			optIndex++;
		}
	}
}

function selected(selectbox)
{
	return selectbox.options[selectbox.selectedIndex];
}

function getLS(prefix)
{
	var f = document.forms.lsform;
	var ls = "";

	if (f.ls_caused.checked)
	{
		ls += "do'(<x>, " + 
			document.forms.oslash.oslash.value + 
			") CAUSE ";
	}
	
	if (f.ls_punct.checked)
	{
		if      (f.ls_punct_result[0].checked) ls += "INGR ";
		else if (f.ls_punct_result[1].checked) ls += "SEMEL ";
	}
	
	if (f.ls_telic.checked) ls += "BECOME ";
	
	if (f.ls_dynamic[1].checked)
	{
		ls += "do'(<x>, [";
	}
	
	ls += f.ls_pred.value;
							
	if (f.ls_trel.selectedIndex >= 0)
	{	   
		ls += them_rels[selected(f.ls_trel).value][2];
	}
	
	if (f.ls_dynamic[1].checked)
	{
		ls += "])";

		if (f.ls_endpoint[1].checked)
		{
			ls += " & INGR " + f.ls_pred_2.value + 
				"(<" + selected(f.ls_arg_2).value + ">)";
		}
	}
	
	return ls;
}

function updateLS()
{
	var f = document.forms.lsform;

	var punctual = f.ls_punct.checked;
	f.ls_punct_result[0].disabled = !punctual;
	f.ls_punct_result[1].disabled = !punctual;
	
	var dynamic = f.ls_dynamic[1].checked;
	f.ls_endpoint[0].disabled = !dynamic;
	f.ls_endpoint[1].disabled = !dynamic;
	f.ls_pred_2.disabled = !dynamic || !f.ls_endpoint[1].checked;
	f.ls_arg_2.disabled  = !dynamic || !f.ls_endpoint[1].checked;
	
	var trel_old_value = "";
	if (f.ls_trel.selectedIndex >= 0)
	{
		var trel_old_info = them_rels[selected(f.ls_trel).value];
		if (trel_old_info != null)
		{
			trel_old_value = trel_old_info[0];
		}
	}
	var trel_new_index = 0;
	f.ls_trel.options.length = 0;
	
	for (var i = 0; i < them_rels.length; i++)
	{
		var rel = them_rels[i];
		if (rel == null && i == them_rels.length - 1) continue;
		if (!dynamic && rel[0].substring(0, 3) != ("STA")) continue;
		if (dynamic  && rel[0].substring(0, 3) != ("ACT")) continue;
		var new_index = f.ls_trel.options.length;
		f.ls_trel.options[new_index] = new Option(rel[1], i);
		if (rel[0] == trel_old_value) 
		{ 
			trel_new_index = new_index; 
		}
	}
	
	f.ls_trel.selectedIndex = trel_new_index;
	f.ls_trel_text.value = them_rels[selected(f.ls_trel).value][2];

	f.ls.value = getLS("ls");
		
	return true;
}

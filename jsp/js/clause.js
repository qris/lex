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

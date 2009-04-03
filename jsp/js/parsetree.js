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

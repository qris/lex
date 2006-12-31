package com.qwirx.lex;

public class TableRenderer
{
    public String getTable(String contents)
    {
        return "<table>" + contents + "</table>";
    }

    public String getRow(String contents)
    {
        return "<tr>" + contents + "</tr>";
    }
    
    public String getCell(String contents, int width, int height)
    {
    	if (width  < 1) { return "bad width"; }
    	if (height < 1) { return "bad height"; }
        
        String colspan = "";
        if (width > 1)
        {
            colspan = " colspan=\"" + width + "\"";
        }

        String rowspan = "";
        if (height > 1)
        {
            rowspan = " rowspan=\"" + height + "\"";
        }

        return "<td"+colspan+rowspan+">" + contents + "</td>";
    }    

    private void assert(boolean condition) throws AssertionError
    {
    	if (!condition) throw new AssertionError();
    }
}


package com.qwirx.lex;

public class TableRenderer
{
    public String getTable(String contents)
    {
        return "<table>\n" + contents + "</table>\n";
    }

    public String getRow(String contents)
    {
        return "  <tr>\n" + contents + "  </tr>\n";
    }
    
    public String getCell(String contents, String clazz, int width, int height)
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
        
        String classString = "";
        if (clazz != null)
        {
            classString = " class=\"" + clazz + "\"";
        }
        
        return "    <td"+colspan+rowspan+classString+">" + 
            contents + "</td>\n";
    }    
}


package com.qwirx.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CommaSeparatedValueParser
{
    public static String [][] parse(InputStream input)
    throws IOException
    {
        BufferedReader reader = 
            new BufferedReader(new InputStreamReader(input));
        List rows = new ArrayList();
        
        for (String line = reader.readLine(); line != null; 
            line = reader.readLine())
        {
            List fields = new ArrayList();
            StringBuffer currentField = new StringBuffer();
            boolean inQuotes = false;
            
            for (int i = 0; i < line.length(); i++)
            {
                char c = line.charAt(i);
                if (c == '"')
                {
                    inQuotes = !inQuotes;
                }
                else if (c == ',' && !inQuotes)
                {
                    fields.add(currentField.toString());
                    currentField.setLength(0);
                }
                else
                {
                    currentField.append(c);
                }
            }
            
            fields.add(currentField.toString());
            
            String [] row = new String [fields.size()];
            row = (String[])fields.toArray(row);
            rows.add(row);
        }
        
        String [][] rowArray = new String[rows.size()][];
        rowArray = (String[][])rows.toArray(rowArray);
        return rowArray;
    }
}

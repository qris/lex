package com.qwirx.db;

import java.util.HashMap;
import java.util.Map;

import jemdros.EmdrosEnv;
import jemdros.MatchedObject;
import jemdros.Sheaf;
import jemdros.SheafConstIterator;
import jemdros.Straw;
import jemdros.StrawConstIterator;
import jemdros.StringList;
import jemdros.StringListConstIterator;
import jemdros.eCharsets;
import jemdros.eOutputKind;

public class CrashEmdrosByCopyingAllFeatures
{
    public static void main(String [] args) throws Exception
    {
        System.load("/home/chris/tomcat/common/lib/libjemdros.so");
        
        EmdrosEnv env = new EmdrosEnv(eOutputKind.kOKConsole, 
            eCharsets.kCSISO_8859_1, "localhost", "emdf", "changeme",
            "wihebrew"); 

        if (!env.connectionOk()) 
        {
            throw new DatabaseException("Failed to connect to database",
                new Exception(env.getDBError()));
        }

        String query = "SELECT ALL OBJECTS IN {27} " +
            "WHERE [word GET tense]";
        
        boolean[] bCompilerResult = new boolean[1];
        boolean bDBResult = env.executeString(query, bCompilerResult, false, 
            false);
    
        if (!bDBResult)
        {
            throw new Exception("Database error: " + env.getDBError());
        }

        if (!bCompilerResult[0]) 
        {
            throw new Exception("Compiler error: " + env.getCompilerError());
        }
        
        Sheaf sheaf = env.takeOverSheaf();        
        SheafConstIterator sci = sheaf.const_iterator();
        Straw straw = sci.next();
        StrawConstIterator swci = straw.const_iterator();
        MatchedObject word = swci.next();
        
        Map<String, String> attributes = new HashMap<String, String>();
        
        for (int i = 0; i < 1000000; i++)
        {
            StringList features = word.getFeatureList();
            
            // Using the code commented out below, with StringVector
            // instead of StringList, does not cause the crash.
            
            // StringVector features = word.getFeatureList().getAsVector();

            for (StringListConstIterator slci = features.const_iterator();
                slci.hasNext();)
            // for (int j = 0; j < features.size(); j++)
            {
                String feature = slci.next();
                // String feature = features.get(j);
                int index = word.getEMdFValueIndex(feature);
                if (index < 0)
                {
                    throw new Exception(feature);
                }
                String value = word.getFeatureAsString(index);
                attributes.put("word_" + feature, value);
            }
            
            if (features.isEmpty()) throw new Exception();
        }
    }
}

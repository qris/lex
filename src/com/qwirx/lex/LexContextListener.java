package com.qwirx.lex;

import javax.servlet.ServletContextEvent;

import org.apache.log4j.Logger;

import com.qwirx.lex.hebrew.HebrewEnglishDatabase;
import com.qwirx.lex.ontology.OntologyDb;
import com.qwirx.lex.wordnet.Wordnet;

public class LexContextListener extends Object implements
    javax.servlet.ServletContextListener
{
    private static final Logger LOG = Logger.getLogger(LexContextListener.class);
    
    public void contextInitialized(ServletContextEvent arg0)
    {
        LOG.warn("Lex is loading");
        
        /*
        try
        {
            Wordnet.getInstance();
        }
        catch (Exception e)
        {
            LOG.error("Failed to initialise Wordnet", e);
        }
        */
        
        try
        {
            Lex.getWivuLexicon();
        }
        catch (Exception e)
        {
            LOG.error("Failed to load WIVU lexicon", e);
        }

        try
        {
            OntologyDb.getInstance();
        }
        catch (Exception e)
        {
            LOG.error("Failed to initialise ontology database", e);
        }

        try
        {
            HebrewEnglishDatabase.getInstance();
        }
        catch (Exception e)
        {
            LOG.error("Failed to initialise Hebrew DiB database", e);
        }
    }

    public void contextDestroyed(ServletContextEvent arg0)
    {
        LOG.warn("Lex is shutting down");
        // Wordnet.delete();
        OntologyDb.delete();
        HebrewEnglishDatabase.delete();
        Lex.emptyPools();
    }

}

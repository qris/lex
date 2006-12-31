package com.qwirx.lex;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * Servlet filter to time page requests
 */

public class TimerFilter implements Filter 
{
    /** Log4j logger */
    private static Logger m_LOG = Logger.getLogger(TimerFilter.class);

    /** Perform the filter */
    public void doFilter(ServletRequest request, ServletResponse response, 
            FilterChain chain)
   		throws IOException, ServletException
    {
        HttpServletRequest hsr = (HttpServletRequest)request;
        
        m_LOG.info("Request starting for "+hsr.getRequestURI());
        long startTime = System.currentTimeMillis();
        
        chain.doFilter(request, response);
        
        long totalTime = System.currentTimeMillis() - startTime;
        m_LOG.info("Request finished in "+totalTime+"ms for "+
                hsr.getRequestURI());
    }

    /** Need to provide this method to implement Filter */
    public void init(FilterConfig config) throws ServletException 
    {
        m_LOG.info("initialising");
    }

    /** Need to provide this method to implement Filter */
    public void destroy() 
    {
        m_LOG.info("removed");
    }
}

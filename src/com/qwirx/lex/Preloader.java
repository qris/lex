/*
 * Created on 11-Nov-2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex;

import java.net.URL;

/**
 * @author chris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Preloader {
	static {
		String osName = System.getProperty("os.name");

		if (osName.matches(".*[Ww]in.*")) 
        {
			// Substitute your path
			System.load("c:\\programmer\\emdros\\lib\\jemdros.dll");
		} 
        else 
        {
            URL url = Preloader.class.getResource("/../lib/");
            
            if (url == null)
            {
                url = Preloader.class.getResource("/../jsp/WEB-INF/lib/");
            }
            
            if (url == null)
            {
                url = Preloader.class.getResource("/");
            }

            String path = url.getPath() + "libjemdros.so";
            if (! new java.io.File(path).exists())
            {
                path = "/usr/local/lib/emdros/libjemdros.so";
            }
            if (! new java.io.File(path).exists())
            {
                path = "/home/chris/tomcat/common/lib/libjemdros.so";
            }
            
            if (new java.io.File(path).exists())
            {
                System.out.println("Library path: "+path);
                System.load(path);
            }
            else
            {
                System.out.println("Library not found, trying System.loadLibrary()");
                System.loadLibrary("libjemdros.so");
            }
		}
	}
}

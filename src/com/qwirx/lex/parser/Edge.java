/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.Map;
import java.util.Vector;

import com.qwirx.lex.parser.EdgeBase.AlreadyBoundException;

public interface Edge
{
	void appendString(StringBuffer buf);
	void appendWords(Vector buf);
	Edge[] parts();
	Edge part(int partNum);
	String symbol();
	String partName(int partNum);
	Map attributes();
	String attribute(String name);
    void bindTo(RuleEdge container, RulePart location)
    throws AlreadyBoundException;
    RuleEdge getBoundInstance();
    RulePart getBoundLocation();
    
    Edge getBoundCopy();
    Edge getUnboundCopy();
    
    boolean isAt(int pos);
    boolean includes(Edge other);
}
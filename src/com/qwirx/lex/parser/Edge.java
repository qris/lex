/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.qwirx.lex.TreeNode;
import com.qwirx.lex.parser.EdgeBase.AlreadyBoundException;
import com.qwirx.lex.parser.Rule.Attribute;

public interface Edge
{
	void appendString(StringBuffer buf);
	void appendWords(Vector buf);
	Edge[] parts();
	Edge part(int partNum);
    Edge part(String name, int index);
    Edge [] parts(String name);
	String symbol();
	String partName(int partNum);
	Map attributes();
    void addAttribute(Attribute attr);
	String attribute(String name);
    boolean isTerminal();
    
    void bindTo(RuleEdge container, RulePart location)
        throws AlreadyBoundException;
    RuleEdge getBoundInstance();
    RulePart getBoundLocation();
    
    // Edge getBoundCopy();
    Edge getUnboundCopy();
    Edge getUnboundOriginal();
    
    boolean isAt(int pos);
    boolean includes(Edge other);
    TreeNode toTree();
    String getHtmlLabel();
    
    void getLeavesInto(List leaves);
    boolean overlaps(Edge other);
    int getLeftPosition();
    int getRightPosition();
    int getDepth();
}
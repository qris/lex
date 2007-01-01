/*
 * Created on 24-Jan-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.qwirx.lex.parser;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import com.qwirx.lex.parser.EdgeBase.AlreadyBoundException;

public class Rule 
{
	private final int         id;
	private final String      left;
	private final RulePart [] right;
	private final Map         fixedAttrs, copyAttrs;
	private final boolean     m_isPermutable;
    private final boolean     m_isSearching;
    private final List []     m_PartBindingLists; 
    private final boolean []  m_PartsFinished;
    private       boolean []  m_PositionsUsed;
    
	public Rule
    (
            int id, String left, RulePart [] right, 
			Map fixedAttrs, Map copyAttrs, boolean isPermutable, 
            boolean isSearching
    ) 
    {
		this.id = id;
		this.left = left;
		this.right = right;
		this.fixedAttrs = fixedAttrs;
		this.copyAttrs  = copyAttrs;
        
        m_isPermutable = isPermutable;
        m_isSearching  = isSearching;
        m_PartBindingLists = new List [right.length];
        m_PartsFinished = new boolean [right.length];
        
        for (int i = 0; i < right.length; i++)
        {
            m_PartBindingLists[i] = new ArrayList();
            m_PartsFinished[i] = false;
        }
	}

	public Rule(int id, String left, RulePart [] right, boolean isPermutable,
            boolean isSearching) 
    {
		this(id, left, right, new Hashtable(), new Hashtable(),
                isPermutable, isSearching);
	}

	public static Rule makeFromString(int id, String left, 
			String right, Map fixedAttrs, Map copyAttrs) 
	throws IllegalArgumentException 
    {
        Map names = new Hashtable();
     
        boolean isPermutable = false;
        boolean isSearching  = false;
        
        String [] rightStrings = right.split(" ");
        
        if (rightStrings.length > 0)
        {
            String first = rightStrings[0];
            if (first.equals("#") || first.equals("*"))
            {
                if (first.equals("#"))
                {
                    isPermutable = true;
                }
                else if (first.equals("*"))
                {
                    isSearching = true;
                }

                String [] newRightStrings = new String [rightStrings.length - 1];
                System.arraycopy(rightStrings, 1, newRightStrings, 0,
                        rightStrings.length - 1);
                rightStrings = newRightStrings;
            }
        }
        
		RulePart [] rightParts = new RulePart[rightStrings.length];
        
		for (int i = 0; i < rightStrings.length; i++) {
            RulePart part = RulePart.fromString(rightStrings[i]);
            rightParts[i] = part;
            
            if (part.name() != null)
            {
                String name = part.name();
                
                if (names.get(name) != null)
                {
                    throw new DuplicateNameException
                    (
                            name, (RulePart)names.get(name), right
                    );
                }
                
                names.put(name, part);
            }
		}
		return new Rule(id, left, rightParts, fixedAttrs, copyAttrs, 
                isPermutable, isSearching);
	}

	public static Rule makeFromString(int id, String left, 
			String right) 
	throws IllegalArgumentException 
    {
		return makeFromString(id, left, right, new Hashtable(),
				new Hashtable());
	}
	
	public int    id()     { return id;   }
	public String symbol() { return left; }
	public RulePart[] parts() {
		RulePart[] result = new RulePart [right.length];
		System.arraycopy(right, 0, result, 0, right.length);
		return result;
	}
    public RulePart part(int i) { return right[i]; }
    public RulePart part(String name)
    {
        for (int i = 0; i < right.length; i++)
        {
            if (right[i].symbol().equals(name))
            {
                return right[i];
            }
        }
        return null;
    }
	public Map fixedAttributes() {
		return new Hashtable(fixedAttrs);
	}
	public Map copiedAttributes() {
		return new Hashtable(copyAttrs);
	}
    public boolean isPermutable() { return m_isPermutable; }
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(left);
		buf.append(" := ");
		for (int i = 0; i < right.length; i++) {
			buf.append(right[i].toString());
			if (i < right.length - 1)
				buf.append(' ');
		}
		return buf.toString();
	}
    
    public class Match
    {
        public Match(int depth)
        {
            counts = new int [depth];
            for (int i = 0; i < depth; i++)
            {
                counts[i] = -1;
            }
            contained = new Vector();
        }
        public Match(Match toCopy)
        {
            counts = new int [toCopy.counts.length];
            System.arraycopy(toCopy.counts, 0, counts, 0, counts.length);
            contained = new Vector();
            for (Iterator i = toCopy.contained.iterator(); i.hasNext(); )
            {
                Edge instance = (Edge)( i.next() );
                contained.add(instance.getBoundCopy());
            }
        }
        int [] counts;
        List contained;
        Stack stack;
        public String toString()
        {
            return stack.toString();
        }
        public Stack getStack() 
        { 
            Stack result = new Stack();
            for (Iterator s = stack.iterator(); s.hasNext(); )
            {
                Edge i = (Edge)( s.next() );
                result.add(i.getBoundCopy());
            }
            return result;
        }
        public boolean compareToStack(Stack other)
        {
            if (other.size() != stack.size())
                return false;
            
            for (int i = 0; i < stack.size(); i++)
            {
                if (other.get(i) != stack.get(i))
                    return false;
            }
            
            return true;
        }
    }
    
    private int m_NumPartsRemaining;
    
    private void hidePart(int index)
    {
        if (m_PartsFinished[index])
        {
            throw new AssertionError("the current rule part should " +
                "not be marked to be skipped");
        }
        m_PartsFinished[index] = true;
        
        if (m_NumPartsRemaining <= 0)
        {
            throw new AssertionError("the number of remaining parts " +
                "should be greater than zero");
        }
        m_NumPartsRemaining--;
    }
    
    private void unhidePart(int index)
    {
        if (!m_PartsFinished[index])
        {
            throw new AssertionError("the current rule part should " +
                "be marked to be skipped");
        }
        m_PartsFinished[index] = false;
        
        if (m_NumPartsRemaining >= right.length)
        {
            throw new AssertionError("the number of remaining parts " +
                "should be less than the total number");
        }
        m_NumPartsRemaining++;
    }
    
    private Chart m_CurrentChart;
    
    /** 
     * Public entry point for parsing.
     * @param chart A chart containing existing edges to apply to
     * @param endPos The right-hand side of the chart, where this rule is
     * required to match this time (it may match elsewhere as well) 
     * @return A list of new Edges to be added to the Chart
     */
    public List applyTo(Chart chart, int endPos)
    {
        m_CurrentChart = chart;
        
        // debugging code
        for (int i = 0; i < right.length; i++)
        {
            if (m_PartBindingLists[i].size() > 0)
            {
                throw new AssertionError
                (
                    "the part binding list should be empty at the start of " +
                    "the parse"
                );
            }
            
            if (m_PartsFinished[i])
            {
                throw new AssertionError
                (
                    "no parts should be already used at the start of the parse"
                );
                
            }
        }
        
        m_PositionsUsed = new boolean[endPos + 1];

        // we must always match an edge at the specified end position,
        // which will be different every time we are called,
        // otherwise we will generate duplicate edges.
        
        List oldEdges = chart.getEdgesAt(endPos);
        List newEdges = new ArrayList();
        
        if (m_isSearching || m_isPermutable)
        {
            // any part can match one or more edges
            
        }
        else
        {
            // the last part must match one or more edges,
            // unless it's optional, in which case it might be skipped
            // and the last-but-one might match, etc.
            
            int firstPartSkipped = right.length;
            
            for (int partIndex = right.length - 1; partIndex >= 0; partIndex--)
            {
                RulePart part = right[partIndex];
                
                for (Iterator i = oldEdges.iterator(); i.hasNext(); )
                {
                    Edge edge = (Edge)( i.next() );
                    if (part.matches(edge))
                    {
                        bindAndContinueParse(partIndex, edge, newEdges);
                    }
                }
                
                if (!part.canSkip())
                {
                    break;
                }
                
                m_PartsFinished[partIndex] = true;
                firstPartSkipped = partIndex;
            }
            
            for (int i = firstPartSkipped; i < right.length; i++)
            {
                if (!m_PartsFinished[i]) throw new AssertionError();
                m_PartsFinished[i] = false;
            }
        }
        
        return newEdges;
    }
    
    private void bindAndContinueParse(int partIndex, Edge edge, List newEdges)
    {
        for (int i = 0; i < right.length; i++)
        {
            if (m_PartBindingLists[i].contains(edge))
            {
                throw new AssertionError
                (
                    "No part binding list should already contain this edge"
                );
            }
        }

        m_PartBindingLists[partIndex].add(0, edge);

        if (m_PartsFinished[partIndex])
        {
            throw new AssertionError
            (
                "Not allowed to bind a rule part that is already finished"
            );
        }
        
        for (int i = 0; i < m_PositionsUsed.length; i++)
        {
            if (!edge.isAt(i))
            {
                continue;
            }
            
            if (m_PositionsUsed[i])
            {
                throw new AssertionError
                (
                    "Not allowed to bind an edge that overlaps an existing " +
                    "bound edge"
                );
            }
            
            m_PositionsUsed[i] = true;
        }
        
        if (right[partIndex].canRepeat())
        {
            continueParseWithThisPart(partIndex, true, newEdges);
        }
        else
        {
            m_PartsFinished[partIndex] = true;
            continueParseWithNextPart(newEdges);        
            m_PartsFinished[partIndex] = false;
        }
        
        for (int i = 0; i < m_PositionsUsed.length; i++)
        {
            if (!edge.isAt(i))
            {
                continue;
            }
            
            if (!m_PositionsUsed[i])
            {
                throw new AssertionError
                (
                    "Positions used by this edge should be marked as used"
                );
            }
            
            m_PositionsUsed[i] = false;
        }

        for (int i = 0; i < right.length; i++)
        {
            if (i == partIndex)
            {
                if (!m_PartBindingLists[i].contains(edge))
                {
                    throw new AssertionError
                    (
                        "The part binding list for this part should " +
                        "contain this edge"
                    );
                }
            }
            else
            {
                if (m_PartBindingLists[i].contains(edge))
                {
                    throw new AssertionError
                    (
                        "No other part binding list should contain this edge"
                    );
                }
            }
        }

        m_PartBindingLists[partIndex].remove(edge);
    }

    private void continueParseWithNextPart(List newEdges)
    {
        boolean foundUnfinishedPart = false;
        
        for (int i = right.length - 1; i >= 0; i--)
        {
            if (m_PartsFinished[i])
            {
                continue;
            }
            
            foundUnfinishedPart = true;
            
            continueParseWithThisPart(i, false, newEdges);
            
            // reordering of elements in permutable rules happens here,
            // by allowing the loop to proceed with the next unfinished part.
            if (!m_isPermutable)
            {
                return;
            }
        }
        
        if (foundUnfinishedPart)
        {
            return;
        }
        
        // base case: we end up here when all parts have been used,
        // and we should be ready to create a new edge.
        
        // don't create new edges which don't contain any other edges:
        // we could keep doing that forever.
        
        int numEdges = 0;
        
        for (int i = 0; i < right.length; i++)
        {
            numEdges += m_PartBindingLists[i].size();
        }
        
        if (numEdges == 0)
        {
            return;
        }

        int position = 0;
        Edge[] contained = new Edge[numEdges];
        
        for (int i = 0; i < right.length; i++)
        {
            for (Iterator j = m_PartBindingLists[i].iterator(); j.hasNext(); )
            {
                Edge edge = (Edge)( j.next() );
                Edge unbound = edge.getUnboundCopy();
                try
                {
                    unbound.bindTo(null, right[i]);
                }
                catch (AlreadyBoundException e)
                {
                    throw new AssertionError(e);
                }
                contained[position++] = unbound;
            }
        }
        
        if (position != numEdges)
        {
            throw new AssertionError();
        }
        
        Edge newEdge = new RuleEdge(this, contained);
        newEdges.add(newEdge);       
    }

    private void continueParseWithThisPart(int partIndex, boolean hasMatched,
        List newEdges)
    {
        if (m_PartsFinished[partIndex])
        {
            throw new AssertionError
            (
                "Not allowed to bind a rule part that is already finished"
            );
        }

        // find any other edges that we can use
        
        int rightmostEmptyPos = -1;
        for (int i = m_PositionsUsed.length - 1; i >= 0; i--)
        {
            if (!m_PositionsUsed[i])
            {
                rightmostEmptyPos = i;
                break;
            }
        }
        
        List edges = m_CurrentChart.getEdgesAt(rightmostEmptyPos);
        RulePart part = right[partIndex];
                
        for (Iterator i = edges.iterator(); i.hasNext(); )
        {
            Edge edge = (Edge)( i.next() );
            if (!part.matches(edge))
            {
                continue;
            }
            
            // check that the edge doesn't overlap any previously chosen ones
            boolean overlaps = false;
            for (int j = rightmostEmptyPos + 1; j < m_PositionsUsed.length; j++)
            {
                if (edge.isAt(j))
                {
                    overlaps = true;
                    break;
                }
            }
            
            if (overlaps == true)
            {
                continue;
            }
            
            bindAndContinueParse(partIndex, edge, newEdges);
        }
        
        // Optional elements may only be skipped (match 0 times)
        // in permutable rules if no matches have yet been made.
        // This enforces the condition that they may not match 
        // nothing at different places in the same input, which 
        // would produce duplicate trees in the output.
        
        boolean canSkip = part.canSkip();
        
        if (canSkip && m_isPermutable)
        {
            for (int i = 0; i < m_PositionsUsed.length; i++)
            {
                if (m_PositionsUsed[i])
                {
                    canSkip = false;
                    break;
                }
            }
        }

        // stop now if this part is not optional and has not matched yet
        if (!canSkip && !hasMatched)
        {
            return;
        }
        
        // mark it as finished, so we can move onto the next part
        // (in the case where it is optional and being skipped)
        
        m_PartsFinished[partIndex] = true;
        
        continueParseWithNextPart(newEdges);
        
        if (!m_PartsFinished[partIndex])
        {
            throw new AssertionError("this part should still be marked" +
                "as fininshed");
        }
        
        m_PartsFinished[partIndex] = false;
    }

    private void apply(List stack, Match match, List out)
    {
        // debug code
        if (true) 
        {
            int realNumPartsRemaining = right.length;
            for (int i = 0; i < right.length; i++)
            {
                if (m_PartsFinished[i])
                {
                    realNumPartsRemaining--;
                }
            }
            if (realNumPartsRemaining != m_NumPartsRemaining)
            {
                throw new AssertionError("mismatch between " +
                        "numPartsRemaining and partsAlreadyUsed");
            }
        }
        
        if (m_NumPartsRemaining == 0)
        {
            // base case

            if (match.contained.size() == 0)
            {
                // don't add matches that don't consume any stack symbols 
                // at all: we could go on doing that forever
                return;
            }
            
            Match matchCopy = new Match(match);
            
            matchCopy.stack = new Stack();
            matchCopy.stack.addAll(stack);

            Edge[] contained = new Edge[0];
            contained = (Edge[])( matchCopy.contained.toArray(contained) );
            
            Edge newInstance = new RuleEdge(this, contained);
            matchCopy.stack.add(newInstance);
            
            out.add(matchCopy);
            return;
        }

        // Theory: if the rule is searching, we check the top/last/most recent
        // object on the stack against each rule part in turn. Any new 
        // (previously unseen) match must consume this element somehow.
        //
        // For each rule part which matches that top element, we fork a search 
        // path where it consumes that element, and do a combinatorial search 
        // on the remaining elements and rules.
        //
        // If the rule is not searching, but is permutable, then we do the same, 
        // but not the combinatorial search.
        //
        // If it is neither searching nor permutable, we consider only the
        // next rule part, and only the item on top of the stack. We fork a 
        // search path for every number of symbols that the rule matches and
        // is allowed to match, including zero for optional rules which
        // don't match any symbols. In each case, we remove those matched
        // symbols from the stack, and call ourselves to continue with the
        // next rule.

        int firstRulePartIndex;
        
        if (m_isPermutable || m_isSearching)
        {
            // rulePartsPossible = parts;
            firstRulePartIndex = 0;
        }
        else
        {
            firstRulePartIndex = right.length;
            
            for (int i = right.length - 1; i >= 0; i--)
            {
                if (!m_PartsFinished[i])
                {
                    firstRulePartIndex = i;
                    break;
                }
            }
            
            if (firstRulePartIndex == right.length)
            {
                throw new AssertionError("should have found an " +
                        "unused rule part");
            }
            // rulePartsPossible = new Vector();
            // rulePartsPossible.add(parts.get(parts.size() - 1));
        }

        for (int i = firstRulePartIndex; i < right.length; i++)
        {
            if (m_PartsFinished[i])
            {
                // the caller has processed this part, and asked us to skip it
                continue;
            }
            
            RulePart nextPart = right[i];
            Match matchCopy = new Match(match);
            Stack stackCopy = new Stack();
            stackCopy.addAll(stack);

            if (m_isSearching)
            {
                Edge stackTop = (Edge)( stack.get(stack.size() - 1) );

                if (!nextPart.matches(stackTop))
                {
                    continue;
                }
                
                stackCopy.remove(stackTop);

                Edge newInstance = stackTop.getUnboundCopy();
                
                try 
                {
                    newInstance.bindTo(null, nextPart);
                }
                catch (AlreadyBoundException e)
                {
                    throw new RuntimeException("Cannot bind the same " +
                            "instance twice", e);
                }
                
                matchCopy.contained.add(0, newInstance);
                applySearch(stackCopy.size() - 1, true, i, 
                        stackCopy, matchCopy, out);
            }
            else
            {
                hidePart(i);
                
                for (int s = 0; s <= stack.size(); s++)
                {
                    // Optional elements may only be skipped (match 0 times)
                    // in permutable rules if no matches have yet been made.
                    // This enforces the condition that they may not match 
                    // nothing at different places in the same input, which 
                    // would produce duplicate trees in the output.
                    
                    boolean canSkip = nextPart.canSkip();
                    
                    if (m_isPermutable && matchCopy.contained.size() > 0)
                    {
                        canSkip = false;
                    }
                    
                    // TODO rewrite this code more clearly
                    if ( (s == 0 && canSkip) ||
                         (s == 1) || 
                         (s >  1 && nextPart.canRepeat()) )
                    {
                        matchCopy.counts[i] = s;
                        apply(stackCopy, matchCopy, out);
                    }
        
                    if (stackCopy.size() == 0)
                        break;
                    
                    Edge stackTop = (Edge)
                    ( 
                            stackCopy.get(stackCopy.size() - 1) 
                    );
                    
                    if (! nextPart.matches(stackTop))
                        break;
                    
                    stackCopy.remove(stackTop); 
                    
                    Edge newInstance = stackTop.getUnboundCopy();
                    try 
                    {
                        newInstance.bindTo(null, nextPart);
                    }
                    catch (AlreadyBoundException e)
                    {
                        throw new RuntimeException("Cannot bind the same " +
                            "instance twice", e);
                    }
                    matchCopy.contained.add(0, newInstance);
                }

                unhidePart(i);
            }
        }
    }

    void applySearch(int startPos, boolean hasMatched, int currentPartIndex,
            List stack, Match match, List out)
    {
        if (startPos < -1 || startPos >= stack.size())
        {
            throw new IllegalArgumentException("starting beyond end of stack");
        }
        
        RulePart nextPart = right[currentPartIndex];
    
        if (stack.size() > 0 && startPos == stack.size() - 1)
        {
            /*
            Instance i = (Instance)( stack.get(startPos) );
            if (! nextPart.matches(i))
            {
                // skip the remaining elements
                startPos = -1;
            }
            */
            
            // if the element cannot repeat, and we have already
            // matched in this route, we should not try to match again
            if (hasMatched && ! nextPart.canRepeat())
            {
                // skip the remaining elements
                startPos = -1;
            }
        }
        
        // search every path that reuses this rule part, with and without 
        // consuming each element on the stack that it matches.
        
        for (int stackPos = startPos; stackPos >= 0; stackPos--)
        {
            Edge i = (Edge)( stack.get(stackPos) );
            
            if (!nextPart.matches(i))
                continue;
            
            // first, without removing this element
            
            applySearch(stackPos - 1, hasMatched, currentPartIndex, 
                stack, match, out);
            
            // then remove it, and search again without it
            
            stack.remove(stackPos);
            
            {
                Edge newInstance = i.getUnboundCopy();
    
                try 
                {
                    newInstance.bindTo(null, nextPart);
                }
                catch (AlreadyBoundException e)
                {
                    throw new RuntimeException(e);
                }
                
                match.contained.add(0, newInstance);
                applySearch(stackPos - 1, true, currentPartIndex, 
                    stack, match, out);
            }
            
            stack.add(stackPos, i);
        }
        
        // now search all the paths where this rule part is finished
        // and will not be used again.
        
        // if the current rule part is required (not optional) and has
        // not yet been matched, then this search path is doomed to fail,
        // so we stop it now.

        if (!hasMatched && !nextPart.canSkip())
        {
            return;
        }
        
        // remove this part from the list of parts, 
        // and continue the search with the next one.
        
        hidePart(currentPartIndex);

        if (m_NumPartsRemaining > 0)
        {
            int nextPartIndex = -1;
            
            for (int i = right.length - 1; i >= 0; i--)
            {
                if (!m_PartsFinished[i])
                {
                    nextPartIndex = i;
                    break;
                }
            }
            
            if (nextPartIndex == -1)
            {
                throw new AssertionError("there must be a rule part " +
                        "left to use at this point");
            }
            
            applySearch(stack.size() - 1, false, nextPartIndex, 
                stack, match, out);
        }
        else
        {
            apply(stack, match, out);
        }

        unhidePart(currentPartIndex);
    }
    
    public List arrayToList(Object[] array)
    {
        Vector vector = new Vector();
        for (int i = 0; i < array.length; i++)
        {
            vector.add(array[i]);
        }
        return vector;
    }
    
    public List applyTo(Stack stack)
    {
        Match match = new Match(right.length);
        List out = new Vector();
        for (int i = 0; i < right.length; i++)
        {
            m_PartsFinished[i] = false;
        }
        m_NumPartsRemaining = right.length;
        apply(stack, match, out);
        return out;
    }
    
    public static class DuplicateNameException extends IllegalArgumentException
    {
        static final long serialVersionUID = 1;
        
        private String m_name, m_attempted;
        private RulePart m_previous;
        
        public DuplicateNameException(String name, RulePart previous, 
                String attempted)
        {
            super("Attempted to create a rule part with the same name "+
                    "("+name+") as an existing rule part "+
                    "("+previous.toString()+") with rule: "+attempted);
            m_name      = name;
            m_attempted = attempted;
            m_previous  = previous;
        }
    }
}
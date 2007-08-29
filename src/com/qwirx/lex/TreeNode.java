package com.qwirx.lex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class TreeNode implements List
{
    private List   m_children = new ArrayList();
    private String m_label;
    private String m_class;
    
    public TreeNode()
    {
        this(null, null);
    }

    public TreeNode(String label)
    {
        this(label, null);
    }

    public TreeNode(String label, String clazz)
    {
        m_label = label;
        m_class = clazz;
    }

    public String getLabel() { return m_label; }
    public String getClazz() { return m_class; }

    public List getChildren()   { return this; }
    public int  getChildCount() { return size(); }
    
    private void assertTrue(boolean condition) throws AssertionError
    {
    	if (!condition) throw new AssertionError();
    }

    public void add(int index, Object element)
    {
        TreeNode t = (TreeNode)element;
        m_children.add(index, t);
    }
    
    public boolean add(Object element)
    {
        TreeNode t = (TreeNode)element;
        return m_children.add(t);
    }
    
    public boolean addAll(Collection coll)
    {
        for (Iterator i = coll.iterator(); i.hasNext(); )
        {
            TreeNode t = (TreeNode)(i.next());
            assertTrue(t != null);
        }
        return m_children.addAll(coll);
    }
    
    public boolean addAll(int index, Collection coll)
    {
        for (Iterator i = coll.iterator(); i.hasNext(); )
        {
            TreeNode t = (TreeNode)( i.next() );
            assertTrue(t != null);
        }
        return m_children.addAll(index, coll);
    }       

    public void clear()
    {
        m_children.clear();
    }
    
    public boolean contains(Object o) 
    {
        return m_children.contains(o);
    }
    
    public boolean containsAll(Collection c)
    {
        return m_children.containsAll(c);
    }
    
    public boolean equals(Object other)
    {
        TreeNode otherNode = (TreeNode)other;
        if (! m_label.equals(otherNode.m_label))
            return false;
        return m_children.equals(otherNode.m_children);
    }
    
    public Object get(int index)
    {
        return (TreeNode)( m_children.get(index) );
    }
    
    public TreeNode getChild(int index)
    {
        return (TreeNode)( m_children.get(index) );
    }

    public int indexOf(Object o)
    {
        TreeNode t = (TreeNode)o;
        return m_children.indexOf(t);
    }
    
    public boolean isEmpty()
    {
        return m_children.isEmpty();
    }
    
    public Iterator iterator()
    {
        return m_children.iterator();
    }
    
    public int lastIndexOf(Object o)
    {
        TreeNode t = (TreeNode)o;
        return m_children.lastIndexOf(t);
    }
    
    public ListIterator listIterator()
    {
        return m_children.listIterator();
    }
    
    public ListIterator listIterator(int index)
    {
        return m_children.listIterator(index);
    }
    
    public Object remove(int index) 
    {
        return m_children.remove(index);
    }
    
    public boolean remove(Object o)
    {
        TreeNode t = (TreeNode)o;
        return m_children.remove(t);
    }
    
    public boolean removeAll(Collection coll)
    {
        for (Iterator i = coll.iterator(); i.hasNext(); )
        {
            TreeNode t = (TreeNode)( i.next() );
            assertTrue(t != null);
        }
        return m_children.removeAll(coll);
    }
    
    public boolean retainAll(Collection coll) 
    {
        for (Iterator i = coll.iterator(); i.hasNext(); )
        {
            TreeNode t = (TreeNode)( i.next() );
            assertTrue(t != null);
        }
        return m_children.retainAll(coll);
    }
    
    public Object set(int index, Object element)
    {
        TreeNode t = (TreeNode)element;
        return m_children.set(index, t);
    }
    
    public int size()
    {
        return m_children.size();
    }
    
    public List subList(int fromIndex, int toIndex)
    {
        return m_children.subList(fromIndex, toIndex);
    }
    
    private static final TreeNode [] treeNodeArray = new TreeNode [0];
    
    public Object[] toArray()
    {
        return m_children.toArray(treeNodeArray);
    }

    public Object[] toArray(Object[] a)
    {
        return m_children.toArray(a);
    }
    
    public int getWidth()
    {
        int width = 0;
        
        for (Iterator i = iterator(); i.hasNext(); )
        {
            TreeNode child = (TreeNode)( i.next() );
            width += child.getWidth();
        }
        
        if (width < 1) 
        {
            width = 1;
        }
        
        return width;
    }

    public int getDepth()
    {
        int depth = 0;
        
        for (Iterator i = iterator(); i.hasNext(); )
        {
            TreeNode child = (TreeNode)( i.next() );
            int childDepth = child.getDepth();
            if (depth < childDepth)
            {
                depth = childDepth;
            }
        }
        
        return depth + 1;
    }
    
    public String toHtml()
    {
        return toHtml(new TableRenderer());
    }
    
    public String toHtml(TableRenderer rend)
    {
        StringBuffer html = new StringBuffer();
        
        html.append(rend.getRow(getHtmlCell(rend)));

        List rowCells = m_children;
        
        while (rowCells != null && rowCells.size() > 0)
        {
            List nextRow = new ArrayList();
            boolean hasRealChildren = false;
            
            StringBuffer rowHtml = new StringBuffer();

            int maxDepth = 0;
            for (Iterator i = rowCells.iterator(); i.hasNext(); )
            {
                Object cell = i.next();
                
                if (cell instanceof String)
                {
                    // there must be at least one real node on this row,
                    // or we wouldn't be scanning it (and maxDepth will
                    // remain 0, and will be caught by assertion later)
                    continue;
                }
                
                TreeNode node = (TreeNode)cell;
                int nodeDepth = node.getDepth();
                
                if (maxDepth < nodeDepth)
                {
                    maxDepth = nodeDepth;
                }
            }
            assertTrue(maxDepth > 0);
            
            for (Iterator i = rowCells.iterator(); i.hasNext(); )
            {
                Object cell = i.next();
                if (cell instanceof String)
                {
                    rowHtml.append(cell);
                    continue;
                }
                
                TreeNode node = (TreeNode)cell;
                rowHtml.append(node.getHtmlCell(rend));

                if (node.getChildCount() > 0)
                {
                    nextRow.addAll(node.getChildren());
                    hasRealChildren = true;
                }
                else
                {
                    nextRow.add(node.getHtmlPlaceholderSpanningRows(
                            maxDepth - 1, rend));
                }
            }
            
            html.append(rend.getRow(rowHtml.toString()));
            
            if (hasRealChildren)
            {
                rowCells = nextRow;
            }
            else
            {
                rowCells = null; // end loop
            }
        }

        return rend.getTable(html.toString());
    }

    private String getHtmlCell(TableRenderer rend)
    {
        return rend.getCell(m_label, m_class, getWidth(), 1);
    }

    private String getHtmlPlaceholderSpanningRows(int depth, 
            TableRenderer rend)
    {
        return rend.getCell("", null, getWidth(), depth);
    }

    public TreeNode createChild(String label, String clazz)
    {
        TreeNode child = new TreeNode(label, clazz);
        this.add(child);
        return child;
    }

    public TreeNode createChild(String label)
    {
        return createChild(label, null);
    }
    
    public void setLabel(String label)
    {
        this.m_label = label;       
    }
}
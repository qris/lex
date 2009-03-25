package com.qwirx.lex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import junit.framework.TestCase;

public class TreeNodeTests extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TreeNodeTests.class);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.TreeNode()'
     */
    public final void testTreeNodeConstructor() 
    {
        TreeNode t = new TreeNode();
        assertNull("Wrong label", t.getLabel());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.getLabel()'
     */
    public final void testGetLabel() 
    {
        TreeNode t = new TreeNode("test");
        assertEquals("Wrong label", "test", t.getLabel());
    }

    TreeNode root;
    TreeNode child1;
    TreeNode child2;
    TreeNode child3;

    public void setUp()
    {
        root   = new TreeNode("root");
        child1 = new TreeNode("child1");
        child2 = new TreeNode("child2");
        child3 = new TreeNode("child3");
    }
    
    /*
     * Test method for 'com.qwirx.lex.TreeNode.add(int, Object)'
     */
    public final void testAddObjectWithIndex() 
    {
        root.add(0, child3);
        root.add(0, child1);
        root.add(1, child2);
        assertEquals(root.size(), 3);
        assertEquals(root.getChild(0), child1);
        assertEquals(root.getChild(1), child2);
        assertEquals(root.getChild(2), child3);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.add(Object)'
     */
    public final void testAddObject() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        assertEquals(root.size(), 3);
        assertEquals(root.getChild(0), child1);
        assertEquals(root.getChild(1), child2);
        assertEquals(root.getChild(2), child3);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.addAll(Collection)'
     */
    public final void testAddAllCollection() 
    {
        List coll = new ArrayList();
        coll.add(child1);
        coll.add(child2);
        root.addAll(coll);
        assertEquals(root.size(), 2);
        assertEquals(root.getChild(0), child1);
        assertEquals(root.getChild(1), child2);
        
        coll = new ArrayList();
        coll.add(child1);
        coll.add(child3);
        root.addAll(coll);
        assertEquals(root.size(), 4);
        assertEquals(root.getChild(0), child1);
        assertEquals(root.getChild(1), child2);
        assertEquals(root.getChild(2), child1);
        assertEquals(root.getChild(3), child3);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.addAll(int, Collection)'
     */
    public final void testAddAllIntCollection() 
    {
        List coll = new ArrayList();
        coll.add(child1);
        coll.add(child2);
        root.addAll(coll);
        assertEquals(root.size(), 2);
        assertEquals(root.getChild(0), child1);
        assertEquals(root.getChild(1), child2);
        
        coll = new ArrayList();
        coll.add(child1);
        coll.add(child3);
        root.addAll(1, coll);
        assertEquals(root.size(), 4);
        assertEquals(root.getChild(0), child1);
        assertEquals(root.getChild(1), child1);
        assertEquals(root.getChild(2), child3);
        assertEquals(root.getChild(3), child2);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.clear()'
     */
    public final void testClear() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        assertEquals(root.size(), 3);

        root.clear();
        assertEquals(root.size(), 0);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.contains(Object)'
     */
    public final void testContains() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        assertTrue(root.contains(child1));
        assertTrue(root.contains(child2));
        assertTrue(root.contains(child3));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.containsAll(Collection)'
     */
    public final void testContainsAll() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);

        List coll = new ArrayList();
        coll.add(child1);
        coll.add(child2);
        assertTrue(root.containsAll(coll));

        coll = new ArrayList();
        coll.add(child3);
        coll.add(child2);
        assertTrue(root.containsAll(coll));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.equals(Object)'
     */
    public final void testEqualsObject() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        TreeNode root2 = new TreeNode("root");
        root2.add(child1);
        root2.add(child2);
        root2.add(child3);        
        assertEquals(root, root2);

        root2 = new TreeNode("notroot");
        root2.add(child1);
        root2.add(child2);
        root2.add(child3);        
        assertFalse(root.equals(root2));

        root2 = new TreeNode("root");
        root2.add(child3);
        root2.add(child1);
        root2.add(child2);
        assertFalse(root.equals(root2));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.getChild(int)'
     */
    public final void testGetChild() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);

        assertEquals(root.get(0), root.getChild(0));
        assertEquals(root.get(1), root.getChild(1));
        assertEquals(root.get(2), root.getChild(2));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.indexOf(Object)'
     */
    public final void testIndexOf() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);

        assertEquals(0, root.indexOf(child1));
        assertEquals(1, root.indexOf(child2));
        assertEquals(2, root.indexOf(child3));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.isEmpty()'
     */
    public final void testIsEmpty() 
    {
        assertTrue(root.isEmpty());
        root.add(child1);
        assertFalse(root.isEmpty());
        root.remove(child2);
        assertFalse(root.isEmpty());
        root.remove(child1);
        assertTrue(root.isEmpty());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.iterator()'
     */
    public final void testIterator() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        Iterator i = root.iterator(); 
        assertTrue(i.hasNext());

        assertEquals(child1, i.next());
        assertTrue(i.hasNext());
        
        assertEquals(child2, i.next());
        assertTrue(i.hasNext());

        assertEquals(child3, i.next());
        assertFalse(i.hasNext());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.lastIndexOf(Object)'
     */
    public final void testLastIndexOf() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        root.add(child3);
        root.add(child2);
        root.add(child1);

        assertEquals(5, root.lastIndexOf(child1));
        assertEquals(4, root.lastIndexOf(child2));
        assertEquals(3, root.lastIndexOf(child3));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.listIterator()'
     */
    public final void testListIterator() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        ListIterator i = root.listIterator(); 
        assertTrue(i.hasNext());
        assertFalse(i.hasPrevious());

        assertEquals(child1, i.next());
        assertTrue(i.hasNext());
        assertTrue(i.hasPrevious());
        
        assertEquals(child2, i.next());
        assertTrue(i.hasNext());
        assertTrue(i.hasPrevious());
        
        assertEquals(child3, i.next());
        assertFalse(i.hasNext());
        assertTrue(i.hasPrevious());
        
        assertEquals(child3, i.previous());
        assertTrue(i.hasNext());
        assertTrue(i.hasPrevious());
        
        assertEquals(child2, i.previous());
        assertTrue(i.hasNext());
        assertTrue(i.hasPrevious());

        assertEquals(child1, i.previous());
        assertTrue(i.hasNext());
        assertFalse(i.hasPrevious());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.listIterator(int)'
     */
    public final void testListIteratorInt() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        ListIterator i = root.listIterator(1); 
        assertTrue(i.hasNext());
        assertTrue(i.hasPrevious());

        assertEquals(child1, i.previous());
        assertTrue(i.hasNext());
        assertFalse(i.hasPrevious());

        i = root.listIterator(1);
        assertEquals(child2, i.next());
        assertTrue(i.hasNext());
        assertTrue(i.hasPrevious());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.remove(int)'
     */
    public final void testRemoveInt() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        root.remove(1);
        assertEquals(2, root.size());
        assertEquals(child1, root.get(0));
        assertEquals(child3, root.get(1));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.remove(Object)'
     */
    public final void testRemoveObject() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        root.remove(child2);
        assertEquals(2, root.size());
        assertEquals(child1, root.get(0));
        assertEquals(child3, root.get(1));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.removeAll(Collection)'
     */
    public final void testRemoveAll() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        Collection coll = new HashSet();
        coll.add(child3);
        coll.add(child2);
        
        root.removeAll(coll);
        assertEquals(1, root.size());
        assertEquals(child1, root.get(0));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.retainAll(Collection)'
     */
    public final void testRetainAll() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        Collection coll = new HashSet();
        coll.add(child3);
        coll.add(child2);
        coll.add(new TreeNode("foo"));
        
        root.retainAll(coll);
        assertEquals(2, root.size());
        assertEquals(child2, root.get(0));
        assertEquals(child3, root.get(1));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.set(int, Object)'
     */
    public final void testSet() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        root.set(0, child3);
        root.set(2, child2);
        assertEquals(child3, root.get(0));
        assertEquals(child2, root.get(1));
        assertEquals(child2, root.get(2));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.size()'
     */
    public final void testSize() 
    {
        assertEquals(0, root.size());
        root.add(child1);
        assertEquals(1, root.size());
        root.add(child2);
        assertEquals(2, root.size());
        root.remove(child3);
        assertEquals(2, root.size());
        root.remove(child2);
        assertEquals(1, root.size());
        root.remove(child1);
        assertEquals(0, root.size());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.subList(int, int)'
     */
    public final void testSubList() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);

        List subList = root.subList(1, 1);
        assertEquals(0, subList.size());

        subList = root.subList(1, 2);
        assertEquals(1, subList.size());
        assertEquals(child2, subList.get(0));
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.toArray()'
     */
    public final void testToArray() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);

        TreeNode [] array = (TreeNode[])root.toArray();
        assertEquals(3, array.length);
        assertEquals(child1, array[0]);
        assertEquals(child2, array[1]);
        assertEquals(child3, array[2]);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.toArray(Object[])'
     */
    public final void testToArrayObjectArray() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);

        List [] array = (List[])root.toArray(new List[0]);
        assertEquals(3, array.length);
        assertEquals(child1, array[0]);
        assertEquals(child2, array[1]);
        assertEquals(child3, array[2]);
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.getWidth()'
     */
    public final void testGetWidthAndGetDepth() 
    {
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        child2.add(new TreeNode("child4"));
        child3.add(new TreeNode("child5"));
        child3.add(new TreeNode("child6"));

        assertEquals(1, child1.getWidth());
        assertEquals(1, child2.getWidth());
        assertEquals(2, child3.getWidth());
        assertEquals(4, root.getWidth());

        assertEquals(1, child1.getDepth());
        assertEquals(2, child2.getDepth());
        assertEquals(2, child3.getDepth());
        assertEquals(3, root.getDepth());
    }

    /*
     * Test method for 'com.qwirx.lex.TreeNode.toHtml()'
     */
    public final void testToHtml() 
    {
        assertEquals("<table>\n" +
                "  <tr>\n" +
                "    <td>root</td>\n" +
                "  </tr>\n" +
                "</table>\n", root.toHtml());

        root.add(child1);
        assertEquals("<table>\n" +
                "  <tr>\n" +
                "    <td>root</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child1</td>\n" +
                "  </tr>\n" +
                "</table>\n", root.toHtml());

        root.add(child2);
        assertEquals("<table>\n" +
                "  <tr>\n" +
                "    <td colspan=\"2\">root</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child1</td>\n" +
                "    <td>child2</td>\n" +
                "  </tr>\n" +
                "</table>\n", root.toHtml());

        root.add(child2);
        assertEquals("<table>\n" +
                "  <tr>\n" +
                "    <td colspan=\"3\">root</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child1</td>\n" +
                "    <td>child2</td>\n" +
                "    <td>child2</td>\n" +
                "  </tr>\n" +
                "</table>\n", root.toHtml());

        child1.add(child3);
        assertEquals("<table>\n" +
                "  <tr>\n" +
                "    <td colspan=\"3\">root</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child1</td>\n" +
                "    <td>child2</td>\n" +
                "    <td>child2</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child3</td>\n" +
                "    <td></td>\n" +
                "    <td></td>\n" +
                "  </tr>\n" +
                "</table>\n", root.toHtml());

        child3.add(new TreeNode("child4"));
        assertEquals("<table>\n" +
                "  <tr>\n" +
                "    <td colspan=\"3\">root</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child1</td>\n" +
                "    <td>child2</td>\n" +
                "    <td>child2</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child3</td>\n" +
                "    <td rowspan=\"2\"></td>\n" +
                "    <td rowspan=\"2\"></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <td>child4</td>\n" +
                "  </tr>\n" +
                "</table>\n", root.toHtml());

        /*
        root.add(child1);
        root.add(child2);
        root.add(child3);
        
        child2.add(new TreeNode("child4"));
        child3.add(new TreeNode("child5"));
        child3.add(new TreeNode("child6"));

        assertEquals(1, child1.getWidth());
        assertEquals(1, child2.getWidth());
        assertEquals(2, child3.getWidth());
        assertEquals(4, root.getWidth());

        assertEquals(1, child1.getDepth());
        assertEquals(2, child2.getDepth());
        assertEquals(2, child3.getDepth());
        assertEquals(3, root.getDepth());
        */
    }
    
    public void testCreateChild()
    {
        TreeNode child4 = root.createChild("child4");
        TreeNode child5 = root.createChild("child5");
        TreeNode child6 = root.createChild("child6");
        assertEquals(3, root.size());
        assertEquals(child4, root.get(0));
        assertEquals(child5, root.get(1));
        assertEquals(child6, root.get(2));
    }
}

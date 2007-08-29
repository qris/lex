package com.qwirx.lex;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.qwirx.lex.trees.PngEncoder;

public class TreeDrawingTest extends TestCase
{
    static class Node
    {
        private Node    m_Parent;
        private List    m_Children = new ArrayList();
        private boolean m_Rendered = false;
        private String  m_Label;
        public  int     x, y, w, h;
        
        private static BufferedImage s_Buffer;
        private static Graphics2D    s_Graphics;
        private static Font          s_Font;
        private static FontRenderContext s_FRC;
        private TextLayout m_Layout;

        private static final int MARGIN = 8, PADDING = 4;
        
        public Node(Node parent, String label)
        {
            this.m_Parent = parent;
            this.m_Label  = label;
            
            if (s_Buffer == null)
            {
                /*
                Font [] fonts = 
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
                for (int i = 0; i < fonts.length; i++)
                {
                    System.out.println(fonts[i].getFontName());
                }
                */

                s_Buffer = new BufferedImage(100, 100, 
                    BufferedImage.TYPE_4BYTE_ABGR);
                s_Graphics = s_Buffer.createGraphics();
                s_Font = new Font("SansSerif", Font.PLAIN, 12);
                s_Graphics.setFont(s_Font);
                s_FRC = s_Graphics.getFontRenderContext();
            }

            m_Layout = new TextLayout(m_Label, s_Font, s_FRC);
            Rectangle2D bounds = m_Layout.getBounds();
            // layout.draw(g, (float)loc.getX(), (float)loc.getY());
            this.w = (int)bounds.getWidth()  + PADDING*2;
            this.h = (int)bounds.getHeight() + PADDING*2;
        }
        
        public Node(String label)
        {
            this(null, label);
        }
        
        public Node add(Node child)
        {
            this.m_Children.add(child);
            return child;
        }
        
        /*
        public int getDepth()
        {
            int maxDepth = 0;
            for (Iterator i = m_Children.iterator(); i.hasNext();)
            {
                Node child = (Node)i.next();
                int depth = child.getDepth();
                if (maxDepth < depth)
                {
                    maxDepth = depth;
                }
            }
            return maxDepth;
        }
        */
        
        public int getWidth()
        {
            int width = 0;

            for (Iterator i = m_Children.iterator(); i.hasNext();)
            {
                Node child = (Node)i.next();
                width += child.getWidth() + MARGIN;
            }
            
            if (width < this.w)
            {
                width = this.w;
            }
            
            return width;
        }
        
        public int getHeight()
        {
            int maxHeight = 0;
            
            for (Iterator i = m_Children.iterator(); i.hasNext();)
            {
                Node child = (Node)i.next();
                int childHeight = child.getHeight();
                if (maxHeight < childHeight)
                {
                    maxHeight = childHeight;
                }
            }
            
            return this.h + MARGIN + maxHeight;
        }
        
        private void layout(int originX, int originY)
        {
            this.x = originX;
            this.y = originY;
            int x = originX;
            
            this.x = originX + ((getWidth() - w) / 2);
            x = originX;
            
            for (Iterator i = m_Children.iterator(); i.hasNext();)
            {
                Node child = (Node)i.next();
                child.layout(x, originY + this.h + MARGIN);
                x += child.getWidth() + MARGIN;
            }
        }
        
        public void render()
        {
            layout(MARGIN, MARGIN);
        }
        
        public void draw(Graphics2D graphics)
        {
            graphics.setFont(s_Font);
            m_Layout.draw(graphics, this.x + PADDING, 
                this.y + m_Layout.getAscent() /*+PADDING*/);
            graphics.drawRect(this.x, this.y, this.w, this.h);

            for (Iterator i = m_Children.iterator(); i.hasNext();)
            {
                Node child = (Node)i.next();
                child.draw(graphics);
            }
        }
    }
    
    public void testDrawSimpleTree1()
    {
        Node root = new Node("root");
        root.render();
        assertEquals(8, root.x);
        assertEquals(8, root.y);
        assertEquals(29, root.w);
        assertEquals(15, root.h);
    }

    public void testDrawSimpleTree2() throws Exception
    {
        Node root = new Node("root");
        Node a = root.add(new Node("a"));
        Node b = root.add(new Node("b"));
        Node c = root.add(new Node("c"));
        root.render();
        assertEquals(24, root.x);
        assertEquals(8, root.y);
        assertEquals(29, root.w);
        assertEquals(15, root.h);
        assertEquals(8, a.x);
        assertEquals(31, a.y);
        assertEquals(29, b.x);
        assertEquals(31, b.y);
        assertEquals(50, c.x);
        assertEquals(31, c.y);
        
        assertEquals(62, root.getWidth());
        assertEquals(48, root.getHeight());
        
        BufferedImage buf = new BufferedImage(root.getWidth(), root.getHeight(), 
            BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = buf.createGraphics();
        root.draw(graphics);
        
        PngEncoder enc = new PngEncoder();
        enc.setImage(buf);
        byte [] imgdata = enc.pngEncode();
        
        FileOutputStream fos = new FileOutputStream("foo.png");
        fos.write(imgdata);
        fos.close();
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(TreeDrawingTest.class);
    }
}

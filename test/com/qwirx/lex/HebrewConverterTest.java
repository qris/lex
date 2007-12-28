package com.qwirx.lex;

import com.qwirx.lex.hebrew.HebrewConverter;

import junit.framework.TestCase;

public class HebrewConverterTest extends TestCase
{
    public void testToTranslit()
    {
        assertEquals("nākərîî", HebrewConverter.toTranslit("N@K:RIJ."));
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HebrewConverterTest.class);
    }

}

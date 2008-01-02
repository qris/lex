package com.qwirx.lex;

import com.qwirx.lex.hebrew.HebrewConverter;

import junit.framework.TestCase;

public class HebrewConverterTest extends TestCase
{
    public void testToTranslit()
    {
        assertEquals("nākərîî", HebrewConverter.toTranslit("N@K:RIJ."));
        assertEquals("jiśśāskār", HebrewConverter.toTranslit("JIF.@#K@R"));
        assertEquals("rē?šî", HebrewConverter.toTranslit("R;>CI73J"));
        assertEquals("îm", HebrewConverter.toTranslit("I92Jm"));
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HebrewConverterTest.class);
    }

}

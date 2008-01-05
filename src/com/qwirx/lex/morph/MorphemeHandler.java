package com.qwirx.lex.morph;

import jemdros.EmdrosException;

public interface MorphemeHandler
{
    public void convert(String surface, boolean lastMorpheme, String desc,
        String morphNode)
    throws EmdrosException;
}

package com.qwirx.lex.morph;

public interface MorphemeHandler
{
    public void convert(String surface, 
        boolean lastMorpheme, String desc,
        String morphNode);
}

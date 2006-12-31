package com.qwirx.lex;

public class Aktionsart
{
    private String m_label;
    private Aktionsart(String label) { this.m_label = label; }
    public static final Aktionsart
        NONE   = new Aktionsart("NONE"),
        INGR   = new Aktionsart("INGR"),
        SEML   = new Aktionsart("SEML"),
        BECOME = new Aktionsart("BECOME");
    public String toString() { return m_label; }
    public static Aktionsart get(String label)
    {
        if (label == null)
            return null;
        if (label.equals("NONE"))
            return NONE;
        else if (label.equals("INGR"))
            return INGR;
        else if (label.equals("SEML"))
            return SEML;
        else if (label.equals("BECOME"))
            return BECOME;
        return null;
    }
}

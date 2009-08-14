package com.qwirx.lex.test.unfinished;

import junit.framework.TestCase;

public class HebrewMorphTest extends TestCase
{
    /* Incomplete! */
    
    static class Verb
    {
        enum Person { P1, P2, P3 }
        enum Number { SG, PL }
        enum Gender { MASC, FEM, UNKNOWN }
        
        private Person m_Person;
        private Number m_Number;
        private Gender m_Gender;
        
        private class Pattern
        {
            private String m_Pattern;
            private Person m_Person;
            private Number m_Number;
            private Gender m_Gender;
            public Pattern(String pattern, Person person, Number number,
                Gender gender)
            {
                m_Pattern = pattern;
                m_Person = person;
                m_Number = number;
                m_Gender = gender;
            }
            public Person getPerson() { return m_Person; }
            public Number getNumber() { return m_Number; }
            public Gender getGender() { return m_Gender; }
        }
        
        private final Pattern[] m_Patterns = {
            new Pattern("כָּֽתְב ָה", Person.P3, Number.SG, Gender.FEM),
            new Pattern("כָּתַבְתָּ",   Person.P2, Number.SG, Gender.MASC),
            new Pattern("כָּתַבְתְּ",   Person.P2, Number.SG, Gender.FEM),
            new Pattern("כָּתַבְתִּי",  Person.P1, Number.SG, Gender.UNKNOWN),
            new Pattern("כָּֽתְבוּ",   Person.P3, Number.PL, Gender.UNKNOWN),
            new Pattern("כְּתַבְתֶּם",  Person.P2, Number.PL, Gender.MASC),
            new Pattern("כְּתַבְתֶּן",  Person.P2, Number.PL, Gender.FEM),
            new Pattern("כָּתַבְנוּ",  Person.P1, Number.PL, Gender.UNKNOWN),
            new Pattern("",       Person.P3, Number.SG, Gender.MASC),
        };
        
        public Verb(String text)
        {
            for (int i = 0; i < m_Patterns.length; i++)
            {
                Pattern pat = m_Patterns[i];
                
            }
            if (text.endsWith("ה ָ"))
            {
                m_Person = Person.P3;
                m_Number = Number.SG;
                m_Gender = Gender.MASC;
            }
            
        }
        
        public Person getPerson() { return m_Person; }
        public Number getNumber() { return m_Number; }
        public Gender getGender() { return m_Gender; }
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HebrewMorphTest.class);
    }

}

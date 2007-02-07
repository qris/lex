package com.qwirx.lex.lexicon;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public final class ThematicRelation
{
    public static final class Arguments
    {
        private String m_name;
        private String[] m_argNames;
        private Arguments(String name, String[] args)
        {
            this.m_name = name;
            this.m_argNames = args; 
        }
        public String toString()
        {
            return m_name;
        }
        public String[] getArgNames()
        {
            String[] ret = new String [m_argNames.length];
            System.arraycopy(m_argNames, 0, ret, 0, m_argNames.length);
            return ret;
        }
        public static final Arguments
            X = new Arguments("X", new String[]{"x"}),
            Y = new Arguments("Y", new String[]{"y"}),
            XY = new Arguments("XY", new String[]{"x","y"}),
            YX = new Arguments("YX", new String[]{"y","x"}),
            ZX = new Arguments("ZX", new String[]{"z","x"});
        private static Arguments[] m_argTypes = new Arguments[]{X,XY,ZX};
        public static final Arguments[] list() 
        {
            Arguments[] ret = new Arguments [m_argTypes.length];
            System.arraycopy(m_argTypes, 0, ret, 0, m_argTypes.length);
            return ret;
        }
    }
    
    public static final class Theme
    {
        private String m_name;
        private Theme(String name) { m_name = name; }
        public String toString() { return m_name; }
        public static final Theme 
        AGENT = new Theme("AGENT"),
        EFFECTOR = new Theme("EFFECTOR"),
        MOVER = new Theme("MOVER"),
        ST_MOVER = new Theme("ST-MOVER"),
        EMITTER = new Theme("EMITTER"),
        PERFORMER = new Theme("PERFORMER"),
        CONSUMER = new Theme("CONSUMER"),
        CREATOR = new Theme("CREATOR"),
        DESTROYER = new Theme("DESTROYER"),
        SPEAKER = new Theme("SPEAKER"),
        OBSERVER = new Theme("OBSERVER"),
        USER = new Theme("USER"),
        LOCATION = new Theme("LOCATION"),
        PERCEIVER = new Theme("PERCEIVER"),
        COGNIZER = new Theme("COGNIZER"),
        WANTER = new Theme("WANTER"),
        JUDGER = new Theme("JUDGER"),
        POSSESSOR = new Theme("POSSESSOR"),
        EXPERIENCER = new Theme("EXPERIENCER"),
        EMOTER = new Theme("EMOTER"),
        ATTRIBUTANT = new Theme("ATTRIBUTANT"),
        IDENTIFIED = new Theme("IDENTIFIED"),
        VARIABLE = new Theme("VARIABLE"),
        THEME = new Theme("THEME"),
        STIMULUS = new Theme("STIMULUS"),
        CONTENT = new Theme("CONTENT"),
        DESIRE = new Theme("DESIRE"),
        JUDGMENT = new Theme("JUDGMENT"),
        POSSESSED = new Theme("POSSESSED"),
        SENSATION = new Theme("SENSATION"),
        TARGET = new Theme("TARGET"),
        ATTRIBUTE = new Theme("ATTRIBUTE"),
        IDENTITY = new Theme("IDENTITY"),
        VALUE = new Theme("VALUE"),
        PERFORMANCE = new Theme("PERFORMANCE"),
        CONSUMED = new Theme("CONSUMED"),
        CREATION = new Theme("CREATION"),
        LOCUS = new Theme("LOCUS"),
        IMPLEMENT = new Theme("IMPLEMENT"),
        PATIENT = new Theme("PATIENT"),
        ENTITY = new Theme("ENTITY"),
        REFERENT = new Theme("REFERENT");
    }
    
    private String m_label, m_prompt, m_example;
    private Arguments m_args;
    private Theme[] m_themes;
    
    /**
     * Should not be public! Only for use by unit tests!
     * @param label
     * @param prompt
     * @param example
     * @param args
     * @param theme1
     */
    public ThematicRelation(String label, String prompt, String example, 
        Arguments args, Theme theme1) 
    { 
        this.m_label   = label;
        this.m_prompt  = prompt;
        this.m_example = example;
        this.m_args    = args;
        this.m_themes  = new Theme[]{theme1};
    }

    /**
     * Should not be public! Only for use by unit tests!
     * @param label
     * @param prompt
     * @param example
     * @param args
     * @param theme1
     * @param theme2
     */
    public ThematicRelation(String label, String prompt, String example, 
        Arguments args, Theme theme1, Theme theme2) 
    { 
        this.m_label   = label;
        this.m_prompt  = prompt;
        this.m_example = example;
        this.m_args    = args;
        this.m_themes  = new Theme[]{theme1, theme2};
    }
    public String    toString()   { return m_label; }
    public String    getLabel()   { return m_label; }
    public String    getPrompt()  { return m_prompt; }
    public String    getExample() { return m_example; }
    public Arguments getArgs()    { return m_args; }
    
    public Theme[]   getThemes()  
    {
        Theme[] themesCopy = new Theme [m_themes.length];
        System.arraycopy(m_themes, 0, themesCopy, 0, m_themes.length);
        return themesCopy; 
    }
    
    public String    getArgText()
    {
        String [] args = m_args.getArgNames();
        String out = "(";
        for (int i = 0; i < args.length; i++)
        {
            out += "<" + args[i] + ">:" + m_themes[i].toString();
            if (i < args.length - 1)
            {
                out += ", ";
            }
        }
        out += ")"; 
        return out;
    }
    
    private static final ThematicRelation[] m_relations = 
        new ThematicRelation[] {
        new ThematicRelation("STA-ind", "<x> is",            "broken", 
            Arguments.X, Theme.PATIENT),
        new ThematicRelation("STA-xst", "<x> exists",        "exist",  
            Arguments.X, Theme.ENTITY),
        new ThematicRelation("STA-loc", "<x> is at <y>",       "be-LOC", 
            Arguments.XY, Theme.LOCATION, Theme.THEME),
        new ThematicRelation("STA-per", "<x> perceives <y>",   "hear",   
            Arguments.XY, Theme.PERCEIVER, Theme.STIMULUS),
        new ThematicRelation("STA-cog", "<x> cognizes <y>",    "know",   
            Arguments.XY, Theme.COGNIZER, Theme.CONTENT),
        new ThematicRelation("STA-des", "<x> desires <y>",     "want",   
            Arguments.XY, Theme.WANTER, Theme.DESIRE),
        new ThematicRelation("STA-con", "<x> considers <y>",   "consider", 
            Arguments.XY, Theme.JUDGER, Theme.JUDGMENT),
        new ThematicRelation("STA-pos", "<x> possesses <y>",   "have",   
            Arguments.XY, Theme.POSSESSOR, Theme.POSSESSED),
        new ThematicRelation("STA-exp", "<x> experiences <y>", "feel",   
            Arguments.XY, Theme.EXPERIENCER, Theme.SENSATION),
        new ThematicRelation("STA-emo", "<x> feels for <y>",   "love",   
            Arguments.XY, Theme.EMOTER, Theme.TARGET),
        new ThematicRelation("STA-att", "<x> has attribute <y>", "be-ATT", 
            Arguments.XY, Theme.ATTRIBUTANT, Theme.ATTRIBUTE),
        new ThematicRelation("STA-idn", "<x> has identity <y>", "be-ID", 
            Arguments.XY, Theme.IDENTIFIED, Theme.IDENTITY),
        new ThematicRelation("STA-val", "<x> has value <y>",   "be-SPEC", 
            Arguments.XY, Theme.VARIABLE, Theme.VALUE),
        new ThematicRelation("STA-equ", "<x> is identical to <y>", "equate", 
            Arguments.XY, Theme.REFERENT, Theme.REFERENT),
        new ThematicRelation("ACT-unsp", "<x> does something unspecified", 
            "&Oslash;", Arguments.X, Theme.EFFECTOR),
        new ThematicRelation("ACT-move", "<x> moves",        "walk",   
            Arguments.X, Theme.MOVER),
        new ThematicRelation("ACT-emit", "<x> emits something", "shine", 
            Arguments.X, Theme.EMITTER),
        new ThematicRelation("ACT-perf", "<x> performs <y>",   "sing",   
            Arguments.XY, Theme.PERFORMER, Theme.PERFORMANCE),
        new ThematicRelation("ACT-cons", "<x> consumes <y>",   "eat",    
            Arguments.XY, Theme.CONSUMER, Theme.CONSUMED),
        new ThematicRelation("ACT-crea", "<x> creates <y>",    "write",  
            Arguments.XY, Theme.CREATOR, Theme.CREATION),
        new ThematicRelation("ACT-dest", "<x> destroys <y>",   "kill", 
            Arguments.XY, Theme.DESTROYER, Theme.PATIENT),
        new ThematicRelation("ACT-dirp", "<x> perceives <y>",  "hear",   
            Arguments.XY, Theme.OBSERVER, Theme.STIMULUS),
        new ThematicRelation("ACT-uses", "<x> uses <y>",       "use",    
            Arguments.XY, Theme.USER, Theme.IMPLEMENT),
    };
    
    private static final Map m_relationNameMap = new Hashtable();
    
    static
    {
        /*
        List relations = new ArrayList();
        
        for (int i = 0; i < m_relationsBase.length; i++)
        {
            ThematicRelation rel = m_relationsBase[i];

            if (rel.getArgs() == Arguments.X)
            {
                relations.add(new ThematicRelation(
                    rel.getLabel() + "-x",
                    rel.getPrompt(),
                    rel.getExample(),
                    Arguments.X,
                    rel.getThemes()[0]
                    ));
                relations.add(new ThematicRelation(
                    rel.getLabel() + "-y",
                    rel.getPrompt().replaceAll("<x>","<y>"),
                    rel.getExample(),
                    Arguments.Y,
                    rel.getThemes()[0]
                    ));
            }
            else if (rel.getArgs() == Arguments.XY)
            {
                relations.add(new ThematicRelation(
                    rel.getLabel() + "-xy",
                    rel.getPrompt(),
                    rel.getExample(),
                    Arguments.XY,
                    rel.getThemes()[0],
                    rel.getThemes()[1]
                    ));
                relations.add(new ThematicRelation(
                    rel.getLabel() + "-yx",
                    rel.getPrompt()
                    .replaceAll("<x>","<_x_>")
                    .replaceAll("<y>", "<x>").replaceAll("<_x_>", "<y>"),
                    rel.getExample(),
                    Arguments.YX,
                    rel.getThemes()[0],
                    rel.getThemes()[1]
                    ));
            }
            else
            {
                relations.add(rel);
            }
        }
        
        m_relations = new ThematicRelation [relations.size()];
        
        for (int i = 0; i < relations.size(); i++)
        {
            m_relations[i] = (ThematicRelation)relations.get(i);
        }
        */
        
        for (int i = 0; i < m_relations.length; i++)
        {
            m_relationNameMap.put(m_relations[i].m_label, m_relations[i]);
        }
    }
    
    public static ThematicRelation[] list() 
    {
        ThematicRelation[] ret = new ThematicRelation [m_relations.length];
        System.arraycopy(m_relations, 0, ret, 0, m_relations.length);
        return ret;
    }
    
    public static ThematicRelation get(String name)
    {
        if (name == null) return null;
        return (ThematicRelation)m_relationNameMap.get(name);
    }
}

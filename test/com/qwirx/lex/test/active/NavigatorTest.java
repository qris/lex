package com.qwirx.lex.test.active;

import jemdros.SetOfMonads;

import com.qwirx.lex.controller.Navigator;
import com.qwirx.lex.test.base.LexTestBase;
import com.qwirx.lex.translit.DatabaseTransliterator;

public class NavigatorTest extends LexTestBase
{
    public NavigatorTest() throws Exception { }
    
    public void testClauseList() throws Exception
    {
        SetOfMonads userTextAccessSet = getEmdros().getVisibleMonads();
        Navigator navigator = new Navigator(null, null, getEmdros(), 
            userTextAccessSet, new DatabaseTransliterator(getSql()));
        /*
        assertEquals("<select id=\"book\" name=\"book\" " +
                "onChange=\"document.forms.nav.submit()\">\n" +
                "<option value=\"1\" selected=\"selected\">Genesis</option>\n" +
                "<option value=\"98054\">Exodus</option>\n" +
                "<option value=\"228327\">Numbers</option>\n" +
                "<option value=\"521677\">II_Samuel</option>\n" +
                "</select>\n",
        */ 
        // don't know what books should be available, but need to call
        // this method to initialise the next step
        navigator.getObjectNavigator("book", "book");
        assertEquals("<select id=\"chapter\" name=\"chapter\" " +
                "onChange=\"document.forms.nav.submit()\">\n" +
                "<option value=\"93473\" selected=\"selected\">1</option>\n" +
                "<option value=\"93474\">2</option>\n" +
                "<option value=\"93475\">3</option>\n" +
                "</select>\n", 
                navigator.getObjectNavigator("chapter", "chapter"));
        assertEquals("<select id=\"verse\" name=\"verse\" " +
                "onChange=\"document.forms.nav.submit()\">\n" +
                "<option value=\"93523\" selected=\"selected\"> " +
                "GEN 01,01</option>\n" +
                "<option value=\"93524\"> GEN 01,02</option>\n" +
                "<option value=\"93525\"> GEN 01,03</option>\n" +
                "<option value=\"93526\"> GEN 01,04</option>\n" +
                "<option value=\"93527\"> GEN 01,05</option>\n" +
                "<option value=\"93528\"> GEN 01,06</option>\n" +
                "<option value=\"93529\"> GEN 01,07</option>\n" +
                "<option value=\"93530\"> GEN 01,08</option>\n" +
                "<option value=\"93531\"> GEN 01,09</option>\n" +
                "<option value=\"93532\"> GEN 01,10</option>\n" +
                "<option value=\"93533\"> GEN 01,11</option>\n" +
                "<option value=\"93534\"> GEN 01,12</option>\n" +
                "<option value=\"93535\"> GEN 01,13</option>\n" +
                "<option value=\"93536\"> GEN 01,14</option>\n" +
                "<option value=\"93537\"> GEN 01,15</option>\n" +
                "<option value=\"93538\"> GEN 01,16</option>\n" +
                "<option value=\"93539\"> GEN 01,17</option>\n" +
                "<option value=\"93540\"> GEN 01,18</option>\n" +
                "<option value=\"93541\"> GEN 01,19</option>\n" +
                "<option value=\"93542\"> GEN 01,20</option>\n" +
                "<option value=\"93543\"> GEN 01,21</option>\n" +
                "<option value=\"93544\"> GEN 01,22</option>\n" +
                "<option value=\"93545\"> GEN 01,23</option>\n" +
                "<option value=\"93546\"> GEN 01,24</option>\n" +
                "<option value=\"93547\"> GEN 01,25</option>\n" +
                "<option value=\"93548\"> GEN 01,26</option>\n" +
                "<option value=\"93549\"> GEN 01,27</option>\n" +
                "<option value=\"93550\"> GEN 01,28</option>\n" +
                "<option value=\"93551\"> GEN 01,29</option>\n" +
                "<option value=\"93552\"> GEN 01,30</option>\n" +
                "<option value=\"93553\"> GEN 01,31</option>\n" +
                "</select>\n",
                navigator.getObjectNavigator("verse",
                    new String[]{"verse_label", "verse"}));
            assertEquals("<select id=\"clause\" name=\"clause\" " +
                "onChange=\"document.forms.nav.submit()\" class=\"translit\">" +
                "\n" +
                "<option value=\"28737\" selected=\"selected\">" +
                "bᵊrēˀšît bārāˀ ʔᵉlōhîm ʔēt haššāmayim " +
                "wᵊʔēt hāʔāreṣ</option>" +
                "\n" +
                "</select>\n",
                navigator.getClauseNavigator());
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(NavigatorTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}

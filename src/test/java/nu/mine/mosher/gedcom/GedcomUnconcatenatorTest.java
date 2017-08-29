package nu.mine.mosher.gedcom;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GedcomUnconcatenatorTest {
    private void v(final String src, final int maxLen, final List<String> expected) {
        final List<String> actual = new ArrayList<>(8);
        GedcomUnconcatenator.splitLineIntoSegments(src, maxLen, actual);

        assertThat(actual.size(), is(equalTo(expected.size())));
        for (int i = 0; i < actual.size(); ++i) {
            final String atLine = "at line "+Integer.toString(i);

            final String act = actual.get(i);
            assertThat(atLine, act, is(equalTo(expected.get(i))));

            // trailing whitespace is OK if and only if the entire
            // line is whitespaces, or it is the last line
            if (!(act.trim().isEmpty() || i >= actual.size()-1)) {
                final String actLastChar = act.substring(act.length() - 1);
                assertThat(atLine, actLastChar, is(not(equalTo(" "))));
            }
        }
    }

    @Test
    void infiniteLoopPrevention() {
        assertThrows(AssertionError.class, () -> {
            GedcomUnconcatenator.splitLineIntoSegments(new String(new char[100001]), 1, new ArrayList<String>(100001));
        });
    }

    @Test
    void invalidMaxLen() {
        assertThrows(AssertionError.class, () -> {
            GedcomUnconcatenator.splitLineIntoSegments("", 0, asList());
        });
    }

    @Test
    void nullResultsList() {
        assertThrows(AssertionError.class, () -> {
            GedcomUnconcatenator.splitLineIntoSegments("", 1, null);
        });
    }

    @Test
    void empty() {
        v("", 10, asList(""));
    }

    @Test
    void len1() {
        v("x", 10, asList("x"));
    }

    @Test
    void len2() {
        v("xy", 10, asList("xy"));
    }

    @Test
    void lenSameAsMaxLen() {
        v("xyz", 3, asList("xyz"));
    }

    @Test
    void lines2() {
        v("0123456789", 5, asList("01234", "56789"));
    }

    @Test
    void max1lines2() {
        v("01", 1, asList("0", "1"));
    }

    @Test
    void max1lines3() {
        v("012", 1, asList("0", "1", "2"));
    }

    @Test
    void lines2short() {
        v("01234567", 5, asList("01234", "567"));
    }

    @Test
    void wordNominal() {
        v("01 23 45 67", 6, asList("01 2", "3 45 6", "7"));
    }

    @Test
    void wordBackupMultipleCharacters() {
        v("01    23 45 67", 6, asList("0", "1    2", "3 45 6", "7"));
    }

    @Test
    void cannotBreakWIthinWord() {
        v("x     012345", 6, asList("x", "     0", "12345"));
    }

    @Test
    void cannotBreakOneLetterWords() {
        v("0 1 2 3 4 5 6 7 8", 6, asList("0 1 2", " 3 4 5", " 6 7 8"));
    }

    @Test
    void tooManySpaces2() {
        v("6     6     ", 3, asList("6", "   ", "  6", "   ", "  "));
    }

    @Test
    void tooManySpaces1() {
        v("6     6    ", 3, asList("6", "   ", "  6", "   ", " "));
    }

    @Test
    void tooManySpaces0() {
        v("6     6   ", 3, asList("6", "   ", "  6", "   " ));
    }

    @Test
    void tooManySpaces() {
        v("6     6", 3, asList("6", "   ", "  6" ));
    }

    @Test
    void tooManySpacesEnd2() {
        v("6     ", 3, asList("6", "   ", "  " ));
    }

    @Test
    void tooManySpacesEnd1() {
        v("6    ", 3, asList("6", "   ", " " ));
    }

    @Test
    void tooManySpacesEnd0() {
        v("6   ", 3, asList("6", "   "));
    }

    @Test
    void mustEndWithSpace() {
        v("z ", 3, asList("z "));
    }

    @Test
    void mustEndWithSpace2() {
        v("01  2  ", 3, asList("0", "1", "  2", "  "));
    }

    @Test
    void realWorldExample() {
        v(
                "From ancestry.com: source \"New York: Report of the Adjut" +
                        "ant-General. (NYRoster) Published in 1894-1906\" enli" +
                        "sted 28 Oct 1861 in Elmira, NY as Private age 24. Enl" +
                        "isted in Company H, 10th Cavalry Regiment New York, 3" +
                        "1 Oct 1861 (mustered in for 3 years). Mustered out 3 " +
                        "Nov 1864 in Petersburg, VA.",
                120,
                asList(
                        "From ancestry.com: source \"New York: Report of the Adjutant-General. (NYRoster) Published in 1894-1906\" enlisted 28 Oc",
                        "t 1861 in Elmira, NY as Private age 24. Enlisted in Company H, 10th Cavalry Regiment New York, 31 Oct 1861 (mustered i",
                        "n for 3 years). Mustered out 3 Nov 1864 in Petersburg, VA."
                ));
    }
}

package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GedcomDataRefTest {
    @Test
    public void nominal() throws GedcomDataRef.InvalidSyntax {
        final GedcomDataRef uut = new GedcomDataRef(".INDI.REFN");
        assertTrue(uut.matches(0, node("INDI")));
        assertTrue(uut.matches(1, node("REFN")));
    }

    @Test
    public void syntaxError() throws GedcomDataRef.InvalidSyntax {
        assertThrows(GedcomDataRef.InvalidSyntax.class, () -> {
            final GedcomDataRef uut = new GedcomDataRef(".INDI..");
        });
    }

    @Test
    public void nominalValue() throws GedcomDataRef.InvalidSyntax {
        final GedcomDataRef uut = new GedcomDataRef(".INDI\"[A-Z].*\"");
        assertTrue(uut.matches(0, node("INDI", "ABC")));
    }

    @Test
    public void nonMatchValue() throws GedcomDataRef.InvalidSyntax {
        final GedcomDataRef uut = new GedcomDataRef(".INDI\"[0-9].*\"");
        assertFalse(uut.matches(0, node("INDI", "ABC")));
    }

    private static TreeNode<GedcomLine> node(final String tag) {
        return node(tag, "");
    }

    private static TreeNode<GedcomLine> node(final String tag, final String value) {
        return new TreeNode<>(GedcomLine.createUser(0,tag, value));
    }
}

package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
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
        return new TreeNode<>(GedcomLine.createUser(0, tag, value));
    }

    @Test
    public void forEach() throws IOException, InvalidLevel, GedcomDataRef.InvalidSyntax {
        final GedcomTree t = Gedcom.valueOf("0 HEAD\n" +
            "0 @I2338@ INDI\n" +
            "1 _XY 73522 950\n" +
            "1 REFN 503db42b-8faa-45f5-9403-306dd9d78f63\n" +
            "1 RIN I2338\n" +
            "1 NAME Charles W. /Cranson/\n" +
            "2 SOUR @S-338823707@\n" +
            "3 _APID 1,7163::33642833\n" +
            "3 PAGE Year: 1870; Census Place: Georgetown, Madison, New York; Roll: M593_967; Page: 194A; Image: 392; Fam\n" +
            "4 CONC ily History Library Film: 552466\n" +
            "3 OBJE @pJwQDhWhTvWxcU4wZD81@\n" +
            "2 SOUR @S-340166898@\n" +
            "3 _APID 1,6742::38724707\n" +
            "3 PAGE Year: 1880; Census Place: Georgetown, Madison, New York; Roll: 860; Family History Film: 1254860; Pa\n" +
            "4 CONC ge: 149C; Enumeration District: 057; Image: 0147\n" +
            "3 OBJE @Ix5pZJQsSvWRUVZS23wU@\n" +
            "2 SOUR @S-301664500@\n" +
            "3 _APID 1,8800::2358507\n" +
            "3 PAGE Letters of Administration, 1830-1903, and Index, 1806-1890, Madison County, New York; Author: New Yo\n" +
            "4 CONC rk. Surrogate's Court (Madison County); Probate Place: Madison, New York\n" +
            "3 OBJE @R82VXyVgRsmuaUmr19i6@\n" +
            "0 @I0@ INDI\n" +
            "1 NAME Q. A. /Tester/\n" +
            "2 SOUR @S0@\n" +
            "3 _APID 0,0,0\n" +
            "0 TRLR\n");
        new GedcomConcatenator(t).concatenate();

        final GedcomDataRef uut = new GedcomDataRef(".INDI.NAME\"Charles.*\".SOUR._APID");
        final List<String> expected = asList("3 _APID 1,7163::33642833", "3 _APID 1,6742::38724707", "3 _APID 1,8800::2358507");

        final List<String> actual = new ArrayList<>(3);
        uut.forEach(t, n -> actual.add(n.toString()));

        assertArrayEquals(expected.toArray(), actual.toArray());
    }
}

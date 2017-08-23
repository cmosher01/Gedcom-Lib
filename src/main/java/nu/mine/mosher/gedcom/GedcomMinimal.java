package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static nu.mine.mosher.logging.Jul.log;

/*
 * 0 HEAD
 * 1 CHAR UTF-8
 * 1 GEDC
 * 2 VERS 5.5.1
 * 2 FORM LINEAGE-LINKED
 * 1 SOUR MINIMAL
 * 1 SUBM @M0@
 * 0 @M0@ SUBM
 * 1 NAME MINIMAL
 * 0 TRLR
 */
public class GedcomMinimal {
    private static final String SUMB_ID = "M0";

    public static GedcomTree minimal(Charset charsetForce) {
        final GedcomTree tree = new GedcomTree();

        final TreeNode<GedcomLine> head = new TreeNode<>(GedcomLine.createHeader());
        tree.getRoot().addChild(head);

        head.addChild(new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.CHAR)));
        tree.setCharset(Charset.forName("UTF-8"));

        final TreeNode<GedcomLine> gedc = new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.GEDC));
        head.addChild(gedc);
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.VERS, "5.5.1")));
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.FORM, "LINEAGE-LINKED")));

        head.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.SOUR, "MINIMAL")));
        head.addChild(new TreeNode<>(GedcomLine.createPointer(1, GedcomTag.SUBM, SUMB_ID)));

        final TreeNode<GedcomLine> subm = new TreeNode<>(GedcomLine.createEmptyId(SUMB_ID, GedcomTag.SUBM));
        tree.getRoot().addChild(subm);

        subm.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.NAME, "MINIMAL")));

        tree.getRoot().addChild(new TreeNode<>(GedcomLine.createTrailer()));

        if (charsetForce == null) {
            charsetForce = StandardCharsets.UTF_8;
        } else {
            log().info("Forcing input character encoding to " + charsetForce.name());
        }

        tree.setCharset(charsetForce);

        return tree;
    }
}

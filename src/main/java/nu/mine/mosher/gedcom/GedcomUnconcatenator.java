package nu.mine.mosher.gedcom;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import nu.mine.mosher.collection.TreeNode;



/**
 * Handles CONT and CONC tags in a given <code>GedcomTree</code> by appending
 * their values to the previous <code>GedcomLine</code>.
 * @author Chris Mosher
 */
class GedcomUnconcatenator {
    private static final int MAX_WIDTH = 200;

    private final GedcomTree tree;

    /**
     * @param tree
     */
    public GedcomUnconcatenator(final GedcomTree tree) {
        this.tree = tree;
    }

    public void unconcatenate() {
        unconc(this.tree.getRoot());
    }

    private void unconc(TreeNode<GedcomLine> node) {
        TreeNode<GedcomLine> xx = null;
        for (final TreeNode<GedcomLine> nodeChild : node) {
            if (xx == null) {
                xx = nodeChild;
            }
            unconc(nodeChild);
        }
        final TreeNode<GedcomLine> existingFirstChild = xx;

        final GedcomLine gedcomLine = node.getObject();

        if (gedcomLine != null) {
            final String value = gedcomLine.getValue();
            if (needsWork(value)) {
                final TreeSet<GedcomLine> currLine = new TreeSet<>();
                currLine.add(new GedcomLine(gedcomLine.getLevel(), "@"+gedcomLine.getID()+"@", gedcomLine.getTag().name(), ""));
                StringBuilder currValue = new StringBuilder(MAX_WIDTH);
                value.codePoints().forEach(c -> {
                    if (c == '\n') {
                        writeChild(node, existingFirstChild, currLine, currValue, GedcomTag.CONT);
                    } else if (currValue.length() >= MAX_WIDTH) {
                        writeChild(node, existingFirstChild, currLine, currValue, GedcomTag.CONC);
                        currValue.appendCodePoint(c);
                    } else {
                        currValue.appendCodePoint(c);
                    }
                });
                writeChild(node, existingFirstChild, currLine, currValue, GedcomTag.CONC);
            }
        }
    }

    private static void writeChild(TreeNode<GedcomLine> node, TreeNode<GedcomLine> existingFirstChild, TreeSet<GedcomLine> currLine, StringBuilder currValue, GedcomTag nextTag) {
        final GedcomLine g = currLine.first();
        final boolean alreadyOnChild = g.getTag().equals(GedcomTag.CONC) || g.getTag().equals(GedcomTag.CONT);
        if (alreadyOnChild) {
            if (existingFirstChild != null) {
                node.addChildBefore(new TreeNode<>(new GedcomLine(g.getLevel(), "", g.getTag().name(), currValue.toString())), existingFirstChild);
            } else {
                node.addChild(new TreeNode<>(new GedcomLine(g.getLevel(), "", g.getTag().name(), currValue.toString())));
            }
        } else {
            node.setObject(new GedcomLine(g.getLevel(), "@"+g.getID()+"@", g.getTag().name(), currValue.toString()));
        }
        final int level = alreadyOnChild ? g.getLevel() : g.getLevel()+1;
        currLine.clear();
        currLine.add(new GedcomLine(level, "", nextTag.name(), ""));
        currValue.setLength(0);
    }

    private static boolean needsWork(final String value) {
        return value.length() > MAX_WIDTH || value.contains("\n");
    }
}

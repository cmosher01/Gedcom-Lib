package nu.mine.mosher.gedcom;


import java.util.TreeSet;

import nu.mine.mosher.collection.TreeNode;


/**
 * @author Chris Mosher
 */
class GedcomUnconcatenator {
    public static final int DEFAULT_MAX_LENGTH = 60;

    private final GedcomTree tree;
    private final int maxLength;

    public GedcomUnconcatenator(final GedcomTree tree) {
        this(tree, DEFAULT_MAX_LENGTH);
    }

    public GedcomUnconcatenator(final GedcomTree tree, final int maxLength) {
        this.tree = tree;
        this.maxLength = maxLength;
    }

    public void unconcatenate() {
        unconc(this.tree.getRoot());
    }

    private void unconc(final TreeNode<GedcomLine> node) {
        node.forEach(this::unconc);

        final TreeNode<GedcomLine> existingFirstChild = node.children().hasNext() ? node.children().next() : null;

        final GedcomLine gedcomLine = node.getObject();

        if (gedcomLine != null) {
            final String value = gedcomLine.getValue();
            if (isSplitable(gedcomLine.getTag()) && needsWork(value)) {
                final TreeSet<GedcomLine> currLine = new TreeSet<>();
                currLine.add(new GedcomLine(gedcomLine.getLevel(), "@" + gedcomLine.getID() + "@", gedcomLine.getTag().name(), ""));
                final StringBuilder currValue = new StringBuilder(this.maxLength);
                // TODO: do not let line end with whitespace
                /* TODO: new algorithm, something like this:
                split by newline
                for each line
                  while len > max
                    b = max-1
                    while charAt b is whitespace and b > 0
                      b--
                    if b == 0, b = max
                    remove b chars from front into new row
                 */
                value.codePoints().forEach(c -> {
                    if (c == '\n') {
                        writeChild(node, existingFirstChild, currLine, currValue, GedcomTag.CONT);
                    } else if (currValue.length() >= this.maxLength) {
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

    private boolean isSplitable(GedcomTag tag) {
        // TODO: not all TITLs are splitable, only SOUR.TITLs
        return tag.equals(GedcomTag.NOTE) || tag.equals(GedcomTag.TEXT) || tag.equals(GedcomTag.AUTH) /*|| tag.equals(GedcomTag.TITL)*/ || tag.equals(GedcomTag.PUBL) || tag.equals(GedcomTag.COPR) || tag.equals(GedcomTag.DSCR);
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
            node.setObject(new GedcomLine(g.getLevel(), "@" + g.getID() + "@", g.getTag().name(), currValue.toString()));
        }
        final int level = alreadyOnChild ? g.getLevel() : g.getLevel() + 1;
        currLine.clear();
        currLine.add(new GedcomLine(level, "", nextTag.name(), ""));
        currValue.setLength(0);
    }

    private boolean needsWork(final String value) {
        return value.length() > this.maxLength || value.contains("\n");
    }
}

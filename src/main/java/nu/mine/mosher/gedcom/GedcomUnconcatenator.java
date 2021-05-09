package nu.mine.mosher.gedcom;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import nu.mine.mosher.collection.TreeNode;


/**
 * @author Chris Mosher
 */
public class GedcomUnconcatenator {
    public static final int DEFAULT_MAX_LENGTH = 60;

    private static final int KEEP_TRAILING_EMPTY_STRINGS = -999;
    private static final Pattern LINEBREAK = Pattern.compile("\\R");



    private final GedcomTree tree;
    private final int maxLength;


    public GedcomUnconcatenator(final GedcomTree tree) {
        this.tree = tree;
        int m = tree.getMaxLength();
        this.maxLength = m != 0 ? m : DEFAULT_MAX_LENGTH;
    }

    public void unconcatenate() {
        unconcDeep(this.tree.getRoot());
    }



    private void unconcDeep(final TreeNode<GedcomLine> node) {
        node.forEach(this::unconcDeep);
        unconc(node);
    }

    private void unconc(final TreeNode<GedcomLine> node) {
        final GedcomLine gedcomLine = node.getObject();
        if (needsWork(gedcomLine)) {
            final List<GedcomLine> gedcomLinesToAdd = new ArrayList<>(8);
            splitToContConc(gedcomLine.getValue(), this.maxLength, gedcomLine.getLevel() + 1, gedcomLinesToAdd);
            addContConcChildren(gedcomLinesToAdd, node);
        }
    }

    private boolean needsWork(final GedcomLine line) {
        return (line != null)  && (line.getValue().length() > this.maxLength || LINEBREAK.matcher(line.getValue()).find());
    }






    private static void addContConcChildren(final List<GedcomLine> lines, final TreeNode<GedcomLine> node) {
        final TreeNode<GedcomLine> insertionPoint = node.getFirstChildOrNull();
        boolean first = true;
        for (final GedcomLine line : lines) {
            if (first) {
                first = false;
                node.setObject(node.getObject().replaceValue(line.getValue()));
            } else {
                node.addChildBefore(new TreeNode<>(line), insertionPoint);
            }
        }
    }

    /**
     * Utility method to help with generating CONT/CONC lines for GEDCOM.
     * Splits originalValue into lines, and splits each line into segments
     * at most maxLen in size each.
     * <p>
     * The results are appended to gedcomLines. For example:
     * <p>
     * CONT line-0-segment0
     * CONC line-0-segment1
     * CONC line-0-segment2
     * CONT line-1-segment0
     * CONT line-2-segment0
     * CONC line-2-segment1
     * <p>
     * Each level will be set to gedcomLevel.
     * <p>
     * When you use the resulting gedcomLines, you would typically use
     * only the value from the first line and reset the existing node's
     * value to it. Then append as (first-most) children the remaining
     * gedcomLines to the existing node;
     *
     * TODO look at the whole line, not just the value, and make all final GEDCOM lines the same length
     *
     * @param originalValue
     * @param maxLen
     * @param gedcomLevel
     * @param gedcomLines
     */
    private static void splitToContConc(final String originalValue, final int maxLen, final int gedcomLevel, final List<GedcomLine> gedcomLines) {
        for (final String line : LINEBREAK.split(originalValue, KEEP_TRAILING_EMPTY_STRINGS)) {
            addLines(line, maxLen, gedcomLevel, gedcomLines);
        }
    }

    private static void addLines(final String line, final int maxLen, final int gedcomLevel, final List<GedcomLine> gedcomLines) {
        final List<String> segments = new ArrayList<>(8);
        splitLineIntoSegments(line, maxLen, segments);
        addSegments(segments, gedcomLevel, gedcomLines);
    }

    private static void addSegments(final List<String> segments, final int gedcomLevel, final List<GedcomLine> gedcomLines) {
        GedcomTag tag = GedcomTag.CONT;
        for (final String segment : segments) {
            gedcomLines.add(new GedcomLine(gedcomLevel, "", tag.name(), segment));
            tag = GedcomTag.CONC;
        }
    }

    public static void splitLineIntoSegments(final String line, final int maxLen, final List<String> segments) {
        assert 0 < maxLen && maxLen < 100000;
        assert segments != null;

        // TODO: log WARNING if segments is not empty upon entry

        String s = line;

        final Sanity sanity = Sanity.create(100000);
        while (s.length() > maxLen) {
            sanity.check();

            final String[] lr = cut(s, findSplitPosition(s, maxLen));
            segments.add(lr[0]);
            s = lr[1];
        }

        segments.add(s);
    }

    /**
     * Apply split rules in order of precedence to find the (first)
     * position in the given line at which to break it.
     *
     * The three algorithms are as follows, in order of precedence:
     * 1. break within a word (leaving no whitespace on either the
     *    end of the first line or the beginning of the second line),
     *
     * 2. break at the end of a word (leaving no whitespace on the
     *    end of the first line, but whitespace on the beginning of
     *    the second line), or
     *
     * 3. break at maxLength, regardless of whitespace.
     *
     * @param line
     * @param maxLen
     * @return
     */
    private static int findSplitPosition(final String line, int maxLen) {
        int pos = 0;

        if (pos <= 0) {
            pos = breakOnWord(line, maxLen, true);
        }
        if (pos <= 0) {
            pos = breakOnWord(line, maxLen, false);
        }
        if (pos <= 0) {
            // TODO is it worth logging this case?
            pos = maxLen;
        }

        return pos;
    }

    private static int breakOnWord(final String s, final int maxLen, final boolean within) {
        int pos = maxLen;
        final Sanity sanity = Sanity.create(100000);
        while (pos > 0 && (spL(s, pos) || (within && spR(s, pos)))) {
            sanity.check();
            --pos;
        }
        return pos;
    }

    /**
     * Is there a space to the Left of pos in String s?
     *
     * @param s
     * @param pos
     * @return
     */
    private static boolean spL(final String s, final int pos) {
        return Character.isWhitespace(s.codePointBefore(pos));
    }

    /**
     * Is there a space to the Right of pos in String s?
     *
     * @param s
     * @param pos
     * @return
     */
    private static boolean spR(final String s, final int pos) {
        return Character.isWhitespace(s.codePointAt(pos));
    }

    private static String[] cut(final String s, final int at) {
        return new String[]{s.substring(0, at), s.substring(at)};
    }


    /**
     * Simple utility class to aid in preventing infinite
     * loops, just to be extra cautious.
     */
    public static class Sanity {
        private int sanity;

        private Sanity(final int n) {
            this.sanity = n;
        }

        public static Sanity create(final int n) {
            return new Sanity(n);
        }

        public void check() {
            --this.sanity;
            assert this.sanity > 0;
            if (!(this.sanity > 0)) {
                throw new IllegalStateException("Possible infinte loop detected, and aborted.");
            }
        }
    }
}

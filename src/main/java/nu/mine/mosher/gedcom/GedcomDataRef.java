package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static nu.mine.mosher.logging.Jul.log;

/**
 * Eventually this will be a full-fledged data reference
 * language, like in Family Historian; but, for now it
 * just handles nested tags.
 *
 * For example, the expression <CODE>.SOUR.REFN</CODE>
 * refers to <CODE>REFN</CODE> sub-records within
 * top-level <CODE>SOUR</CODE> records.
 */
public class GedcomDataRef {
    public static class InvalidSyntax extends Exception {
    }

    /**
     * Represents each item in the reference expression.
     * For example, the "BIRT" in ".INDI.BIRT", or the "*"
     * in ".INDI.*.DATE"
     */
    private static class Tag {
        final String tagAsString;
        GedcomTag tag;
        Pattern pattern;
        Tag(final String tag) {
            this.tagAsString = tag;
            try {
                this.tag = GedcomTag.valueOf(tag);
            } catch (final Throwable e) {
                this.tag = null;
            }
        }
        void setPattern(final String pattern) throws PatternSyntaxException {
            this.pattern = Pattern.compile(pattern);
        }
    }

    private final List<Tag> path;



    public GedcomDataRef(final String expr) throws InvalidSyntax {
        this.path = parse(expr);
    }


    public void forEach(final GedcomTree tree, final Consumer<TreeNode<GedcomLine>> fn) {
        forEachHelper(tree.getRoot(), 0, fn);
    }

    private void forEachHelper(final TreeNode<GedcomLine> node, final int level, final Consumer<TreeNode<GedcomLine>> fn) {
        node.forEach( c -> {
            if (matches(level, c)) {
                if (at(level)) {
                    fn.accept(c);
                } else {
                    forEachHelper(c, level + 1, fn);
                }
            }
        });
    }

    public boolean matches(final int i, final TreeNode<GedcomLine> node) {
        if (i < 0 || this.path.size() <= i) {
            return false;
        }

        final Tag tagRef = this.path.get(i);
        if (tagRef.tagAsString.equals("*") || node.getObject().getTagString().toLowerCase().equals(tagRef.tagAsString)) {
            return matchesPattern(tagRef.pattern, node.getObject().getValue());
        }

        return false;
    }

    private boolean matchesPattern(final Pattern pattern, final String value) {
        if (pattern == null) {
            return true;
        }
        return pattern.matcher(value).matches();
    }

    public String get(final int i) {
        if (i < 0 || this.path.size() <= i) {
            return "";
        }
        return this.path.get(i).tagAsString;
    }

    public boolean at(final int i) {
        return i == this.path.size() - 1;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(40);

        this.path.forEach(i -> {
            sb.append('.');
            sb.append(i);
        });

        return sb.toString();
    }



    private static ArrayList<Tag> parse(final String expr) throws InvalidSyntax {
        final ArrayList<Tag> path = new ArrayList<>(8);

        final int START = 0;
        final int TAG = 1;
        final int DOWN = 2;

        final StreamTokenizer token = tokenizer(expr);

        int state = START;
        while (next(token)) {
            switch (state) {
                case START: {
                    if (token.ttype == '.') {
                        //this.rooted = true;
                    } else {
                        token.pushBack();
                    }
                    state = TAG;
                }
                break;
                case TAG: {
                    if (token.ttype == '.') {
                        throw new InvalidSyntax();
                    }
                    path.add(new Tag(token.sval));
                    state = DOWN;
                }
                break;
                case DOWN: {
                    if (token.ttype == '\"') {
                        path.get(path.size()-1).setPattern(token.sval);
                    } else if (token.ttype != '.') {
                        throw new InvalidSyntax();
                    } else {
                        state = TAG;
                    }
                }
                break;
            }
        }

        return path;
    }

    private static StreamTokenizer tokenizer(final String expr) {
        final StreamTokenizer t = new StreamTokenizer(new StringReader(expr));

        t.resetSyntax();

        t.slashSlashComments(false);
        t.slashStarComments(false);
        t.eolIsSignificant(false);
        t.lowerCaseMode(true);

        // valid characters for GEDCOM tag:
        t.wordChars('0', '9');
        t.wordChars('A', 'Z');
        t.wordChars('a', 'z');
        t.wordChars('_', '_');

        // characters with special meaning to us
        t.wordChars('*', '*');

        // quotes (for value-matching pattern string)
        t.quoteChar('\"');

        return t;
    }

    private static boolean next(final StreamTokenizer t) {
        try {
            return t.nextToken() != StreamTokenizer.TT_EOF;
        } catch (final Throwable cannotHappen) {
            throw new IllegalStateException();
        }
    }
}

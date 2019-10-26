package nu.mine.mosher.gedcom;


import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.IOException;
import java.nio.charset.*;
import java.text.*;
import java.util.*;

import static nu.mine.mosher.logging.Jul.log;


/**
 * Represents a GEDCOM document. A GEDCOM document is a tree structure or
 * <code>GedcomLine</code> objects.
 *
 * @author Chris Mosher
 */
public class GedcomTree {
    private Charset charset = null;
    private int maxLength = 0;
    private final TreeNode<GedcomLine> root;
    private final Map<String, TreeNode<GedcomLine>> mapIDtoNode = new HashMap<>();

    private int prevLevel;
    private TreeNode<GedcomLine> prevNode;

    /**
     * Initializes a new <code>GedcomTree</code>.
     */
    public GedcomTree() {
        this.root = new TreeNode<>();
        this.prevNode = this.root;
        this.prevLevel = -1;
    }

    public Charset getCharset() {
        return this.charset;
    }

    private static final Map<Charset, String> mapCharsetToGedcom;

    static {
        final HashMap<Charset, String> m = new HashMap<>();
        m.put(StandardCharsets.UTF_8, "UTF-8");
        m.put(StandardCharsets.UTF_16, "UTF-16");
        m.put(StandardCharsets.US_ASCII, "ASCII");
        mapCharsetToGedcom = Collections.unmodifiableMap(m);
    }

    public void setCharset(Charset charset) {
        if (!mapCharsetToGedcom.containsKey(charset)) {
            log().warning("Cannot convert to encoding " + charset.name() + "; defaulting to UTF-8.");
            charset = StandardCharsets.UTF_8;
        }
        for (final TreeNode<GedcomLine> r : this.root) {
            if (r.getObject().getTag().equals(GedcomTag.HEAD)) {
                for (final TreeNode<GedcomLine> c : r) {
                    if (c.getObject().getTag().equals(GedcomTag.CHAR)) {
                        c.setObject(c.getObject().replaceValue(mapCharsetToGedcom.get(charset)));
                    }
                }
            }
        }
        this.charset = charset;
        log().info("Set output character encoding to " + this.charset.name());
    }

    void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public void readFrom(final GedcomParser parser) throws InvalidLevel {
        int i = 0;
        for (final GedcomLine line : parser) {
            ++i;
            log().finest("parsed GEDCOM line: " + line);
            try {
                appendLine(line);
            } catch (final InvalidLevel err) {
                log().warning("at line number " + i); // TODO improve error reporting
                throw err;
            }
        }
    }

    /**
     * Appends a <code>GedcomLine</code> to this tree. This method must be
     * called in the same sequence that GEDCOM lines appear in the file.
     *
     * @param line GEDCOM line to be appended to this tree.
     * @throws InvalidLevel if the <code>line</code>'s level is invalid (that
     *                      is, in the wrong sequence to be correct within the context of
     *                      the lines added to this tree so far)
     */
    private void appendLine(final GedcomLine line) throws InvalidLevel {
        final int cPops = this.prevLevel + 1 - line.getLevel();
        if (cPops < 0) {
            throw new InvalidLevel(line);
        }

        TreeNode<GedcomLine> parent = this.prevNode;
        for (int i = 0; i < cPops; ++i) {
            parent = parent.parent();
        }

        this.prevLevel = line.getLevel();
        this.prevNode = new TreeNode<>();
        this.prevNode.setObject(line);
        parent.addChild(this.prevNode);

        if (line.hasID()) {
            this.mapIDtoNode.put(line.getID(), this.prevNode);
        }
    }

    /**
     * Gets the node in this <code>GedcomTree</code> with the given ID.
     *
     * @param id ID of the GEDCOM node to look up
     * @return the node with the given ID.
     */
    public TreeNode<GedcomLine> getNode(final String id) {
        return this.mapIDtoNode.get(id);
    }

    /**
     * Returns a string representation of this tree. The string returned is
     * intended for debugging purposes, not for any kind of persistence.
     *
     * @return string representation of this tree
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(1024);

        try {
            this.root.appendStringDeep(sb);
        } catch (final IOException e) {
            /*
             * StringBuffer does not throw IOException, so this should never
             * happen.
             */
            throw new IllegalStateException(e);
        }

        return sb.toString();
    }

    /**
     * Gets the root of this tree.
     *
     * @return root node
     */
    public TreeNode<GedcomLine> getRoot() {
        return this.root;
    }

    private static final String formDate = "dd MMM yyyy";
    private static final String formTime = "HH:mm:ss";

    public void timestamp() {
        final Date now = new Date();

        final DateFormat dfDate = new SimpleDateFormat(formDate);
        dfDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String date = dfDate.format(now).toUpperCase();

        final DateFormat dfTime = new SimpleDateFormat(formTime);
        dfTime.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String time = dfTime.format(now);

        for (final TreeNode<GedcomLine> head : this.root) {
            if (head.getObject().getTag().equals(GedcomTag.HEAD)) {
                addOrUpdateChild(head, GedcomTag.DATE, date);
                for (final TreeNode<GedcomLine> d : head) {
                    if (d.getObject().getTag().equals(GedcomTag.DATE)) {
                        addOrUpdateChild(d, GedcomTag.TIME, time);
                    }
                }
            }
        }
    }

    public static void addOrUpdateChild(final TreeNode<GedcomLine> nodeParent, final GedcomTag tagChild, final String valChild) {
        for (final TreeNode<GedcomLine> nodeChild : nodeParent) {
            final GedcomLine lineChild = nodeChild.getObject();
            if (lineChild.getTag().equals(tagChild)) {
                nodeChild.setObject(lineChild.replaceValue(valChild));
                return;
            }
        }
        nodeParent.addChild(new TreeNode<>(GedcomLine.create(nodeParent.getObject().getLevel() + 1, tagChild, valChild)));
    }
}

package nu.mine.mosher.gedcom;



import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;



/**
 * Represents a GEDCOM document. A GEDCOM document is a tree structure or
 * <code>GedcomLine</code> objects.
 * @author Chris Mosher
 */
public class GedcomTree
{
    private Charset charset = null;
    private int maxLength = 0;
    private final TreeNode<GedcomLine> root;
    private final Map<String, TreeNode<GedcomLine>> mapIDtoNode = new HashMap<>();

    private int prevLevel;
    private TreeNode<GedcomLine> prevNode;

    /**
     * Initializes a new <code>GedcomTree</code>.
     */
    public GedcomTree()
    {
        this.root = new TreeNode<>();
        this.prevNode = this.root;
        this.prevLevel = -1;
    }

    public Charset getCharset() {
        return this.charset;
    }

    private static final Map<Charset, String> mapCharsetToGedcom = Collections.unmodifiableMap(new HashMap<Charset, String>() {{
        put(Charset.forName("UTF-8"), "UTF-8");
        put(Charset.forName("UTF-16"), "UTF-16");
        put(Charset.forName("x-gedcom-ansel"), "ANSEL");
        put(Charset.forName("US-ASCII"), "ASCII");
    }});

    public void setCharset(final Charset charset) {
        boolean first = (this.charset == null);
        if (!first) {
            if (!mapCharsetToGedcom.containsKey(charset)) {
                throw new IllegalStateException("Cannot convert to encoding "+charset.name());
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
        }
        this.charset = charset;
    }

    void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return this.maxLength;
    }
    /**
     * Appends a <code>GedcomLine</code> to this tree. This method must be
     * called in the same sequence that GEDCOM lines appear in the file.
     * @param line GEDCOM line to be appended to this tree.
     * @throws InvalidLevel if the <code>line</code>'s level is invalid (that
     *             is, in the wrong sequence to be correct within the context of
     *             the lines added to this tree so far)
     */
    void appendLine(final GedcomLine line) throws InvalidLevel
    {
        final int cPops = this.prevLevel + 1 - line.getLevel();
        if (cPops < 0)
        {
            throw new InvalidLevel(line);
        }

        TreeNode<GedcomLine> parent = this.prevNode;
        for (int i = 0; i < cPops; ++i)
        {
            parent = parent.parent();
        }

        this.prevLevel = line.getLevel();
        this.prevNode = new TreeNode<>();
        this.prevNode.setObject(line);
        parent.addChild(this.prevNode);

        if (line.hasID())
        {
            this.mapIDtoNode.put(line.getID(), this.prevNode);
        }
    }

    /**
     * Gets the node in this <code>GedcomTree</code> with the given ID.
     * @param id ID of the GEDCOM node to look up
     * @return the node with the given ID.
     */
    public TreeNode<GedcomLine> getNode(final String id)
    {
        return this.mapIDtoNode.get(id);
    }

    /**
     * Returns a string representation of this tree. The string returned is
     * intended for debugging purposes, not for any kind of persistence.
     * @return string representation of this tree
     */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(1024);

        try
        {
            this.root.appendStringDeep(sb);
        }
        catch (final IOException e)
        {
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
     * @return root node
     */
    public TreeNode<GedcomLine> getRoot()
    {
        return this.root;
    }
}

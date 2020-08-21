package nu.mine.mosher.gedcom;


import static java.text.Normalizer.Form.NFC;
import static java.text.Normalizer.normalize;

/**
 * Represents one GEDCOM entry (usually one line). Objects of this class are
 * immutable.
 *
 * @author Chris Mosher
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class GedcomLine implements Comparable<GedcomLine> {
    private static final GedcomUidGenerator uid = new GedcomUidGenerator(20);

    private final int level;
    private final String id;
    private final String tagString;
    private final String value;
    private final String pointer;
    private final GedcomTag tag;

    /**
     * Initializes a <code>GedcomLine</code>.
     *
     * @param level
     * @param id
     * @param tag
     * @param value
     */
    GedcomLine(final int level, final String id, final String tag, final String value) {
        this.level = level;
        this.id = getPointer(id);
        this.tagString = tag;
        final String v = getPointer(value);
        if (v.length() > 0) {
            this.pointer = v;
            this.value = "";
        } else {
            this.value = replaceAts(value);
            this.pointer = "";
        }

        this.tag = parseTag();
    }

    private GedcomLine(final String id, final int level, final String pointer, final GedcomTag tag, final String tagString, final String value) {
        this.id = id;
        this.level = level;
        this.pointer = pointer;
        this.tag = tag;
        this.tagString = tagString;
        this.value = value;
    }

    private GedcomTag parseTag() {
        try {
            return GedcomTag.valueOf(this.tagString);
        } catch (final IllegalArgumentException e) {
            return GedcomTag.UNKNOWN;
        }
    }

    private static String getPointer(final String s) {
        if (s.contains("@@") || !s.startsWith("@") || !s.endsWith("@") || s.length() < 3) {
            return "";
        }

        final String pointer = s.substring(1, s.length() - 1);
        if (pointer.indexOf('@') >= 0) {
            return "";
        }

        return pointer;
    }

    private static String replaceAts(final String s) {
        return s.replaceAll("@@", "@");
    }

    private static String restoreAts(final String s) {
        return s.replaceAll("@", "@@");
    }

    public GedcomLine replaceValue(final String newValue) {
        return new GedcomLine(this.level, "@" + this.id + "@", this.tagString, newValue);
    }

    public GedcomLine replaceLink(final String newId) {
        if (hasID()) {
            return replaceId(newId);
        }
        if (isPointer()) {
            return replacePointer(newId);
        }
        throw new IllegalStateException("GedcomLine does not have an ID or a Pointer: " + toString());
    }

    public GedcomLine replacePointer(final String newPointerWithoutAts) {
        return new GedcomLine(this.level, "", this.tagString, "@" + newPointerWithoutAts + "@");
    }

    public GedcomLine replaceId(final String newIdWithoutAts) {
        return new GedcomLine(this.level, "@" + newIdWithoutAts + "@", this.tagString, this.value);
    }

    public GedcomLine replacePointer(final GedcomLine to) {
        return new GedcomLine(this.level, "", this.tagString, "@" + to.getID() + "@");
    }

    public GedcomLine createChild(final GedcomTag newTag, final String newValue) {
        return createChild(newTag.name(), newValue);
    }

    public GedcomLine createChild(final String newTag, final String newValue) {
        return new GedcomLine(this.level + 1, "", newTag, newValue);
    }

    public static GedcomLine createHeader() {
        return new GedcomLine(0, "", GedcomTag.HEAD.name(), "");
    }

    public static GedcomLine createTrailer() {
        return new GedcomLine(0, "", GedcomTag.TRLR.name(), "");
    }

    public static GedcomLine createEmpty(final int level, final GedcomTag tag) {
        return new GedcomLine(level, "", tag.name(), "");
    }

    public static GedcomLine createEmptyId(final String idWithoutAts, final GedcomTag tag) {
        return new GedcomLine(0, "@" + idWithoutAts + "@", tag.name(), "");
    }

    public static GedcomLine createEmptyUid(final GedcomTag tag) {
        return createEmptyId(uid.generateId(), tag);
    }

    public static GedcomLine create(final int level, final GedcomTag tag, final String valueWithDoubledAts) {
        return new GedcomLine(level, "", tag.name(), valueWithDoubledAts);
    }

    public static GedcomLine createId(final String idWithoutAts, final GedcomTag tag, final String valueWithDoubledAts) {
        return new GedcomLine(0, "@" + idWithoutAts + "@", tag.name(), valueWithDoubledAts);
    }

    public static GedcomLine createUid(final GedcomTag tag, final String valueWithDoubledAts) {
        return createId(uid.generateId(), tag, valueWithDoubledAts);
    }

    public static GedcomLine createPointer(final int level, final GedcomTag tag, final String ptrWithoutAts) {
        return new GedcomLine(level, "", tag.name(), "@" + ptrWithoutAts + "@");
    }

    public static GedcomLine createUser(final int level, final String tag, final String valueWithDoubledAts) {
        return new GedcomLine(level, "", tag, valueWithDoubledAts);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);

        sb.append(this.level).append(" ");

        if (hasID()) {
            sb.append("@").append(this.id).append("@ ");
        }

        sb.append(this.tagString);

        if (isPointer() || this.value.length() > 0) {
            sb.append(" ");
        }

        if (isPointer()) {
            sb.append("@").append(this.pointer).append("@");
        } else {
            String v = this.value;
            if (!this.tag.equals(GedcomTag.DATE)) {
                v = restoreAts(v);
                // TODO make normalization optional
                v = normalize(v, NFC);
            }
            sb.append(v);
        }
        return sb.toString();
    }

    public void dump(final StringBuilder appendTo) {
        appendTo.append(this.level);
        appendTo.append(",");
        if (hasID()) {
            appendTo.append("id=");
            appendTo.append(this.id);
            appendTo.append(",");
        }
        appendTo.append("tag=");
        appendTo.append(this.tagString);
        appendTo.append(",");
        if (isPointer()) {
            appendTo.append("pointer=");
            appendTo.append(this.pointer);
        } else {
            appendTo.append("value=\"");
            appendFilteredValue(this.value, appendTo);
            appendTo.append("\"");
        }
    }

    private static void appendFilteredValue(final String value, final StringBuilder appendTo) {
        appendTo.append(value.replaceAll("\n", "[NEWLINE]"));
    }

    /**
     * @return if this line has an ID
     */
    public boolean hasID() {
        return this.id.length() > 0;
    }

    /**
     * @return if this line has a pointer
     */
    public boolean isPointer() {
        return this.pointer.length() > 0;
    }

    public boolean isLink() {
        return isPointer() || hasID();
    }

    /**
     * @return the ID for this line
     */
    public String getID() {
        return this.id;
    }

    /**
     * @return the level number of this line
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * @return the pointer value, if any, in this line
     */
    public String getPointer() {
        return this.pointer;
    }

    public String getLink() {
        if (isPointer()) {
            return getPointer();
        }
        if (hasID()) {
            return getID();
        }
        throw new IllegalStateException("GedcomLine does not have an ID or a Pointer: " + toString());
    }

    /**
     * @return the GEDCOM tag on this line
     */
    public GedcomTag getTag() {
        return this.tag;
    }

    public String getTagString() {
        return this.tagString;
    }

    /**
     * @return the actual value of this line
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Handles CONT tags by appending the given string to the value of this
     * line, and returning a new <code>GedcomLine</code>.
     *
     * @param sContinuedLine
     * @return new <code>GedcomLine</code>
     */
    GedcomLine contValue(final String sContinuedLine) {
        return new GedcomLine(this.id, this.level, this.pointer, this.tag,
            this.tagString, this.value + "\n" + sContinuedLine);
    }

    /**
     * Handles CONC tags by appending the given string to the value of this
     * line, and returning a new <code>GedcomLine</code>.
     *
     * @param sConcatenatedLine
     * @return new <code>GedcomLine</code>
     */
    GedcomLine concValue(final String sConcatenatedLine) {
        return new GedcomLine(this.id, this.level, this.pointer, this.tag,
            this.tagString, this.value + sConcatenatedLine);
    }

    @Override
    public int compareTo(final GedcomLine that) {
        int c = 0;
        if (c == 0) {
            c = this.tagString.compareTo(that.tagString);
        }
        if (c == 0) {
            c = this.value.compareTo(that.value);
        }
        return c;
    }
}

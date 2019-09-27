package nu.mine.mosher.gedcom.model;

import java.text.BreakIterator;

/*
 * Created on 2006-10-15.
 */
public class Source {
    private final String ID;
    private final String author;
    private final String title;
    private final String publication;
    private final String text;
    private final String shortName;

    /**
     * @param author
     * @param title
     * @param publication
     */
    public Source(final String ID, final String author, final String title, final String publication, final String text) {
        this.ID = ID;
        this.author = author;
        this.title = title;
        this.publication = publication;
        this.text = text;

        this.shortName = buildShortName();
    }

    public String getID() {
        return this.ID;
    }

    private String buildShortName() {
        if (this.title.length() == 0) {
            return this.title;
        }

        final BreakIterator wordIter = BreakIterator.getWordInstance();
        wordIter.setText(this.title);
        int start = wordIter.first();
        final StringBuilder sb = new StringBuilder(32);
        int iWord = 0;
        for (int end = wordIter.next(); end != BreakIterator.DONE && ++iWord < 15; start = end, end = wordIter.next()) {
            sb.append(this.title, start, end);
        }
        return sb.toString();
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return this.author;
    }

    /**
     * @return the publication
     */
    public String getPublication() {
        return this.publication;
    }

    /**
     * @return the shortName
     */
    public String getShortName() {
        return this.shortName;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @return the text
     */
    public String getText() {
        return this.text;
    }
}

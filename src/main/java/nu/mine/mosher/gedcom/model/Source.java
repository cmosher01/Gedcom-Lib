package nu.mine.mosher.gedcom.model;

import java.text.BreakIterator;
import java.util.UUID;

/*
 * Created on 2006-10-15.
 */
public class Source
{
	private final UUID uuid;
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
	public Source(final String ID, final String author, final String title, final String publication,final String text, final UUID uuid)
	{
		this.ID = ID;
		if (uuid == null)
		{
			this.uuid = UUID.randomUUID();
		}
		else
		{
			this.uuid = uuid;
		}
		this.author = author;
		this.title = title;
		this.publication = publication;
		this.text = text;

		this.shortName = buildShortName();
	}

	public String getID() {
		return this.ID;
	}

	private String buildShortName()
	{
		if (this.title.length() == 0)
		{
			return this.title;
		}

		final BreakIterator wordIter = BreakIterator.getWordInstance();
		wordIter.setText(this.title);
		int start = wordIter.first();
		final StringBuilder sb = new StringBuilder(32);
		int iWord = 0;
		for (int end = wordIter.next(); end != BreakIterator.DONE && ++iWord < 15; start = end, end = wordIter.next())
		{
			sb.append(this.title.substring(start,end));
		}
		return sb.toString();
	}

	/**
	 * @return the author
	 */
	public String getAuthor()
	{
		return this.author;
	}

	/**
	 * @return the publication
	 */
	public String getPublication()
	{
		return this.publication;
	}

	/**
	 * @return the shortName
	 */
	public String getShortName()
	{
		return this.shortName;
	}

	/**
	 * @return the title
	 */
	public String getTitle()
	{
		return this.title;
	}

	/**
	 * @return the text
	 */
	public String getText()
	{
		return this.text;
	}

	/**
	 * Gets the UUID from the gedcom file, or a generated
	 * one if there was not one in the file.
	 * @return the UUID
	 */
	public UUID getUuid()
	{
		return this.uuid;
	}

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof Source)) {
            return false;
        }
        final Source that = (Source)object;
        return this.uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }
}

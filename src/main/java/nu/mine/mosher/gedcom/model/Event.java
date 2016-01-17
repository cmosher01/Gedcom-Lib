package nu.mine.mosher.gedcom.model;

import nu.mine.mosher.gedcom.date.DatePeriod;

/*
 * Created on 2006-10-08.
 */
public class Event implements Comparable<Event>
{
	private final String type;
	private final DatePeriod date;
	private final String place;
	private final String note;
	private final Source source;
	private final String citationPage;
	private final String citationExtraText;

	/**
	 * @param type
	 * @param date
	 * @param place
	 * @param note
	 * @param source
	 */
	public Event(final String type, final DatePeriod date, final String place, final String note, final Source source, final String citationPage, final String citationExtraText)
	{
		this.type = type;
		this.date = date;
		this.place = place;
		this.note = note;
		this.source = source;
		this.citationPage = citationPage;
		this.citationExtraText = citationExtraText;
	}

	public String getType()
	{
		return this.type;
	}
	public DatePeriod getDate()
	{
		return this.date;
	}
	public String getPlace()
	{
		return this.place;
	}
	public String getNote()
	{
		return this.note;
	}
	public Source getSource()
	{
		return this.source;
	}
	public String getCitationPage() { return citationPage; }
	public String getCitationExtraText() { return citationExtraText; }

	@Override
	public int compareTo(final Event that)
	{
		if (this.date == null && that.date == null)
		{
			return 0;
		}
		if (this.date == null)
		{
			return +1;
		}
		if (that.date == null)
		{
			return -1;
		}
		return this.date.compareTo(that.date);
	}
}

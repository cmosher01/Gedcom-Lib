package nu.mine.mosher.gedcom.model;

import nu.mine.mosher.gedcom.date.DatePeriod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
 * Created on 2006-10-08.
 */
public class Event implements Comparable<Event>
{
	private final String type;
	private final DatePeriod date;
	private final String place;
	private final String note;
	private final List<Citation> citations;

	public Event(final String type, final DatePeriod date, final String place, final String note, final List<Citation> citations)
	{
		this.type = type;
		this.date = date;
		this.place = place;
		this.note = note;
		this.citations = Collections.unmodifiableList(new ArrayList<>(citations));
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
	public List<Citation> getCitations() { return this.citations; }

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

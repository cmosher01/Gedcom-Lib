package nu.mine.mosher.gedcom.model;

import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.date.DateRange;

import java.util.*;

/*
 * Created on 2006-10-08.
 */
public class Event implements Comparable<Event>, Privatizable
{
	private final String type;
	private final DatePeriod date;
	private final String place;
	private final String note;
	private final List<Citation> citations;
    private final boolean isPrivate;

    public Event(final String type, final DatePeriod date, final String place, final String note, final List<Citation> citations)
    {
        this(type,date,place,note,citations,false);
    }
	public Event(final String type, final DatePeriod date, final String place, final String note, final List<Citation> citations, final boolean isPrivate)
	{
		this.type = type;
        this.date = Optional.ofNullable(date).orElse(DatePeriod.UNKNOWN);
		this.place = place;
		this.note = note;
		this.citations = Collections.unmodifiableList(new ArrayList<>(citations));
		this.isPrivate = isPrivate;
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

    @Override
    public boolean isPrivate() {
        return this.isPrivate;
    }
}

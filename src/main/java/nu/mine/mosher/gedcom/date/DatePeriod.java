/*
 * Created on Apr 23, 2005
 */
package nu.mine.mosher.gedcom.date;



/**
 * Represents a period of time, specified by a starting and ending date, either
 * of which could be a range of possible dates.
 * @author Chris Mosher
 */
public class DatePeriod implements Comparable<DatePeriod>
{
    private final DateRange dateStart;
    private final DateRange dateEnd;

    /**
     * Represents an unknown date (Jan. 1, 9999 BC to Dec. 31, 9999 AD)
     */
    public static final DatePeriod UNKNOWN = new DatePeriod(DateRange.UNKNOWN);

    /**
     * Degenerate (but also nominal) case of a period lasting just one day.
     * @param date the single date
     */
    public DatePeriod(final DateRange date)
    {
        this(date, date);
    }

    /**
     * Period of time lasting from <code>dateStart</code> to
     * <code>dateEnd</code>, inclusive. Either argument could be
     * <code>null</code>, indicating "unknown."
     * @param dateStart start date (range) or <code>null</code>
     * @param dateEnd end date (range) or <code>null</code>
     */
    public DatePeriod(final DateRange dateStart, final DateRange dateEnd)
    {
        if (dateStart == null)
        {
            this.dateStart = DateRange.UNKNOWN;
        }
        else
        {
            this.dateStart = dateStart;
        }
        if (dateEnd == null)
        {
            this.dateEnd = DateRange.UNKNOWN;
        }
        else
        {
            this.dateEnd = dateEnd;
        }
    }

    /**
     * @return the start date
     */
    public DateRange getStartDate()
    {
        return this.dateStart;
    }

    /**
     * @return the end date
     */
    public DateRange getEndDate()
    {
        return this.dateEnd;
    }

    /**
     * Checks if this period is just one date (that is, start equals end).
     * @return if start equals end
     */
    public boolean isSingle()
    {
        return this.dateStart.equals(this.dateEnd);
    }

    /**
     * Checks if this period has the same start and end date (ranges) as the
     * given <code>DatePeriod</code>.
     * @return if this period is the same as the given period
     */
    @Override
    public boolean equals(final Object object)
    {
        if (!(object instanceof DatePeriod))
        {
            return false;
        }
        final DatePeriod that = (DatePeriod) object;
        return this.dateStart.equals(that.dateStart)
            && this.dateEnd.equals(that.dateEnd);
    }

    /**
     * Gets a hash code for this period.
     */
    @Override
    public int hashCode()
    {
        return this.dateStart.hashCode() ^ this.dateEnd.hashCode();
    }

    /**
     * Alternate user-displayable representation, intended for
     * tabular display.
     * @return
     */
    public String getTabularString() {
        if (isSingle()) {
            return this.dateStart.getTabularString();
        }
        return this.dateStart.getTabularString() + "/" + this.dateEnd.getTabularString();
    }

    /**
     * Returns a string representation of this period, intended for display to
     * the end user (not persistence).
     * @return display string
     */
    @Override
    public String toString()
    {
        if (isSingle())
        {
            return this.dateStart.toString();
        }
        return this.dateStart.toString() + "-" + this.dateEnd.toString();
    }

    /**
     * Compares two periods; for sorting purposes.
     * @param that period to compare to (can be <code>null</code>)
     * @return -1, 0, or +1, for less, equal, or greater
     */
    @Override
    public int compareTo(final DatePeriod that)
    {
        if (that == null)
        {
            return -1;
        }

        final DateRange thisDate = this.dateStart.equals(DateRange.UNKNOWN) ? this.dateEnd : this.dateStart;
        final DateRange thatDate = that.dateStart.equals(DateRange.UNKNOWN) ? that.dateEnd : that.dateStart;

        if (thatDate.equals(DateRange.UNKNOWN)) {
            return -1;
        }
        if (thisDate.equals(DateRange.UNKNOWN)) {
            return +1;
        }

        return thisDate.compareTo(thatDate);
    }

    /**
     * Checks to see if this DatePeriod overlaps the given DatePeriod.
     * @param periodTarget period to compare to
     * @return true if they overlap
     */
    public boolean overlaps(final DatePeriod periodTarget)
    {
        return this.dateStart.compareTo(periodTarget.dateEnd) <= 0
            && periodTarget.dateStart.compareTo(this.dateEnd) <= 0;
    }
}

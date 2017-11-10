package nu.mine.mosher.gedcom.model;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.date.DateRange;
import nu.mine.mosher.gedcom.date.YMD;
import nu.mine.mosher.time.Time;



/*
 * Created on 2006-10-08.
 */
public class Person implements Comparable<Person>
{
    private static final Pattern patternName = Pattern.compile("(.*)/(.*)/(.*)");

    private final UUID uuid;
    private final String ID;
    private final String name;
    private final String nameSortable;
    private final String nameSortedDisplay;
    private final ArrayList<Event> rEvent;
    private final ArrayList<Partnership> rPartnership;
    private final boolean isPrivate;

    private Time birth = new Time(new Date(0));
    private Time death = new Time(new Date(0));
    private List<Time> rMarriage = new ArrayList<>();
    private List<Time> rDivorce = new ArrayList<>();

    private Person father;
    private Person mother;

    /**
     * @param ID
     * @param name
     * @param rEvent
     * @param partnership
     * @param isPrivate
     * @param uuid
     */
    public Person(final String ID, final String name, final ArrayList<Event> rEvent, final ArrayList<Partnership> partnership,
        final boolean isPrivate, final UUID uuid)
    {
        if (uuid == null)
        {
            this.uuid = UUID.randomUUID();
        }
        else
        {
            this.uuid = uuid;
        }
        this.ID = ID;
        this.name = name;
        this.nameSortable = buildNameForAlphaSort(name);
        this.nameSortedDisplay = buildNameForAlphaDisplay(name);
        this.rEvent = rEvent;
        this.rPartnership = partnership;
        this.isPrivate = isPrivate;

        Collections.sort(this.rEvent);
        Collections.sort(this.rPartnership);
    }

    public static String buildNameForAlphaSort(final String name) {
        final Matcher matcher = patternName.matcher(name);
        if (!matcher.matches()) {
            return name.trim().toUpperCase();
        }

        final String givenName1 = matcher.group(1).trim();
        final String surname = matcher.group(2).trim();
        final String givenName2 = matcher.group(3).trim();

        return buildAlphaName(cleanseGivenNames(givenName1), cleanseSurname(surname), cleanseGivenNames(givenName2)).toUpperCase();
    }

    public static String buildNameForAlphaDisplay(final String name) {
        final Matcher matcher = patternName.matcher(name);
        if (!matcher.matches()) {
            return name;
        }

        final String givenName1 = matcher.group(1).trim();
        final String surname = matcher.group(2).trim();
        final String givenName2 = matcher.group(3).trim();

        return buildAlphaName(givenName1, surname, givenName2);
    }

    public static String buildAlphaName(final String givenName1, final String surname, final String givenName2) {
        final StringBuilder sb = new StringBuilder(32);
        appendWord(sb, "", surname);
        appendWord(sb, ", ", givenName1);
        appendWord(sb, " ", givenName2);
        return sb.toString();
    }

    public static void appendWord(final StringBuilder appendTo, final String delim, final String wordOrEmpty) {
        if (wordOrEmpty == null) {
            return;
        }
        final String w = wordOrEmpty.trim();
        if (w.isEmpty()) {
            return;
        }
        if (appendTo.length() > 0) {
            appendTo.append(delim);
        }
        appendTo.append(w);
    }

    public static String cleanseGivenNames(final String s) {
        return cleanse(s," ");
    }

    public static String cleanseSurname(final String s) {
        return cleanse(s,"\u00a0");
    }

    public static String cleanse(final String s, final String r) {
        return s.replaceAll("[^\\p{IsAlphabetic}\\p{IsWhite_Space}]", "").replaceAll("\\p{IsWhite_Space}+", r);
    }

    public void initKeyDates()
    {
        for (final Event event : this.rEvent)
        {
            if (event.getDate() == null)
            {
                continue;
            }
            if (event.getType().equals("birth"))
            {
                this.birth = event.getDate().getStartDate().getApproxDay();
            }
            else if (event.getType().equals("death"))
            {
                this.death = event.getDate().getStartDate().getApproxDay();
            }
        }
        if (this.birth.asDate().getTime() == 0)
        {
            this.birth = YMD.getMinimum().getApproxTime();
        }
        if (this.death.asDate().getTime() == 0)
        {
            this.death = YMD.getMaximum().getApproxTime();
        }
        if (this.rPartnership.size() > 0)
        {
            for (final Partnership par : this.rPartnership)
            {
                boolean mar = false;
                boolean div = false;
                for (final Event event : par.getEvents())
                {
                    if (event.getDate() == null)
                    {
                        continue;
                    }
                    if (event.getType().equals("marriage"))
                    {
                        this.rMarriage.add(event.getDate().getStartDate().getApproxDay());
                        mar = true;
                    }
                    else if (event.getType().equals("divorce"))
                    {
                        this.rDivorce.add(event.getDate().getStartDate().getApproxDay());
                        div = true;
                    }
                }
                if (!mar)
                {
                    if (par.getChildren().size() > 0)
                    {
                        final Time birthChild = par.getChildren().get(0).getBirth();
                        final GregorianCalendar cal = new GregorianCalendar();
                        cal.setGregorianChange(new Date(Long.MIN_VALUE));
                        cal.setTime(birthChild.asDate());
                        cal.add(Calendar.YEAR, -1);
                        this.rMarriage.add(new Time(cal.getTime()));
                        mar = true;
                    }
                }
                if (!mar)
                {
                    this.rMarriage.add(YMD.getMinimum().getApproxTime());
                    mar = true;
                }
                if (!div)
                {
                    this.rDivorce.add(this.death);
                    div = true;
                }
                assert mar && div;
            }
        }
        else
        {
            final GregorianCalendar cal = new GregorianCalendar();
            cal.setGregorianChange(new Date(Long.MIN_VALUE));
            cal.setTime(this.birth.asDate());
            cal.add(Calendar.YEAR, 18);
            this.rMarriage.add(new Time(cal.getTime()));

            this.rDivorce.add(YMD.getMaximum().getApproxTime());
        }
    }

    public Time getBirth()
    {
        return this.birth;
    }

    public Time getDeath()
    {
        return this.death;
    }

    @Override
    public String toString()
    {
        return this.name.replaceAll("/", "");
    }

    public String getNameSortable() {
        return this.nameSortable;
    }

    public String getNameSortedDisplay() {
        return this.nameSortedDisplay;
    }

    public String getClassedName()
    {
        final Matcher matcher = patternName.matcher(this.name);
        if (!matcher.matches())
        {
            /* oops, can't find surname, so don't style it */
            return this.name;
        }

        return matcher.group(1) + "<span class=\"surname\">" + matcher.group(2) + "</span>" + matcher.group(3);
    }

    /**
     * @param father the father to set
     */
    public void setFather(final Person father)
    {
        this.father = father;
    }

    /**
     * @param mother the mother to set
     */
    public void setMother(final Person mother)
    {
        this.mother = mother;
    }

    public Person getFather()
    {
        return this.father;
    }

    public Person getMother()
    {
        return this.mother;
    }

    public ArrayList<Event> getEvents()
    {
        return this.rEvent;
    }

    public ArrayList<Event> getEventsWithin(final DatePeriod period)
    {
        final ArrayList<Event> rWithin = new ArrayList<>();
        for (final Event event : this.rEvent)
        {
            if (event.getDate() == null)
            {
                continue;
            }
            if (event.getDate().overlaps(period))
            {
                rWithin.add(event);
            }
        }
        return rWithin;
    }

    public ArrayList<Partnership> getPartnerships()
    {
        return this.rPartnership;
    }

    /**
     * Gets the UUID from the gedcom file, or a generated one if there was not
     * one in the file.
     * @return the UUID
     */
    public UUID getUuid()
    {
        return this.uuid;
    }

    public String getID()
    {
        return this.ID;
    }

    public boolean isPrivate()
    {
        return this.isPrivate;
    }

    @Override
    public int compareTo(final Person that)
    {
        if (this.rEvent.isEmpty() && that.rEvent.isEmpty())
        {
            return 0;
        }
        if (!this.rEvent.isEmpty() && that.rEvent.isEmpty())
        {
            return -1;
        }
        if (this.rEvent.isEmpty() && !that.rEvent.isEmpty())
        {
            return +1;
        }
        return this.rEvent.get(0).compareTo(that.rEvent.get(0));
    }

    public ArrayList<FamilyEvent> getFamilyEvents()
    {
        final ArrayList<FamilyEvent> rEventRet = new ArrayList<>();

        getEventsOfSelf(rEventRet);
        getEventsOfPartnership(rEventRet);
        getEventsOfFather(rEventRet);
        getEventsOfMother(rEventRet);
        getEventsOfSpouses(rEventRet);
        getEventsOfChildren(rEventRet);

        Collections.sort(rEventRet);

        return rEventRet;
    }

    private void getEventsOfSelf(final List<FamilyEvent> rEventRet)
    {
        rEventRet.addAll(this.getEvents().stream().map(event -> new FamilyEvent(this, event, "self")).collect(Collectors.toList()));
    }

    private void getEventsOfPartnership(final List<FamilyEvent> rEventRet)
    {
        for (final Partnership part : this.rPartnership)
        {
            rEventRet.addAll(part.getEvents().stream().map(event -> new FamilyEvent(part.getPartner(), event, "spouse")).collect(Collectors.toList()));
        }
    }

    private void getEventsOfFather(final List<FamilyEvent> rEventRet)
    {
        getEventsOfParent(this.father, "father", rEventRet);
    }

    private void getEventsOfMother(final List<FamilyEvent> rEventRet)
    {
        getEventsOfParent(this.mother, "mother", rEventRet);
    }

    private void getEventsOfParent(final Person parent, final String relation, final List<FamilyEvent> rEventRet)
    {
        if (parent != null)
        {
            rEventRet.addAll(parent.getEventsWithin(getChildhood()).stream().map(event -> new FamilyEvent(parent, event, relation)).collect(Collectors.toList()));
        }
    }

    private void getEventsOfSpouses(final List<FamilyEvent> rEventRet)
    {
        int p = 0;
        for (final Partnership partnership : this.rPartnership)
        {
            final Person partner = partnership.getPartner();
            if (partner != null)
            {
                rEventRet.addAll(partner.getEventsWithin(getPartnerhood(p)).stream().map(event -> new FamilyEvent(partnership.getPartner(), event, "spouse")).collect(Collectors.toList()));
            }
            ++p;
        }
    }

    private void getEventsOfChildren(final List<FamilyEvent> rEventRet)
    {
        for (final Partnership partnership : this.rPartnership)
        {
            for (final Person child : partnership.getChildren())
            {
                rEventRet.addAll(child.getEventsWithin(child.getChildhood()).stream().map(event -> new FamilyEvent(child, event, "child")).collect(Collectors.toList()));
            }
        }
    }

    private DatePeriod getChildhood()
    {
        return new DatePeriod(
            new DateRange(new YMD(this.birth)),
            new DateRange(new YMD(this.rMarriage.get(0))));
    }

    private DatePeriod getPartnerhood(final int p)
    {
        return new DatePeriod(
            new DateRange(new YMD(this.rMarriage.get(p))),
            new DateRange(new YMD(this.rDivorce.get(p))));
    }
}

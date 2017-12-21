package nu.mine.mosher.gedcom.model;

import java.io.StringReader;
import java.text.Collator;
import java.util.*;

import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.GedcomTag;
import nu.mine.mosher.gedcom.GedcomTree;
import nu.mine.mosher.gedcom.date.parser.GedcomDateValueParser;
import nu.mine.mosher.gedcom.date.parser.ParseException;
import nu.mine.mosher.gedcom.date.parser.TokenMgrError;
import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.gedcom.date.DateRange.DatesOutOfOrder;
import nu.mine.mosher.time.Time;
import nu.mine.mosher.collection.TreeNode;

/**
 * Parses the given <code>GedcomTree</code> into <code>Person</code> objects.
 * <p>
 * <p>Created on 2006-10-09.</p>
 *
 * @author Chris Mosher
 */
public class Loader {
    private final GedcomTree gedcom;
    private final String name;

    private final Map<UUID, Person> mapUUIDtoPerson = new HashMap<>();
    private final Map<UUID, Source> mapUUIDtoSource = new HashMap<>();

    private final Map<TreeNode<GedcomLine>, Person> mapNodeToPerson = new HashMap<>();
    //	private final Map<TreeNode<GedcomLine>,Partnership> mapNodeToPartnership = new HashMap<>();
    private final Map<TreeNode<GedcomLine>, Source> mapNodeToSource = new HashMap<>();
    private final Map<TreeNode<GedcomLine>, Event> mapNodeToEvent = new HashMap<>();

    private Person first;
    private final List<Person> people = new ArrayList<>(256);
    private final Collator sorter;
    private String description;


    public Loader(final GedcomTree gedcom, final String filename) {
        this.gedcom = gedcom;
        this.name = filename;
        this.sorter = sorter();
    }


    public void parse() {
        final Collection<TreeNode<GedcomLine>> rNodeTop = new ArrayList<>();
        getChildren(this.gedcom.getRoot(), rNodeTop);

        String root = "";
        for (final TreeNode<GedcomLine> nodeTop : rNodeTop) {
            final GedcomLine lineTop = nodeTop.getObject();
            final GedcomTag tagTop = lineTop.getTag();

            if (tagTop.equals(GedcomTag.HEAD)) {
                root = parseHead(nodeTop);
                break;
            }
        }

        final Map<String, Source> mapIDtoSource = new HashMap<>();

        for (final TreeNode<GedcomLine> nodeTop : rNodeTop) {
            final GedcomLine lineTop = nodeTop.getObject();
            final GedcomTag tagTop = lineTop.getTag();

            if (tagTop.equals(GedcomTag.SOUR)) {
                final Source source = parseSource(nodeTop);
                this.mapNodeToSource.put(nodeTop, source);
                mapIDtoSource.put(source.getID(), source);
                storeInUuidMap(source);
            }
        }

        final Map<String, Person> mapIDtoPerson = new HashMap<>();

        for (final TreeNode<GedcomLine> nodeTop : rNodeTop) {
            final GedcomLine lineTop = nodeTop.getObject();
            final GedcomTag tagTop = lineTop.getTag();

            if (tagTop.equals(GedcomTag.INDI)) {
                final Person person = parseIndividual(nodeTop, mapIDtoSource);
                this.people.add(person);
                this.mapNodeToPerson.put(nodeTop, person);
                mapIDtoPerson.put(person.getID(), person);
                storeInUuidMap(person);
                if (this.first == null || person.getID().equals(root)) {
                    this.first = person;
                }
            }
        }

        for (final TreeNode<GedcomLine> nodeTop : rNodeTop) {
            final GedcomLine lineTop = nodeTop.getObject();
            final GedcomTag tagTop = lineTop.getTag();

            if (tagTop.equals(GedcomTag.FAM)) {
                parseFamily(nodeTop, mapIDtoPerson, mapIDtoSource);
            }
        }

        mapIDtoPerson.values().forEach(Person::initKeyDates);

        this.people.sort((p1, p2) -> this.sorter.compare(p1.getNameSortable(), p2.getNameSortable()));
    }

    /* list of all people, sorted by name */
    public List<Person> getAllPeople() {
        return Collections.unmodifiableList(this.people);
    }

    private Collator sorter() {
        final Collator c = Collator.getInstance();
        c.setDecomposition(Collator.FULL_DECOMPOSITION);
        c.setStrength(Collator.PRIMARY);
        return c;
    }

    public String getName() {
        return this.name;
    }

    public GedcomTree getGedcom() {
        return this.gedcom;
    }

    public String getDescription() {
        return this.description;
    }

    public Person getFirstPerson() {
        return this.first;
    }

    public Person lookUpPerson(final UUID uuid) {
        return this.mapUUIDtoPerson.get(uuid);
    }

    public Source lookUpSource(final UUID uuid) {
        return this.mapUUIDtoSource.get(uuid);
    }

    public Person lookUpPerson(final TreeNode<GedcomLine> node) {
        return this.mapNodeToPerson.get(node);
    }

    public Source lookUpSource(final TreeNode<GedcomLine> node) {
        return this.mapNodeToSource.get(node);
    }

    public Event lookUpEvent(final TreeNode<GedcomLine> node) {
        return this.mapNodeToEvent.get(node);
    }

    public void appendAllUuids(final Set<UUID> appendTo) {
        appendTo.addAll(this.mapUUIDtoPerson.keySet());
    }

    private void storeInUuidMap(final Person person) {
        final UUID uuid = person.getUuid();
        if (uuid == null) {
            return;
        }
        final Person existing = this.mapUUIDtoPerson.get(uuid);
        if (existing != null) {
            System.err.println("Duplicate INDI UUID value: " + uuid);
            return;
        }
        this.mapUUIDtoPerson.put(uuid, person);
    }

    private void storeInUuidMap(final Source source) {
        final UUID uuid = source.getUuid();
        if (uuid == null) {
            return;
        }
        final Source existing = this.mapUUIDtoSource.get(uuid);
        if (existing != null) {
            return;
        }
        this.mapUUIDtoSource.put(uuid, source);
    }


    private static void getChildren(TreeNode<GedcomLine> root, Collection<TreeNode<GedcomLine>> rNodeTop) {
        for (final TreeNode<GedcomLine> child : root) {
            rNodeTop.add(child);
        }
    }

    private String parseHead(final TreeNode<GedcomLine> head) {
        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(head, rNode);

        String root = "";
        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();
            if (tag.equals(GedcomTag.NOTE)) {
                this.description = line.getValue();
            } else if (line.getTagString().equals("_ROOT")) {
                root = line.getPointer();
            }
        }
        return root;
    }

    private Source parseSource(final TreeNode<GedcomLine> nodeSource) {
        String author = "";
        String title = "";
        String publication = "";
        String text = "";
        UUID uuid = null;

        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(nodeSource, rNode);

        for (final TreeNode<GedcomLine> n : rNode) {
            final GedcomLine line = n.getObject();
            final GedcomTag tag = line.getTag();
            if (tag.equals(GedcomTag.AUTH)) {
                author = line.getValue();
            } else if (tag.equals(GedcomTag.TITL)) {
                title = line.getValue();
            } else if (tag.equals(GedcomTag.PUBL)) {
                publication = line.getValue();
            } else if (tag.equals(GedcomTag.TEXT)) {
                text = line.getValue();
            } else if (uuid == null && hasUuidTag(line)) {
                uuid = parseUuid(n);
            }
        }
        if (uuid == null) {
            //System.err.println("Cannot find REFN UUID for source \"" + title + "\"; will generate temporary UUID.");
        }
        return new Source(nodeSource.getObject().getID(), author, title, publication, text, uuid);
    }

    private Person parseIndividual(final TreeNode<GedcomLine> nodeIndi, final Map<String, Source> mapIDtoSource) {
        String name = "";
        UUID uuid = null;
        final ArrayList<Event> rEvent = new ArrayList<>();
        boolean isPrivate = false;

        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(nodeIndi, rNode);

        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();
            if (uuid == null && hasUuidTag(line)) {
                uuid = parseUuid(node);
            } else if (GedcomTag.setIndividualEvent.contains(tag) || GedcomTag.setIndividualAttribute.contains(tag) || tag.equals(GedcomTag.NAME)) {
                if (tag.equals(GedcomTag.NAME)) {
                    if (name.isEmpty()) {
                        // grab out the name (just the first one)
                        name = parseName(node);
                    }
                    // fall through and parse name as an event:
                }
                final Event event = parseEvent(node, mapIDtoSource);
                this.mapNodeToEvent.put(node, event);
                rEvent.add(event);
                if (tag.equals(GedcomTag.BIRT) && !isPrivate) {
                    isPrivate = calculatePrivacy(event);
                }
            }
        }
        if (name.isEmpty()) {
            name = "[unknown]";
        }
        if (uuid == null) {
            //System.err.println("Cannot find REFN UUID for individual \"" + name + "\"; will generate temporary UUID.");
        }

        return new Person(nodeIndi.getObject().getID(), name, rEvent, new ArrayList<>(), isPrivate, uuid);
    }

    private static boolean hasUuidTag(final GedcomLine line) {
        if (line.getTag().equals(GedcomTag.REFN)) {
            return true;
        }
        if (!line.getTag().equals(GedcomTag.UNKNOWN)) {
            return false;
        }
        return line.getTagString().equals("_UID");
    }

    private static UUID parseUuid(final TreeNode<GedcomLine> nodeUuid) {
        try {
            return UUID.fromString(nodeUuid.getObject().getValue());
        } catch (final Throwable ignore) {
            return null;
        }
    }

    private void parseFamily(final TreeNode<GedcomLine> nodeFam, final Map<String, Person> mapIDtoPerson, final Map<String, Source> mapIDtoSource) {
        Person husb = null;
        Person wife = null;
        final ArrayList<Person> rChild = new ArrayList<>();
        final ArrayList<Event> rEvent = new ArrayList<>();

        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(nodeFam, rNode);

        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();

            if (tag.equals(GedcomTag.HUSB)) {
                husb = lookUpPerson(line.getPointer(), mapIDtoPerson);
            } else if (tag.equals(GedcomTag.WIFE)) {
                wife = lookUpPerson(line.getPointer(), mapIDtoPerson);
            } else if (tag.equals(GedcomTag.CHIL)) {
                final Person child = lookUpPerson(line.getPointer(), mapIDtoPerson);
                rChild.add(child);
            } else if (GedcomTag.setFamilyEvent.contains(tag)) {
                final Event event = parseEvent(node, mapIDtoSource);
                this.mapNodeToEvent.put(node, event);
                rEvent.add(event);
            }
        }
        buildFamily(husb, wife, rChild, rEvent);
    }

    private static Person lookUpPerson(final String id, final Map<String, Person> mapIDtoPerson) {
        return mapIDtoPerson.get(id);
    }

    private static Source lookUpSource(final String id, final Map<String, Source> mapIDtoSource) {
        return mapIDtoSource.get(id);
    }

    private static String parseName(final TreeNode<GedcomLine> nodeName) {
        return nodeName.getObject().getValue();
    }

    private Event parseEvent(final TreeNode<GedcomLine> nodeEvent, final Map<String, Source> mapIDtoSource) {
        final String whichEvent = getEventName(nodeEvent);

        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(nodeEvent, rNode);

        DatePeriod date = null;
        String place = "";
        String note = "";
        final ArrayList<Citation> citations = new ArrayList<>();

        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();
            if (tag.equals(GedcomTag.DATE)) {
                final String sDate = line.getValue();
                final GedcomDateValueParser parser = new GedcomDateValueParser(new StringReader(sDate));
                try {
                    date = parser.parse();
                } catch (final ParseException | DatesOutOfOrder | TokenMgrError e) {
                    System.err.println("Error while parsing \"" + sDate + "\"");
                    e.printStackTrace();
                    date = null;
                }
            } else if (tag.equals(GedcomTag.PLAC)) {
                place = line.getValue();
            } else if (tag.equals(GedcomTag.NOTE)) {
                final String n = parseNote(node);
                if (!note.isEmpty() && !n.isEmpty()) {
                    note += "/n";
                }
                note += n;
            } else if (tag.equals(GedcomTag.SOUR)) {
                final Source source = lookUpSource(node.getObject().getPointer(), mapIDtoSource);
                final String citationPage = getSourcePtPage(node);
                final String citationExtraText = getSourcePtText(node);
                citations.add(new Citation(source, citationPage, citationExtraText));
            }
        }
        // TODO handle case of date == null
        return new Event(whichEvent, date, place, note, citations);
    }

    private static String getSourcePtPage(TreeNode<GedcomLine> node) {
        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(node, rNode);
        for (final TreeNode<GedcomLine> n : rNode) {
            final GedcomLine line = n.getObject();
            final GedcomTag tag = line.getTag();
            if (tag.equals(GedcomTag.PAGE)) {
                return n.getObject().getValue();
            }
        }
        return "";
    }

    private static String getSourcePtText(TreeNode<GedcomLine> node) {
        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(node, rNode);
        for (final TreeNode<GedcomLine> n : rNode) {
            final GedcomLine line = n.getObject();
            final GedcomTag tag = line.getTag();
            if (tag.equals(GedcomTag.DATA)) {
                return parseData(n);
            }
        }
        return "";
    }

    private static String parseData(TreeNode<GedcomLine> node) {
        final StringBuilder sb = new StringBuilder(256);
        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(node, rNode);
        for (final TreeNode<GedcomLine> n : rNode) {
            final GedcomLine line = n.getObject();
            final GedcomTag tag = line.getTag();
            if (tag.equals(GedcomTag.TEXT)) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(n.getObject().getValue());
            }
        }
        return sb.toString();
    }

    private String parseNote(final TreeNode<GedcomLine> node) {
        final String id = node.getObject().getPointer();
        final TreeNode<GedcomLine> nodeNote = this.gedcom.getNode(id);
        if (nodeNote == null) {
            return "";
        }
        return nodeNote.getObject().getValue();
    }

    private static boolean calculatePrivacy(final Event birth) {
        final DatePeriod dateInformation = birth.getDate();
        if (dateInformation == null) {
            return false;
        }

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -90);
        final Time dateLatestPublicInformation = new Time(cal.getTime());
        return dateLatestPublicInformation.compareTo(dateInformation.getEndDate().getApproxDay()) < 0;
    }

    private static String getEventName(final TreeNode<GedcomLine> node) {
        final GedcomTag tag = node.getObject().getTag();

        if (tag.equals(GedcomTag.EVEN)) {
            final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
            getChildren(node, rNode);
            for (final TreeNode<GedcomLine> n : rNode) {
                final GedcomLine line = n.getObject();
                final GedcomTag t = line.getTag();
                if (t.equals(GedcomTag.TYPE)) {
                    return line.getValue();
                }
            }
        }

        final String eventName = tag.equals(GedcomTag.NAME) ? "Name" : EventNames.getName(tag);
        final String value = node.getObject().getValue();
        if (value.length() == 0) {
            return eventName;
        }
        return eventName + ": " + value;
    }

    private static void buildFamily(final Person husb, final Person wife, final ArrayList<Person> rChild, final ArrayList<Event> rEvent) {
        if (husb != null) {
            final Partnership partnership = new Partnership(rEvent);
            partnership.addChildren(rChild);
            if (wife != null) {
                partnership.setPartner(wife);
            }
            husb.getPartnerships().add(partnership);
        }
        if (wife != null) {
            final Partnership partnership = new Partnership(rEvent);
            partnership.addChildren(rChild);
            if (husb != null) {
                partnership.setPartner(husb);
            }
            wife.getPartnerships().add(partnership);
        }
        for (final Person child : rChild) {
            if (husb != null) {
                child.setFather(husb);
            }
            if (wife != null) {
                child.setMother(wife);
            }
        }
    }
}

package nu.mine.mosher.gedcom.model;

import java.io.StringReader;
import java.net.URI;
import java.text.Collator;
import java.util.*;
import java.util.logging.Level;

import nu.mine.mosher.gedcom.GedcomLine;
import nu.mine.mosher.gedcom.GedcomTag;
import nu.mine.mosher.gedcom.GedcomTree;
import nu.mine.mosher.gedcom.date.parser.GedcomDateValueParser;
import nu.mine.mosher.gedcom.date.DatePeriod;
import nu.mine.mosher.logging.Jul;
import nu.mine.mosher.time.Time;
import nu.mine.mosher.collection.TreeNode;

/**
 * Parses the given <code>GedcomTree</code> into <code>Person</code> objects.
 * <p>
 * <p>Created on 2006-10-09.</p>
 *
 * @author Chris Mosher
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Loader {

    public static class Partnerships {
        public Optional<Partnership> husb = Optional.empty();
        public Optional<Partnership> wife = Optional.empty();
    }

    // hardcoded source for Family Tree links (at FamilySearch)
    private static final Source sourceLdsFamilyTree = new Source(
        "74a586b6-6786-4ba1-859c-8e317fec4dde",
        "The Church of Jesus Christ of Latter‐day Saints [LDS]",
        "Family Tree",
        "https://www.familysearch.org",
        "");

    private final GedcomTree gedcom;
    private final String name;

    private final Map<UUID, Person> mapUUIDtoPerson = new HashMap<>();

    private final Map<TreeNode<GedcomLine>, Person> mapNodeToPerson = new HashMap<>();
    private final Map<TreeNode<GedcomLine>, Partnerships> mapNodeToPartnerships = new HashMap<>();
    private final Map<TreeNode<GedcomLine>, Event> mapNodeToEvent = new HashMap<>();
    private final Map<TreeNode<GedcomLine>, Source> mapNodeToSource = new HashMap<>();

    private Person first;
    private final List<Person> people = new ArrayList<>(256);
    private final Collator sorter;
    private String description = "";
    private String copyright = "";


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

        this.people.forEach(Person::initKeyDates);
        this.people.forEach(Person::sortPartnerships);

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

    public String getCopyright() {
        return this.copyright;
    }

    public Person getFirstPerson() {
        return this.first;
    }

    public Person lookUpPerson(final UUID uuid) {
        return this.mapUUIDtoPerson.get(uuid);
    }

    public Person lookUpPerson(final TreeNode<GedcomLine> node) {
        return this.mapNodeToPerson.get(node);
    }

    public Partnerships lookUpFamily(final TreeNode<GedcomLine> node) {
        return this.mapNodeToPartnerships.get(node);
    }

    public Event lookUpEvent(final TreeNode<GedcomLine> node) {
        return this.mapNodeToEvent.get(node);
    }

    public Source lookUpSource(final TreeNode<GedcomLine> node) {
        return this.mapNodeToSource.get(node);
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
            Jul.log().log(Level.WARNING, "Duplicate INDI UUID value: "+uuid);
            return;
        }
        this.mapUUIDtoPerson.put(uuid, person);
    }


    private static void getChildren(TreeNode<GedcomLine> root, final Collection<TreeNode<GedcomLine>> rNodeTop) {
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
            } else if (line.getTag().equals(GedcomTag.COPR)) {
                this.copyright = line.getValue();
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
            }
        }
        return new Source(nodeSource.getObject().getID(), author, title, publication, text);
    }

    private Person parseIndividual(final TreeNode<GedcomLine> nodeIndi, final Map<String, Source> mapIDtoSource) {
        String name = "";
        UUID uuid = null;
        final ArrayList<Event> rEvent = new ArrayList<>();
        boolean isPrivate = false;

        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(nodeIndi, rNode);

        Event birth = null;
        Event death = null;
        // first check if this individual should be privatized
        // 1. if INDI has RESN confidential; or,
        // 2. if INDI has recent birth
        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();

            if (!isPrivate) {
                if (tag.equals(GedcomTag.RESN)) {
                    isPrivate = isPrivateRestriction(line.getValue());
                }
            }
            if (tag.equals(GedcomTag.BIRT)) {
                birth = parseEvent(node, mapIDtoSource, false);
            } else if (tag.equals(GedcomTag.DEAT)) {
                death = parseEvent(node, mapIDtoSource, false);
            }
        }
        if (!isPrivate) {
            isPrivate = isRecentEnoughToPrivatize(birth, death);
        }

        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();
            if (uuid == null && hasUuidTag(line)) {
                uuid = parseUuid(node);
            } else if (isEventish(tag, line.getTagString())) {
                if (tag.equals(GedcomTag.NAME)) {
                    if (name.isEmpty()) {
                        // grab out the name (just the first one)
                        name = parseName(node);
                    }
                    // fall through and parse name as an event:
                }
                // note: private on INDI forces private on all their events, too
                final Event event = parseEvent(node, mapIDtoSource, isPrivate);
                this.mapNodeToEvent.put(node, event);
                rEvent.add(event);
            }
        }
        if (name.isEmpty()) {
            name = "[unknown]";
        }
        if (uuid == null) {
            Jul.log().log(Level.WARNING, "Cannot find REFN UUID for individual \"" + name + "\"; will generate temporary UUID.");
        }

        return new Person(nodeIndi.getObject().getID(), name, rEvent, new ArrayList<>(), isPrivate, uuid);
    }

    private boolean isEventish(final GedcomTag tag, String tagString) {
        return
            GedcomTag.setIndividualEvent.contains(tag) ||
            GedcomTag.setIndividualAttribute.contains(tag) ||
            tag.equals(GedcomTag.NOTE) ||
            tag.equals(GedcomTag.NAME) ||
            tag.equals(GedcomTag.SEX) ||
            tagString.equalsIgnoreCase("FSID"); // FSID is FamilySearch ID, as output from Family Tree Maker
    }

    private static boolean hasUuidTag(final GedcomLine line) {
        if (line.getTag().equals(GedcomTag.REFN)) {
            return true;
        }
        if (!line.getTag().equals(GedcomTag.UNKNOWN)) {
            return false;
        }
        final String tagString = line.getTagString();
        // FTM can output _GUID
        return tagString.equals("_UID") || tagString.equals("_UUID") || tagString.equals("_GUID");
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
        boolean isPrivate = false;

        final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
        getChildren(nodeFam, rNode);

        // step 1: get parents and privatization (because we will need to apply these to each child)
        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();

            if (tag.equals(GedcomTag.HUSB)) {
                husb = lookUpPerson(line.getPointer(), mapIDtoPerson);
            } else if (tag.equals(GedcomTag.WIFE)) {
                wife = lookUpPerson(line.getPointer(), mapIDtoPerson);
            }

            if (!isPrivate) {
                if (tag.equals(GedcomTag.RESN)) {
                    isPrivate = isPrivateRestriction(line.getValue());
                }
            }
            if (!isPrivate) {
                if (tag.equals(GedcomTag.MARR)) {
                    final Event event = parseEvent(node, mapIDtoSource, false);
                    isPrivate = isRecentEnoughToPrivatize(event, null);
                }
            }
        }

        // step 2: find children, and build parent-relationship info for each one
        final ArrayList<ParentChildRelation> husbandsChildren = new ArrayList<>();
        final ArrayList<ParentChildRelation> wifesChildren = new ArrayList<>();
        final ArrayList<Event> rEvent = new ArrayList<>();
        for (final TreeNode<GedcomLine> node : rNode) {
            final GedcomLine line = node.getObject();
            final GedcomTag tag = line.getTag();

            if (tag.equals(GedcomTag.CHIL)) {
                final Person child = lookUpPerson(line.getPointer(), mapIDtoPerson);

                // check for non-birth parents (Family Tree Maker tags _MREL and _FREL)
                String frel = "";
                String mrel = "";
                for (final TreeNode<GedcomLine> nodeSub : node) {
                    final GedcomLine lineSub = nodeSub.getObject();
                    final String tagSub = lineSub.getTagString();
                    if (tagSub.equalsIgnoreCase("_FREL")) {
                        frel = lineSub.getValue();
                    } else if (tagSub.equalsIgnoreCase("_MREL")) {
                        mrel = lineSub.getValue();
                    }
                }

                if (Objects.nonNull(husb)) {
                    // set husband's child relationship
                    husbandsChildren.add(ParentChildRelation.create(child, isPrivate, frel));
                    // set child's father relationship
                    child.addFather(ParentChildRelation.create(husb, isPrivate, frel));
                }
                if (Objects.nonNull(wife)) {
                    // set wife's child relationship
                    wifesChildren.add(ParentChildRelation.create(child, isPrivate, mrel));
                    // set child's mother relationship
                    child.addMother(ParentChildRelation.create(wife, isPrivate, mrel));
                }
            } else if (GedcomTag.setFamilyEvent.contains(tag)) {
                // note: private on FAM forces private on all its events, too
                final Event event = parseEvent(node, mapIDtoSource, isPrivate);
                this.mapNodeToEvent.put(node, event);
                rEvent.add(event);
            }
        }

        final Partnerships partnerships = new Partnerships();
        if (Objects.nonNull(husb)) {
            final Partnership part = new Partnership(rEvent, isPrivate);
            part.addChildRelations(husbandsChildren);
            if (Objects.nonNull(wife)) {
                part.setPartner(wife);
            }
            husb.getPartnerships().add(part);
            partnerships.husb = Optional.of(part);
        }
        if (Objects.nonNull(wife)) {
            final Partnership part = new Partnership(rEvent, isPrivate);
            part.addChildRelations(wifesChildren);
            if (Objects.nonNull(husb)) {
                part.setPartner(husb);
            }
            wife.getPartnerships().add(part);
            partnerships.wife = Optional.of(part);
        }
        this.mapNodeToPartnerships.put(nodeFam, partnerships);
    }

    private static Person lookUpPerson(final String id, final Map<String, Person> mapIDtoPerson) {
        return mapIDtoPerson.get(id);
    }

    private static Source lookUpSource(final String id, final Map<String, Source> mapIDtoSource) {
        return mapIDtoSource.containsKey(id) ?
            mapIDtoSource.get(id) :
            new Source("", "", "", "", "");
    }

    private static String parseName(final TreeNode<GedcomLine> nodeName) {
        return nodeName.getObject().getValue();
    }

    private Event parseEvent(final TreeNode<GedcomLine> nodeEvent, final Map<String, Source> mapIDtoSource, boolean isPrivate) {
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
                final String sDate = line.getValue().trim();
                final GedcomDateValueParser parser = new GedcomDateValueParser(new StringReader(sDate));
                try {
                    date = parser.parse();
                } catch (final Exception  e) {
                    if (!sDate.isEmpty()) {
                        Jul.log().log(Level.WARNING, "Invalid DATE format: \""+ sDate + "\"");
                    }
                    date = null;
                }
            } else if (tag.equals(GedcomTag.PLAC)) {
                place = line.getValue();
            } else if (tag.equals(GedcomTag.NOTE)) {
                // TODO should we keep multiple NOTE records separate?
                final String n = parseNote(node);
                if (!note.isEmpty() && !n.isEmpty()) {
                    note += "\n";
                }
                note += n;
            } else if (tag.equals(GedcomTag.SOUR)) {
                final Source source = lookUpSource(node.getObject().getPointer(), mapIDtoSource);
                final String page = getSourcePtPage(node);
                final String text = getSourcePtText(node);
                final Set<MultimediaReference> attachments = getSourcePtAttachments(node);
                final Set<URI> links = getSourcePtLinks(node);
                final Optional<AncestryPersona> apid = getSourcePtApid(node);
                citations.add(new Citation(source, page, text, attachments, links, thru(apid)));
            } else if (tag.equals(GedcomTag.RESN)) {
                if (!isPrivate) {
                    isPrivate = isPrivateRestriction(line.getValue());
                }
            }
        }
        if (nodeEvent.getObject().getTag().equals(GedcomTag.NOTE)) {
            note = parseNote(nodeEvent);
        } else if (nodeEvent.getObject().getTagString().equalsIgnoreCase("FSID")) {
            // FamilySearch ID, output from Family Tree Maker.
            // Add a link to familysearch.org (as a proper citation)
            citations.add(new Citation(sourceLdsFamilyTree, buildLdsFamilyTreeLink(nodeEvent.getObject().getValue()), "", new HashSet<>(), new HashSet<>(), null));
        } else if (!nodeEvent.getObject().getValue().isEmpty() &&
            !nodeEvent.getObject().getTag().equals(GedcomTag.NAME) &&
            !nodeEvent.getObject().getTag().equals(GedcomTag.SEX)) {
            if (!note.isEmpty()) {
                note += "\n";
            }
            note += nodeEvent.getObject().getValue();
        }

        return new Event(whichEvent, date, place, note, citations, isPrivate);
    }

    private String buildLdsFamilyTreeLink(final String fsid) {
        return "<bibl>" +
            "<author>The Church of Jesus Christ of Latter‐day Saints [LDS]</author>, " +
            "“<title level=\"u\">Family Tree</title>”, " +
            "database, " +
            "<title level=\"m\">FamilySearch</title> " +
            "(<ref target=\"https://www.familysearch.org/tree/person/" +
            fsid.trim() +
            "\"/>)." +
            "</bibl>";
    }

    /**
     * Check value of RESN line, and determine if record should be privatized.
     * (Assumes that RESN tag exists, of course.)
     * If value is illegal or unrecognized, then fail-safe and return true.
     * If value is "confidential", then return true.
     * If value is "locked, then return false (locked data is assumed non-private,
     * otherwise it wouldn't need to be locked).
     * If value is "privacy", it indicates that the data is not included in the
     * GEDCOM file; however, Family Tree Maker, for one, includes the
     * data; therefore, fail-safe and return true.
     * Values are compared ignoring upper-/lower-case.
     *
     * WARNING: this library does not automatically privatize any data. All
     * privatization is the responsibility of the caller.
     *
     * @param valueOfResnLine RESN value string
     * @return true if data should be privatized
     */
    private static boolean isPrivateRestriction(final String valueOfResnLine) {
        final String upperValue = valueOfResnLine.toUpperCase();
        return !upperValue.equals("LOCKED");
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static<T> T thru(final Optional<T> v) {
        return v.orElse(null);
    }

    private static Optional<AncestryPersona> getSourcePtApid(final TreeNode<GedcomLine> node) {
        for (final TreeNode<GedcomLine> apid : node) {
            if (apid.getObject().getTagString().equals("_APID")) {
                final String v = apid.getObject().getValue();
                return AncestryPersona.of(v);
            }
        }
        return Optional.empty();
    }

    private Set<URI> getSourcePtLinks(final TreeNode<GedcomLine> node) {
        final Set<URI> r = new HashSet<>();
        node.forAll(link -> {
            if (link.getObject().getTagString().equals("_LINK")) {
                addIfUri(r, link);
            }
        });
        return r;
    }

    private static void addIfUri(final Set<URI> r, final TreeNode<GedcomLine> link) {
        try {
            r.add(new URI(link.getObject().getValue()));
        } catch (final Throwable ignore) {
            // best-effort only
        }
    }

    private Set<MultimediaReference> getSourcePtAttachments(final TreeNode<GedcomLine> node) {
        final Set<MultimediaReference> r = new HashSet<>();
        node.forAll(obje -> {
            if (obje.getObject().getTag().equals(GedcomTag.OBJE) && obje.getObject().isPointer()) {
                final TreeNode<GedcomLine> n = this.gedcom.getNode(obje.getObject().getPointer());
                if (n != null) {
                    n.forAll(file -> {
                        if (file.getObject().getTag().equals(GedcomTag.FILE)) {
                            final String ref = file.getObject().getValue();
                            if (!ref.isEmpty()) {
                                r.add(new MultimediaReference(ref));
                            }
                        }
                    });
                }
            }
        });
        return r;
    }

    private static String getSourcePtPage(final TreeNode<GedcomLine> node) {
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

    private static String getSourcePtText(final TreeNode<GedcomLine> node) {
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

    private static String parseData(final TreeNode<GedcomLine> node) {
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
        if (!node.getObject().isPointer()) {
            return node.getObject().getValue();
        }
        final String id = node.getObject().getPointer();
        final TreeNode<GedcomLine> nodeNote = this.gedcom.getNode(id);
        if (nodeNote == null) {
            return "";
        }
        return nodeNote.getObject().getValue();
    }

    private static boolean isRecentEnoughToPrivatize(final Event event, final Event death) {
        if (Objects.isNull(event)) {
            return false;
        }
        final DatePeriod dpEvent = event.getDate();
        if (Objects.isNull(dpEvent) || dpEvent.equals(DatePeriod.UNKNOWN)) {
            return false;
        }

        if (Objects.nonNull(death)) {
            final DatePeriod dpDeath = death.getDate();
            if (Objects.nonNull(dpDeath) && !dpDeath.equals(DatePeriod.UNKNOWN)) {
                return false;
            }
        }

        return dateOfLatestPublicInformation().compareTo(dpEvent.getEndDate().getApproxDay()) < 0;
    }

    private static Time dateOfLatestPublicInformation() {
        final Calendar cal = Calendar.getInstance();
        // GEDCOM 5.5.1 mentions that Ancestral File uses 110 years:
        cal.add(Calendar.YEAR, -110);
        return new Time(cal.getTime());
    }

    private static String getEventName(final TreeNode<GedcomLine> node) {
        final GedcomTag tag = node.getObject().getTag();
        final String tagString = node.getObject().getTagString();

        String eventName = "";

        if (tag.equals(GedcomTag.NAME)) {
            eventName = "name";
        } else if (tag.equals(GedcomTag.NOTE)) {
            eventName = "note";
        } else if (tag.equals(GedcomTag.SEX)) {
            eventName = "sex";
        } else if (tag.equals(GedcomTag.EVEN)) {
            final Collection<TreeNode<GedcomLine>> rNode = new ArrayList<>();
            getChildren(node, rNode);
            for (final TreeNode<GedcomLine> n : rNode) {
                final GedcomLine line = n.getObject();
                final GedcomTag t = line.getTag();
                if (t.equals(GedcomTag.TYPE)) {
                    eventName = line.getValue();
                }
            }
        }

        if (tagString.equalsIgnoreCase("FSID")) {
            eventName = "FamilySearch Family Tree ID: "+node.getObject().getValue();
        }

        if (eventName.isEmpty()) {
            eventName = EventNames.getName(tag);
        }

        if (eventName.isEmpty()) {
            eventName = tagString;
        }

        if (tag.equals(GedcomTag.NAME)) {
            return eventName + ": " + node.getObject().getValue();
        }

        if (tag.equals(GedcomTag.SEX)) {
            return eventName + ": " + getSexName(node.getObject().getValue());
        }

        return eventName;
    }

    private static String getSexName(final String value) {
        switch (value) {
            case "M": return "male";
            case "F": return "female";
            case "U": return "unknown";
            default : return value;
        }
    }
}

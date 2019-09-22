package nu.mine.mosher.gedcom.model;

import java.util.*;

public class ParentChildRelation implements Comparable<ParentChildRelation>, Privatizable {
    private Person other;
    private Optional<String> relation = Optional.empty(); // empty indicates genetic/natural/biological parent
    private boolean isPrivate;

    public static ParentChildRelation of(final Person parent) {
        final ParentChildRelation it = new ParentChildRelation();
        it.setOther(parent);
        return it;
    }

    public Person getOther() {
        return this.other;
    }

    public Optional<String> getRelation() {
        return this.relation;
    }

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public void setOther(final Person parent) {
        this.other = parent;
    }

    public void setRelation(final String relation) {
        this.relation = parseRelation(relation);
    }

    public void setPrivate(final boolean aPrivate) {
        this.isPrivate = aPrivate;
    }

    private static Optional<String> parseRelation(final String relation) {
        if (Objects.isNull(relation)) {
            return Optional.empty();
        }
        if (relation.trim().isEmpty()) {
            return Optional.empty();
        }
        if (relation.trim().equalsIgnoreCase("natural")) {
            return Optional.empty();
        }
        return Optional.of(relation.trim());
    }

    @Override
    public int compareTo(final ParentChildRelation that) {
        return this.other.compareTo(that.other);
    }
}

package nu.mine.mosher.gedcom.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/*
 * Created on 2006-10-08.
 */
public class Partnership implements Comparable<Partnership>, Privatizable {
    private final ArrayList<Event> rEvent;
    private final ArrayList<Person> rChild = new ArrayList<>();
    private final boolean isPrivate;

    private Person partner;

    public Partnership(final ArrayList<Event> rEvent) {
        this(rEvent, false);
    }

    public Partnership(final ArrayList<Event> rEvent, final boolean isPrivate) {
        this.rEvent = new ArrayList<>(rEvent);
        this.isPrivate = isPrivate;

        Collections.sort(this.rEvent);
    }

    public void setPartner(final Person partner) {
        this.partner = partner;
    }

    public Person getPartner() {
        return this.partner;
    }

    public ArrayList<Event> getEvents() {
        return this.rEvent;
    }

    public ArrayList<Person> getChildren() {
        return this.rChild;
    }

    public void addChildren(final Collection<Person> rChildToAdd) {
        this.rChild.addAll(rChildToAdd);

        Collections.sort(this.rChild);
    }

    @Override
    public boolean isPrivate() {
        return this.isPrivate;
    }

    @Override
    public int compareTo(final Partnership that) {
        if (this.rEvent.isEmpty() && that.rEvent.isEmpty()) {
            return 0;
        }
        if (!this.rEvent.isEmpty() && that.rEvent.isEmpty()) {
            return -1;
        }
        if (this.rEvent.isEmpty() && !that.rEvent.isEmpty()) {
            return +1;
        }
        return this.rEvent.get(0).compareTo(that.rEvent.get(0));
    }
}

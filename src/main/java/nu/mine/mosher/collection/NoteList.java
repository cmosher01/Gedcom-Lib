package nu.mine.mosher.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for footnotes.
 * Add notable items in order (keeping track of the returned footnote index for each item).
 * Use the index to display the footnote number (1-n).
 * Retrieve notable by index, or retrieve index by notable.
 * Not thread safe.
 */
public final class NoteList {
    private final Map<Object,Integer> map = new HashMap<>();
    private final List<Object> list = new ArrayList<>();
    private int next = 1;

    public int note(final Object notable) {
        if (notable == null) {
            throw new IllegalArgumentException("A null notable is not allowed.");
        }
        if (this.map.containsKey(notable)) {
            return this.map.get(notable);
        }

        this.map.put(notable, this.next);
        this.list.add(notable);
        return this.next++;
    }

    public Object note(final int i) {
        return this.list.get(i-1);
    }

    public int size() {
        return this.next-1;
    }
}

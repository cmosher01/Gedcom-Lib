package nu.mine.mosher.collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NoteListTest {
    @Test
    public void nominal() {
        final NoteList uut = new NoteList();

        assertEquals(1, uut.note("foobar"));
        assertEquals(2, uut.note("quux"));
        assertEquals(2, uut.note("quux"));
        assertEquals(1, uut.note("foobar"));

        assertEquals("quux", uut.note(2));
        assertEquals("foobar", uut.note(1));

        assertEquals(2, uut.size());
    }

    @Test
    public void empty() {
        final NoteList uut = new NoteList();
        assertEquals(0, uut.size());
    }

    @Test
    public void outOfBounds() {
        final NoteList uut = new NoteList();
        assertThrows(IndexOutOfBoundsException.class, () -> uut.note(999));
    }

    @Test
    public void nullNote() {
        final NoteList uut = new NoteList();
        assertThrows(IllegalArgumentException.class, () -> uut.note(null));
    }
}

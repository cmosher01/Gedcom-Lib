package nu.mine.mosher.gedcom.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AncestryPersonaTest {
    @Test
    void nominal() {
        final Optional<AncestryPersona> uut = AncestryPersona.of("1,7163::33642833");
        assertTrue(uut.isPresent());
        assertEquals(33642833, uut.get().getIndi());
        assertEquals(7163, uut.get().getDb());
    }

    @Test
    void ftm() {
        final Optional<AncestryPersona> uut = AncestryPersona.of("7163:33642833");
        assertTrue(uut.isPresent());
        assertEquals(33642833, uut.get().getIndi());
        assertEquals(7163, uut.get().getDb());
    }
}

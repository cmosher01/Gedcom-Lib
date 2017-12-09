package nu.mine.mosher.gedcom.loader;

import nu.mine.mosher.gedcom.model.Person;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LoaderTest {
    @Test
    public void sortableName() {
        final String actual = Person.buildNameForAlphaSort("Christopher A. /D'Mosher y López/, Junior");
        assertEquals("DMOSHER\u00a0Y\u00a0LÓPEZ, CHRISTOPHER A JUNIOR", actual);
    }
}

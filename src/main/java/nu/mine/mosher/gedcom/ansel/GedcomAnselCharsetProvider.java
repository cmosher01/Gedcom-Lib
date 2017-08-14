package nu.mine.mosher.gedcom.ansel;

import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author Chris Mosher
 */
public class GedcomAnselCharsetProvider extends CharsetProvider {
    private final Charset charset;

    /**
     *
     */
    public GedcomAnselCharsetProvider() {
        this.charset = new GedcomAnselCharset();
    }

    @Override
    public Iterator<Charset> charsets() {
        return Collections.unmodifiableSet(Collections.singleton(this.charset)).iterator();
    }

    @Override
    public Charset charsetForName(final String charsetName) {
        if (!charsetName.equalsIgnoreCase(GedcomAnselCharset.name)) {
            return null;
        }

        return this.charset;
    }
}

package nu.mine.mosher.gedcom.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Represents a MULTIMEDIA_FILE_REFERENCE
 */
public class MultimediaReference
{
    private final String s;
    private final URI uri;

    public MultimediaReference(final String reference) {
        this.s = reference;
        this.uri = tryToParseAsUri(reference);
    }

    private static URI tryToParseAsUri(final String s) {
        try {
            return new URI(s);
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    public String get() {
        return this.s;
    }

    public boolean wasUri() {
        return this.uri != null;
    }

    public URI asUri() {
        if (this.uri == null) {
            throw new UnsupportedOperationException("Multimedia reference could not be converted to a URI: "+this);
        }
        return this.uri;
    }

    @Override
    public boolean equals(final Object object) {
        return this.s.equals(object);
    }

    @Override
    public int hashCode() {
        return this.s.hashCode();
    }

    @Override
    public String toString() {
        return this.s;
    }
}

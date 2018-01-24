package nu.mine.mosher.gedcom.model;

import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * Created by user on 12/10/16.
 */
public class Citation {
    private final Source source;
    private final String page;
    private final String extraText;
    private final Set<MultimediaReference> attachments;

    public Citation(final Source source, final String page, final String extraText, final Set<MultimediaReference> attachments)
    {
        this.source = source;
        this.page = page;
        this.extraText = extraText;
        this.attachments = unmodifiableSet(attachments);
    }

    public Source getSource() { return this.source; }
    public String getPage() { return this.page; }
    public String getExtraText() { return this.extraText; }
    public Set<MultimediaReference> getAttachments() { return this.attachments; }
}

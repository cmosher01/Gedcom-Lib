package nu.mine.mosher.gedcom.model;

/**
 * Created by user on 12/10/16.
 */
public class Citation {
    private final Source source;
    private final String page;
    private final String extraText;

    public Citation(final Source source, final String page, final String extraText)
    {
        this.source = source;
        this.page = page;
        this.extraText = extraText;
    }

    public Source getSource() { return this.source; }
    public String getPage() { return this.page; }
    public String getExtraText() { return this.extraText; }
}

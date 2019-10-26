package nu.mine.mosher.gedcom;

import nu.mine.mosher.logging.Jul;
import nu.mine.mosher.mopper.Optional;

import java.io.File;
import java.nio.charset.Charset;

import static nu.mine.mosher.logging.Jul.log;

@SuppressWarnings({"WeakerAccess", "unused"})
public class GedcomOptions {
    public boolean minimal = false;
    public boolean verbose = false;
    public boolean timestamp = false;
    public boolean utf8 = false;
    public Charset encoding;
    public Integer concToWidth;
    public boolean help = false;
    public File input = null;
    public boolean model = false;

    public void h() {
        help();
    }

    public void help() {
        this.help = true;
        System.err.println("Usage: gedcom-lib [OPTION]... <in.ged >out.ged 2>ged.log");
        System.err.println("Process a GEDCOM file.");
        System.err.println("Options:");
        options();
    }

    protected void options() {
        //@formatter:off
        String s = String.join("\n",
        ""+
        "-h, --help           Print this help",
        "-v, --verbose        Show verbose informational messages",
        "-s, --timestamp      Update .HEAD.DATE.TIME with the current time, in UTC.",
        "-u, --utf8           Convert output to UTF-8 encoding. RECOMMENDED.",
        "-e, --encoding[=ENC] Force input encoding to be ENC; do not detect it.",
        "-c, --conc[=WIDTH]   Rebuild CONC/CONT lines, formatting to maximum width WIDTH"
        );
        //@formatter:on

        System.err.println(s);
    }

    public void v() {
        verbose();
    }

    public void verbose() {
        Jul.verbose(true);
        log().config("Showing verbose log messages.");
    }

    public void minimal() {
        this.minimal = true;
    }

    public void s() {
        timestamp();
    }

    public void timestamp() {
        this.timestamp = true;
    }

    public void u() {
        utf8();
    }

    public void utf8() {
        this.utf8 = true;
    }

    public void e(@Optional final String encoding) {
        encoding(encoding);
    }

    public void encoding(@Optional final String encoding) {
        if (encoding.isEmpty()) {
            this.encoding = Charset.forName("windows-1252");
        } else {
            this.encoding = Charset.forName(encoding);
        }
    }

    public void c(@Optional final String width) {
        conc(width);
    }

    public void conc(@Optional final String width) {
        if (width.isEmpty()) {
            this.concToWidth = 80;
        } else {
            try {
                this.concToWidth = Integer.valueOf(width);
            } catch (final Throwable e) {
                throw new IllegalArgumentException("invalid width: " + width);
            }
        }
        if (this.concToWidth <= 0) {
            throw new IllegalArgumentException("width specified as " + width + ", but must be greater than 0");
        }
    }

    public void input(final String gedcom) {
        this.input = new File(gedcom);
    }

    public void model() {
        this.model = true;
    }
}

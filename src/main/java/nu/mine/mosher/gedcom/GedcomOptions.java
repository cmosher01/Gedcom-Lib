package nu.mine.mosher.gedcom;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.nio.charset.Charset;

import static java.util.Arrays.asList;

public class GedcomOptions {
    private final OptionParser parser = new OptionParser();
    private final OptionSet options;




    // special case for user-defined type-safe arguments:
    private final OptionSpec<CharSetArg> encoding = this.parser.acceptsAll(asList("e", "encoding"),
        "Forces input encoding to be ENC; do not detect it.")
        .withRequiredArg().ofType(CharSetArg.class).describedAs("ENC").defaultsTo(new CharSetArg());

    {
        this.parser.acceptsAll(asList("h", "help"),
            "Prints this help message.")
            .forHelp();

        this.parser.acceptsAll(asList("v", "verbose"),
            "Show verbose informational messages.");

//        this.parser.acceptsAll(asList("s", "timestamp"),
//            "Updates .HEAD.DATE.TIME with the current time, in UTC.");
        this.parser.acceptsAll(asList("u", "utf8"),
            "Converts output to UTF-8 encoding.");
        this.parser.acceptsAll(asList("c", "conc"),
            "Rebuilds CONC/CONT lines to WIDTH")
            .withOptionalArg().ofType(Integer.class).describedAs("WIDTH");
    }





    public GedcomOptions(final String... args) throws OptionException {
        this.options = this.parser.parse(args);
    }

    public boolean help() {
        final boolean help = this.options.has("help");
        if (help) {
            try {
                this.parser.printHelpOn(System.out);
            } catch (final Throwable e) {
                throw new IllegalStateException(e);
            }
        }
        return help;
    }

    public OptionSet get() {
        return this.options;
    }





    // special case for user-defined type-safe argument,
    // with default value only if option is specified
    public Charset encoding() {
        return this.options.has("encoding") ? this.encoding.value(this.options).getCharset() : null;
    }


    public static class CharSetArg {
        private final Charset charset;

        public CharSetArg() {
            this("windows-1252");
        }

        public CharSetArg(final String charsetName) throws IllegalArgumentException {
            this.charset = Charset.forName(charsetName);
        }

        public Charset getCharset() {
            return this.charset;
        }

        @Override
        public String toString() {
            return this.charset.toString();
        }
    }
}

package nu.mine.mosher.gedcom;

import joptsimple.*;

import java.nio.charset.Charset;

import static java.util.Arrays.asList;

public class GedcomOptions {
    private final OptionParser parser;
    private OptionSet options;


    // special case for user-defined type-safe arguments:
    private final OptionSpec<CharSetArg> encoding;
    private final OptionSpec<Integer> conc;


    public GedcomOptions(final OptionParser parser) {
        this.parser = parser;
        this.parser.acceptsAll(asList("h", "help"), "Prints this help message.")
            .forHelp();

        this.parser.acceptsAll(asList("v", "verbose"), "Show verbose informational messages.");

        this.parser.acceptsAll(asList("s", "timestamp"), "Updates .HEAD.DATE.TIME with the current time, in UTC.");

        this.parser.acceptsAll(asList("u", "utf8"), "Converts output to UTF-8 encoding. RECOMMENDED.");

        this.encoding = this.parser.acceptsAll(asList("e", "encoding"),
            "Forces input encoding to be ENC; do not detect it.")
            .withRequiredArg()
            .ofType(CharSetArg.class)
            .describedAs("ENC")
            .defaultsTo(new CharSetArg());

        this.conc = this.parser.acceptsAll(asList("c", "conc"), "Rebuilds CONC/CONT lines to WIDTH")
            .withOptionalArg()
            .ofType(Integer.class)
            .describedAs("WIDTH")
            .withValuesConvertedBy(new ValueConverter<Integer>() {
                @Override
                public Integer convert(String value) throws OptionException {
                    int w = Integer.parseInt(value, 10);
                    if (w <= 0) {
                        throw new IllegalStateException("WIDTH must be greater than 0.");
                    }
                    return Integer.valueOf(w);
                }

                @Override
                public Class<? extends Integer> valueType() {
                    return Integer.class;
                }

                @Override
                public String valuePattern() {
                    return null;
                }
            });
    }


    public GedcomOptions parse(final String... args) throws OptionException {
        this.options = this.parser.parse(args);
        return this;
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
        return this.options.has("encoding") ? this.encoding.value(this.options)
            .getCharset() : null;
    }

    public Integer concWidth() {
        return this.conc.value(this.options);
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

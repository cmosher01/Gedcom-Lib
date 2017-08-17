package nu.mine.mosher.gedcom;


import joptsimple.OptionException;
import joptsimple.OptionParser;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.nio.charset.Charset;

import static nu.mine.mosher.gedcom.GedcomMinimal.minimal;
import static nu.mine.mosher.logging.Jul.log;
import static nu.mine.mosher.logging.Jul.verbose;


/**
 * Handles reading in a GEDCOM file and parsing into an internal representation.
 *
 * @author Christopher Alan Mosher
 */
public final class Gedcom {
    public interface Processor {
        boolean process(GedcomTree tree);
    }

    private final GedcomOptions options;
    private final Processor proc;


    public static void main(final String... args) throws InvalidLevel, IOException, OptionException {
        final GedcomOptions options = new GedcomOptions(new OptionParser()).parse(args);

        new Gedcom(options, g -> true).main();

        System.out.flush();
        System.err.flush();
    }


    public Gedcom(final GedcomOptions options, final Processor proc) {
        this.options = options;
        this.proc = proc;
    }


    public void main() throws InvalidLevel, IOException {
        verbose(this.options.get().has("verbose"));
        log().config("Showing verbose log messages.");

        if (this.options.help()) {
            return;
        }


        final GedcomTree tree;

        final BufferedInputStream streamInput = getStandardInput();
        final int cIn = streamInput.available();
        log().finest("Estimating standard input has available byte count: " + Integer.toString(cIn));
        if (cIn <= 0) {
            log().warning("No input GEDCOM detected. Generating MINIMAL GEDCOM file.");
            tree = minimal(this.options.encoding());
        } else {
            tree = readFile(streamInput, this.options.encoding());
        }

        if (this.options.get().has("conc")) {
            log().info("Concatenating CONC/CONT lines.");
            new GedcomConcatenator(tree).concatenate();
        }


        if (this.proc.process(tree)) {
            if (this.options.get().has("timestamp")) {
                tree.timestamp();
            }

            if (this.options.get().has("conc")) {
                final Integer width = this.options.concWidth();
                if (width != null) {
                    log().info("Rebuilding CONC/CONT lines to specified width: " + width);
                    tree.setMaxLength(width);
                } else {
                    log().info("Rebuilding CONC/CONT lines to guessed width " + Integer.toString(tree.getMaxLength()));
                }
                new GedcomUnconcatenator(tree).unconcatenate();
            }

            if (this.options.get().has("utf8")) {
                log().info("Converting to UTF-8 encoding for output.");
                tree.setCharset(Charset.forName("UTF-8"));
            }

            writeFile(tree, getStandardOutput());
        }
    }


    public static GedcomTree readFile(final BufferedInputStream streamInput) throws IOException, InvalidLevel {
        return readFile(streamInput, null);
    }

    public static GedcomTree readFile(final BufferedInputStream streamInput, Charset charsetForce) throws
        IOException, InvalidLevel {
        if (charsetForce == null) {
            charsetForce = new GedcomCharsetDetector(streamInput).detect();
        } else {
            log().info("Forcing input character encoding to " + charsetForce.name());
        }

        final GedcomParser parser = new GedcomParser(new BufferedReader(new InputStreamReader(streamInput, charsetForce)));
        final GedcomTree tree = new GedcomTree();
        tree.readFrom(parser);
        tree.setCharset(charsetForce);
        return tree;
    }

    public static void writeFile(final GedcomTree tree, final BufferedOutputStream streamOutput) throws IOException {
        final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(streamOutput, tree.getCharset()));
        out.write(tree.toString());
        out.flush();
    }


    private static BufferedInputStream getStandardInput() {
        return new BufferedInputStream(new FileInputStream(FileDescriptor.in));
    }

    private static BufferedOutputStream getStandardOutput() {
        return new BufferedOutputStream(new FileOutputStream(FileDescriptor.out));
    }
}

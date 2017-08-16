package nu.mine.mosher.gedcom;


import joptsimple.OptionException;
import joptsimple.OptionParser;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.nio.charset.Charset;

import static nu.mine.mosher.logging.Jul.log;
import static nu.mine.mosher.logging.Jul.verbose;


/**
 * Handles reading in a GEDCOM file and parsing into an internal representation.
 *
 * @author Christopher Alan Mosher
 */
public final class Gedcom {
    public interface Processor {
        void process(GedcomTree tree);
    }

    private final GedcomOptions options;
    private final Processor proc;



    public static void main(final String... args) throws InvalidLevel, IOException, OptionException {
        final GedcomOptions options = new GedcomOptions(new OptionParser()).parse(args);

        new Gedcom(options, g -> {}).main();

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
        if (this.options.get().has("encoding")) {
            tree = readFile(getStandardInput(), this.options.encoding());
        } else {
            tree = readFile(getStandardInput());
        }

        if (this.options.get().has("conc")) {
            log().info("Concatenating CONC/CONT lines.");
            new GedcomConcatenator(tree).concatenate();
        }



        this.proc.process(tree);



        if (this.options.get().has("conc")) {
            final Integer width = this.options.concWidth();
            if (width != null) {
                log().info("Rebuilding CONC/CONT lines to specified width: "+width);
                tree.setMaxLength(width);
            } else {
                log().info("Rebuilding CONC/CONT lines to guessed width "+Integer.toString(tree.getMaxLength()));
            }
            new GedcomUnconcatenator(tree).unconcatenate();
        }

        if (this.options.get().has("utf8")) {
            log().info("Converting to UTF-8 encoding for output.");
            tree.setCharset(Charset.forName("UTF-8"));
        }

        writeFile(tree, getStandardOutput());
    }



    public static GedcomTree readFile(final BufferedInputStream streamInput) throws IOException, InvalidLevel {
        return readFile(streamInput, null);
    }

    public static GedcomTree readFile(final BufferedInputStream streamInput, Charset charsetForce) throws IOException, InvalidLevel {
        final int cIn = streamInput.available();
        log().finest("Estimating standard input has available byte count: "+Integer.toString(cIn));
        if (cIn <= 0) {
            log().warning("No input GEDCOM detected. Generating MINIMAL GEDCOM file.");
            return minimal();
        }

        if (charsetForce == null) {
            charsetForce = new GedcomCharsetDetector(streamInput).detect();
        }
        final GedcomParser parser = new GedcomParser(new BufferedReader(new InputStreamReader(streamInput, charsetForce)));
        final GedcomTree tree = new GedcomTree();
        tree.readFrom(parser);
        tree.setCharset(charsetForce);
        return tree;
    }

    /*
     * 0 HEAD
     * 1 CHAR UTF-8
     * 1 GEDC
     * 2 VERS 5.5.1
     * 2 FORM LINEAGE-LINKED
     * 1 SOUR MINIMAL
     * 1 SUBM @M0@
     * 0 @M0@ SUBM
     * 1 NAME MINIMAL
     * 0 TRLR
     */
    public static final String SUMB_ID = "M0";
    private static GedcomTree minimal() {
        final GedcomTree tree = new GedcomTree();

        final TreeNode<GedcomLine> head = new TreeNode<>(GedcomLine.createEmpty(0, GedcomTag.HEAD));
        tree.getRoot().addChild(head);

        head.addChild(new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.CHAR)));
        tree.setCharset(Charset.forName("UTF-8"));

        final TreeNode<GedcomLine> gedc = new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.GEDC));
        head.addChild(gedc);
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.VERS, "5.5.1")));
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.FORM, "LINEAGE-LINKED")));

        head.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.SOUR, "MINIMAL")));
        head.addChild(new TreeNode<>(GedcomLine.createPointer(1, GedcomTag.SUBM, SUMB_ID)));

        final TreeNode<GedcomLine> subm = new TreeNode<>(GedcomLine.createEmptyId(0, SUMB_ID, GedcomTag.SUBM));
        tree.getRoot().addChild(subm);

        subm.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.NAME, "MINIMAL")));

        tree.getRoot().addChild(new TreeNode<>(GedcomLine.createEmpty(0, GedcomTag.TRLR)));

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

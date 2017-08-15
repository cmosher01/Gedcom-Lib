package nu.mine.mosher.gedcom;


import joptsimple.OptionException;
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
    private final GedcomOptions options;

    public static void main(final String... args) throws InvalidLevel, IOException, OptionException {
        new Gedcom(new GedcomOptions(args)).main();

        System.out.flush();
        System.err.flush();
    }

    private Gedcom(final GedcomOptions options) {
        this.options = options;
    }

    private void main() throws InvalidLevel, IOException {
        verbose(this.options.get().has("verbose"));
        log().config("Showing verbose log messages.");

        if (this.options.help()) {
            return;
        }

        final GedcomTree tree = readFile(getStandardInput());

        if (this.options.get().has("utf8")) {
            log().config("Converting to UTF-8 encoding for output.");
            tree.setCharset(Charset.forName("UTF-8"));
        }

        writeFile(tree, getStandardOutput());
    }

    public static GedcomTree readFile(final BufferedInputStream streamInput) throws IOException, InvalidLevel {
        final Charset deduced = new GedcomCharsetDetector(streamInput).detect();
        final GedcomParser parser = new GedcomParser(new BufferedReader(new InputStreamReader(streamInput, deduced)));
        final GedcomTree tree = new GedcomTree();
        parseLines(parser, tree);

        new GedcomConcatenator(tree).concatenate();

        tree.setCharset(deduced);
        return tree;
    }


    private static void parseLines(final GedcomParser parser, final GedcomTree tree) throws InvalidLevel {
        int i = 0;
        for (final GedcomLine line : parser) {
            ++i;
            log().finest("parsed GEDCOM line: " + line);
            try {
                tree.appendLine(line);
            } catch (final InvalidLevel err) {
                log().warning("at line number " + i); // TODO improve error reporting
                throw err;
            }
        }
    }

    public static void writeFile(final GedcomTree tree, final BufferedOutputStream streamOutput) throws IOException {
        new GedcomUnconcatenator(tree).unconcatenate();

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


    //    public static void writeFile(GedcomTree gt, BufferedWriter out) throws IOException {
//        final GedcomUnconcatenator concat = new GedcomUnconcatenator(gt, gt.getMaxLength());
//        concat.unconcatenate();
//        out.write(gt.toString());
//        out.flush();
//    }
//
//    public static GedcomTree parseFile(File in, Charset charset) throws IOException, InvalidLevel {
//        return parseFile(in, charset, true);
//    }
//
//    public static GedcomTree parseFile(File in, Charset charset, boolean removeConcCont) throws IOException, InvalidLevel {
//        return readTree(in, charset, removeConcCont);
//    }
//
//    protected static GedcomTree readTree(File fileIn, Charset charset, boolean removeConcCont) throws UnsupportedEncodingException, FileNotFoundException, InvalidLevel {
//        BufferedReader in = null;
//        try {
//            in = new BufferedReader(new InputStreamReader(new FileInputStream(fileIn), charset));
//            return readTree(in, removeConcCont);
//        } finally {
//            if (in != null) {
//                try {
//                    in.close();
//                } catch (Throwable ignore) {
//                    ignore.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public static GedcomTree readTree(final Reader reader) throws InvalidLevel {
//        return readTree(reader, true);
//    }
//
//    public static GedcomTree readTree(final Reader reader, boolean removeConcCont) throws InvalidLevel {
//        final GedcomParser parser = new GedcomParser(new BufferedReader(reader));
//
//        final GedcomTree tree = new GedcomTree();
//        int i = 0;
//        for (final GedcomLine line : parser) {
//            ++i;
//            try {
//                tree.appendLine(line);
//            } catch (final InvalidLevel err) {
//                System.err.println("at line number " + i); // TODO improve error reporting
//                throw err;
//            }
//        }
//
//        if (removeConcCont) {
//            final GedcomConcatenator concat = new GedcomConcatenator(tree);
//            concat.concatenate();
//        }
//
//        return tree;
//    }
//
//    public static Charset getCharset(final File f) throws IOException {
//        // TODO fix this, so we can get the charset without needing
//        // to close and re-open the file (which prevents reading
//        // from stdin).
//        // maybe use ICU4J
//        InputStream in = null;
//        try {
//            in = new FileInputStream(f);
//            return Charset.forName(guessGedcomCharset(in));
//        } finally {
//            if (in != null) {
//                try {
//                    in.close();
//                } catch (Throwable ignore) {
//                    ignore.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public static String guessGedcomCharset(final InputStream in) throws IOException {
//        // read first four bytes of input stream
//        int b0 = in.read();
//        if (b0 == -1) return "";
//        int b1 = in.read();
//        if (b1 == -1) return "";
//        int b2 = in.read();
//        if (b2 == -1) return "";
//        int b3 = in.read();
//        if (b3 == -1) return "";
//
//        // build a word from the first two bytes,
//        // assuming little-endian byte order
//        int w0 = 0;
//        w0 |= b1;
//        w0 <<= 8;
//        w0 |= b0;
//
//        // build a longword from the first four bytes,
//        // assuming little-endian byte order
//        int i0 = 0;
//        i0 |= b3;
//        i0 <<= 8;
//        i0 |= b2;
//        i0 <<= 8;
//        i0 |= b1;
//        i0 <<= 8;
//        i0 |= b0;
//
//        if (i0 == 0x0000feff || i0 == 0x00000030) {
//            return "UTF-32";
//        }
//
//        if (i0 == 0xfffe0000 || i0 == 0x30000000) {
//            return "UTF-32";
//        }
//
//        if (w0 == 0x0000feff || w0 == 0x00000030) {
//            return "UTF-16";
//        }
//
//        if (w0 == 0x0000fffe || w0 == 0x00003000) {
//            return "UTF-16";
//        }
//
//        if (b0 == 0x000000ef && b1 == 0x000000bb && b2 == 0x000000bf) {
//            return "UTF-8";
//        }
//
//        BufferedReader bin = new BufferedReader(new InputStreamReader(in, "US-ASCII"));
//        bin.readLine(); // ignore rest of header line
//
//        String s = bin.readLine();
//        while (s != null && s.length() > 0 && s.charAt(0) != '0') {
//            if (s.startsWith("1 CHAR")) {
//                s = s.toUpperCase();
//                if (s.indexOf("WIN", 6) >= 0) {
//                    return "windows-1252";
//                }
//                if (s.indexOf("ANSI", 6) >= 0) {
//                    return "windows-1252";
//                }
//                if (s.indexOf("UTF-8", 6) >= 0) {
//                    return "UTF-8";
//                }
//                if (s.indexOf("DOS", 6) >= 0) {
//                    return "Cp850";
//                }
//                if (s.indexOf("PC", 6) >= 0) {
//                    return "Cp850";
//                }
//                if (s.indexOf("OEM", 6) >= 0) {
//                    return "Cp850";
//                }
//                if (s.indexOf("ASCII", 6) >= 0) {
//                    return "windows-1252";
//                }
//                if (s.indexOf("MAC", 6) >= 0) {
//                    return "MacRoman";
//                }
//                if (s.indexOf("ANSEL", 6) >= 0) {
//                    return "x-gedcom-ansel";
//                }
    /////// Also:  UNICODE --> UTF-16
//            }
//            s = bin.readLine();
//        }
//
//        return "windows-1252";
//    }
}

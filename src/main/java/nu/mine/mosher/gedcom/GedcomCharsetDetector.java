package nu.mine.mosher.gedcom;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nu.mine.mosher.logging.Jul.log;

public class GedcomCharsetDetector {
    private static final Logger log = Logger.getLogger("");

    private final BufferedInputStream gedcom;

    public GedcomCharsetDetector(final BufferedInputStream gedcom) {
        this.gedcom = gedcom;
    }

    public Charset detect() throws IOException {
        checkMagicBytes(this.gedcom);

        String use = "windows-1252";

        final String headChar = findHeadCharValue(this.gedcom);

        String interpretedDeclaredEncoding = "";
        if (!headChar.isEmpty()) {
            log.info("Found .HEAD.CHAR declared character encoding: " + headChar);
            use = headChar;
            if (isLegalJavaCharset(headChar)) {
                final String canonicalCharsetName = Charset.forName(headChar).name();
                log.info(headChar + " interpreted by Java as " + canonicalCharsetName);
                use = canonicalCharsetName;
            } else {
                log.info(headChar + " cannot be interpreted by Java");
            }

            interpretedDeclaredEncoding = interpretHeadChar(headChar);
            if (!interpretedDeclaredEncoding.isEmpty()) {
                final String canonicalCharsetName = Charset.forName(interpretedDeclaredEncoding).name();
                log.info(headChar + " interpreted heuristically as " + interpretedDeclaredEncoding + " (by Java: " + canonicalCharsetName + ")");
                use = canonicalCharsetName;
            } else {
                log.warning(headChar + " unknown GEDCOM character encoding charset value");
            }
        }


        final CharsetDetector det = new CharsetDetector();
        det.enableInputFilter(true);

        final Set<String> icu4jDetectable = new HashSet<>(Arrays.asList(CharsetDetector.getAllDetectableCharsets()));
        if (icu4jDetectable.contains(use)) {
            log.info("Using " + use + " as declared character encoding hint for ICU4J detection.");
            det.setDeclaredEncoding(use);
        } else {
            use = interpretedDeclaredEncoding;
        }

        boolean skip = false;
        byte[] sample = getSampleBytes(this.gedcom);
        if (sample.length > 0) {
            log.info("Using sample " + sample.length + " bytes for ICU4J detection.");
            det.setText(sample);
        } else {
            log.warning("Could not find any bytes out of range 0-127. Finding NAME records for ICU4J detection.");
            sample = getSampleNameBytes(this.gedcom);
            if (sample.length > 0) {
                log.info("Using sample " + sample.length + " bytes from NAME records for ICU4J detection.");
                det.setText(sample);
            } else {
                log.warning("Could not find any NAME records for ICU4J detection.");
                skip = true;
            }
        }

        if (!skip) {
            final CharsetMatch in = det.detect();
            log.info("ICU4J detected character encoding " + in.getName() + ", with " + Integer.toString(in.getConfidence()) + "% confidence.");

            final int confidenceThreshold;
            if (icu4jDetectable.contains(interpretedDeclaredEncoding)) {
                confidenceThreshold = 52;
            } else {
                confidenceThreshold = 98;
            }
            if (in.getConfidence() >= confidenceThreshold) {
                log.info("ICU4J confidence is above threshold of "+Integer.toString(confidenceThreshold)+"%.");
                use = in.getName();
            } else {
                log.warning("ICU4J confidence is BELOW threshold of "+Integer.toString(confidenceThreshold)+"%; ignoring.");
            }
        }

        if (!isLegalJavaCharset(use)) {
            use = "UTF-8";
        }

        final Charset deduced = Charset.forName(use);
        log.info("Using charset " + deduced.name());
        return deduced;
    }

    private static boolean isLegalJavaCharset(final String possbileCharSetName) {
        try {
            return Charset.isSupported(possbileCharSetName);
        } catch (final Throwable ignored) {
            log().warning("Cannot find character encoding ("+possbileCharSetName+") to use for GEDCOM file.");
            return false;
        }
    }

    /*
    Many taken from:
    https://www.tamurajones.net/GEDCOMCharacterEncodings.xhtml
    "Modern Software Experience" "GEDCOM Character Encodings"
     */
    private static final Map<String, String> mapChar = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("IBMPC", "Cp437");
        put("IBM-PC", "Cp437");
        put("IBM", "Cp437");
        put("PC", "Cp437");
        put("OEM", "Cp437");

        put("MSDOS", "Cp850");
        put("MS-DOS", "Cp850");
        put("DOS", "Cp850");
        put("IBM DOS", "Cp850");

        put("ANSI", "windows-1252");
        put("WINDOWS", "windows-1252");
        put("WIN", "windows-1252");
        put("IBM WINDOWS", "windows-1252");
        put("IBM_WINDOWS", "windows-1252");

        put("ASCII", "windows-1252");
        put("CP1252", "windows-1252");

        put("ISO-8859-1", "windows-1252");
        put("ISO8859-1", "windows-1252");
        put("ISO-8859", "windows-1252");
        put("LATIN1", "windows-1252");

        put("MAC", "MacRoman");
        put("MACINTOSH", "MacRoman");

        put("UNICODE", "UTF-16");
        put("UTF-8", "UTF-8");

        put("ANSEL", "x-gedcom-ansel");
    }});

    private static String interpretHeadChar(String headChar) {
        headChar = headChar.toUpperCase();

        if (!mapChar.containsKey(headChar)) {
            return "";
        }

        return mapChar.get(headChar);
    }

    private static byte[] getSampleBytes(final BufferedInputStream in) throws IOException {
        int cint = 1 << 23;
        byte[] interesting = new byte[cint];

        in.mark(cint);

        int iint = 0;

        final List<Byte> r = new ArrayList<>(1 << 10);
        while (findInterestingLine(in, r)) {
            for (final byte b : r) {
                interesting[iint++] = b;
                if (cint <= iint) {
                    log.warning("Hit internal buffer length.");
                    break;
                }
            }
            r.clear();
            if (cint <= iint) {
                log.warning("Hit internal buffer length.");
                break;
            }
        }

        in.reset();

        return Arrays.copyOfRange(interesting, 0, iint);
    }

    private static boolean findInterestingLine(final BufferedInputStream in, final List<Byte> r) throws IOException {
        final List<Byte> candidate = new ArrayList<>(1 << 10);
        boolean got = readLine(in, candidate);
        while (got && candidate.stream().noneMatch(b -> b < 0)) {
            candidate.clear();
            got = readLine(in, candidate);
        }
        if (got) {
            r.addAll(candidate);
        }
        return got;
    }


    private static byte[] getSampleNameBytes(final BufferedInputStream in) throws IOException {
        int cint = 1 << 23;
        byte[] interesting = new byte[cint];

        in.mark(cint);

        int iint = 0;

        final List<Byte> r = new ArrayList<>(1 << 10);
        while (findNameLine(in, r)) {
            for (final byte b : r) {
                interesting[iint++] = b;
                if (cint <= iint) {
                    log.warning("Hit internal buffer length.");
                    break;
                }
            }
            r.clear();
            if (cint <= iint) {
                log.warning("Hit internal buffer length.");
                break;
            }
        }

        in.reset();

        return Arrays.copyOfRange(interesting, 0, iint);
    }

    private static boolean findNameLine(final BufferedInputStream in, final List<Byte> r) throws IOException {
        final List<Byte> candidate = new ArrayList<>(1 << 10);
        boolean got = readLine(in, candidate);
        while (got && !isNameLine(candidate)) {
            candidate.clear();
            got = readLine(in, candidate);
        }
        if (got) {
            r.addAll(candidate);
        }
        return got;
    }

    private static boolean isNameLine(final List<Byte> candidate) {
        return candidate.size() > 5 && candidate.get(0) == '1' && candidate.get(2) == 'N' && candidate.get(3) == 'A' && candidate.get(4) == 'M' && candidate.get(5) == 'E';
    }

    private static boolean readLine(final BufferedInputStream in, final List<Byte> r) throws IOException {
        assert r.size() == 0;
        int b = in.read();
        while (b != -1 && b != 0x0a && b != 0x0d) {
            r.add((byte) b);
            b = in.read();
        }
        return !(b == -1 && r.size() == 0);
    }



    private static final String DUMB_GUESS_UNIVERSAL_CHARSET_NAME = "windows-1252";
    private static final Charset DUMB_GUESS_UNIVERSAL_CHARSET = Charset.forName(DUMB_GUESS_UNIVERSAL_CHARSET_NAME);
    private static final Pattern HEAD_LINE = Pattern.compile("0\\s+HEAD.*");
    private static final Pattern CHAR_LINE = Pattern.compile("1\\s+CHAR\\s+(.*)");
    private static final Pattern REC_LINE = Pattern.compile("0\\s+.*");

    private static final int START = 0;
    private static final int IN_HEAD = 1;
    private static final int OUT_HEAD = 2;

    private static String findHeadCharValue(final BufferedInputStream buf) throws IOException {
        buf.mark(1 << 24);
        try {
            final BufferedReader inspector = new BufferedReader(new InputStreamReader(buf, DUMB_GUESS_UNIVERSAL_CHARSET));

            int state = START;
            for (String line = inspector.readLine(); line != null; line = inspector.readLine()) {
                line = line.trim();
                switch (state) {
                    case START: {
                        if (HEAD_LINE.matcher(line).matches()) {
                            state = IN_HEAD;
                        }
                    }
                    break;
                    case IN_HEAD: {
                        if (REC_LINE.matcher(line).matches()) {
                            state = OUT_HEAD;
                        } else {
                            final Matcher matcher = CHAR_LINE.matcher(line);
                            if (matcher.matches()) {
                                return matcher.group(1).trim();
                            }
                        }
                    }
                    break;
                    case OUT_HEAD: {
                        return "";
                    }
                }
            }
            return "";
        } finally {
            buf.reset();
        }
    }

    private static void checkMagicBytes(final BufferedInputStream buf) throws IOException {
        buf.mark(1 << 24);
        try {
            boolean ok = false;
            final byte[] rb = new byte[7];
            if (buf.read(rb,0,7) >= 7) {
                ok = rb[0] == '0' && rb[1] == ' ' && rb[2] == 'H' && rb[3] == 'E' && rb[4] == 'A' && rb[5] == 'D' &&
                    (rb[6] == '\n' || rb[6] == '\r');
                if (!ok) {
                    // TODO read more bytes and check for UTF-16 (BE and LE)
                    // might as well check for UTF-32, as well
                    // and UTF-8
                    // all with our without BOM
//                    ok = (rb[0] == 0xfe && rb[1] == 0xff &&
//                        rb[2] == 0 && rb[3] == '0' &&
//                        rb[4] == 0 && rb[1] == ' ' &&
//                        rb[6] == 0 && rb[2] == 'H' &&
//                        rb[8] == 0 && rb[3] == 'E' &&
//                        rb[10] == 0 && rb[4] == 'A' &&
//                        rb[2] == 0 && rb[5] == 'D' &&
//                        (rb[2] == 0 && (rb[6] == '\n' || rb[6] == '\r')));
                }
            }
            if (!ok) {
                log().severe("Input file does not start with 0 HEAD line, and therefore probably is not a GEDCOM file.");
            }
        } finally {
            buf.reset();
        }
    }
}

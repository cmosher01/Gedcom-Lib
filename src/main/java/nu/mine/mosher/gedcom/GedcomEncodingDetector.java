package nu.mine.mosher.gedcom;

import org.mozilla.universalchardet.UnicodeBOMInputStream;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GedcomEncodingDetector {
    private static final Logger log = Logger.getLogger("");

//    public static void main(final String... args) throws IOException {
//        final BufferedInputStream in = getStandardInput();
//
//        new GedcomEncodingDetector(in).detect();
//
//        System.out.flush();
//        System.err.flush();
//    }

    private final BufferedInputStream gedcom;

    public GedcomEncodingDetector(final BufferedInputStream gedcom) {
        this.gedcom = gedcom;
    }

    public Charset detect() throws IOException {
        final Optional<Charset> charsetDetected = detectCharsetDefault(this.gedcom);

        if (charsetDetected.isPresent()) {
            log.info(String.format("First guess at character encoding: %s", charsetDetected.get().displayName()));
        } else {
            log.info("First guess at character encoding failed.");
            log.info(String.format("First guess at character encoding defaulting to: %s", Charset.defaultCharset().displayName()));
        }

        final Optional<Charset> charsetDeclared = detectCharsetDeclared(this.gedcom, charsetDetected.orElse(Charset.defaultCharset()));
        if (charsetDeclared.isPresent()) {
            log.info(String.format("Found declared character encoding: %s", charsetDeclared.get().displayName()));
        }

        final Charset charsetResult;
        if (charsetDetected.isPresent() && charsetDeclared.isPresent()) {
            charsetResult = resolveConflictingCharsets(charsetDetected.get(), charsetDeclared.get());
        } else if (charsetDetected.isPresent()) {
            charsetResult = charsetDetected.get();
        } else if (charsetDeclared.isPresent()) {
            charsetResult = charsetDeclared.get();
        } else {
            charsetResult = Charset.defaultCharset();
        }

        log.info(String.format("Will use character encoding: %s", charsetResult.displayName()));

        return charsetResult;
    }

    private Charset resolveConflictingCharsets(final Charset detected, final Charset declared) {
        if (detected.equals(declared)) {
            return detected;
        }
        if (isDetectionReliable(detected)) {
            return detected;
        }
        return declared;
    }

    private boolean isDetectionReliable(final Charset detected) {
        return detected.name().contains("UTF");
    }

    private static Optional<Charset> detectCharsetDefault(final BufferedInputStream gedcomStream) throws IOException {
        final int cBytesToCheck = 64 * 1024;
        gedcomStream.mark(cBytesToCheck);
        try {
            return tryDetectCharsetDefault(gedcomStream, cBytesToCheck);
        } finally {
            gedcomStream.reset();
        }
    }

    private static Optional<Charset> tryDetectCharsetDefault(final BufferedInputStream gedcomStream, final int cBytesToCheck) throws IOException {
        final UniversalDetector detector = new UniversalDetector();

        final int cBufferSize = 4 * 1024;

        final byte[] buf = new byte[cBufferSize];

        boolean first = true;
        int sane = cBytesToCheck / cBufferSize;
        for (int nread = gedcomStream.read(buf); nread > 0 && --sane > 0; nread = gedcomStream.read(buf)) {
            if (first && nread >= 4) {
                final Optional<Charset> prescreened = prescreen(buf);
                if (prescreened.isPresent()) {
                    return prescreened;
                }
            }
            first = false;
            detector.handleData(buf, 0, nread);
        }

        detector.dataEnd();

        return charsetForName(detector);
    }

    /*
    Universal detector has trouble detecting UTF-16 without BOM.
     */
    private static Optional<Charset> prescreen(final byte[] b) {
        if (b[0]==0x30 && b[1]==0x00 && b[2]==0x20 && b[3]==0x00) {
            return Optional.of(StandardCharsets.UTF_16LE);
        }
        if (b[0]==0x00 && b[1]==0x30 && b[2]==0x00 && b[3]==0x20) {
            return Optional.of(StandardCharsets.UTF_16BE);
        }
        return Optional.empty();
    }

    private static Optional<Charset> charsetForName(final UniversalDetector detector) {
        final String c = detector.getDetectedCharset();
        if (Objects.isNull(c)) {
            log.info("Character detector returned null.");
            return Optional.empty();
        }
        if (c.isEmpty()) {
            log.info("Character detector returned empty string.");
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(c));
        } catch (final Exception ignore) {
            log.log(Level.WARNING, String.format("Character detector returned invalid Charset name: %s", c), ignore);
            return Optional.empty();
        }
    }

    private static Optional<Charset> detectCharsetDeclared(final BufferedInputStream gedcomStream, final Charset charsetBestGuess) throws IOException {
        final int cBytesToCheck = 32 * 1024;
        gedcomStream.mark(cBytesToCheck);
        try {
            return tryDetectCharsetDeclared(gedcomStream, cBytesToCheck, charsetBestGuess);
        } finally {
            gedcomStream.reset();
        }
    }

    private static Optional<Charset> tryDetectCharsetDeclared(final BufferedInputStream gedcomStream, final int cBytesToCheck, final Charset charsetBestGuess) throws IOException {
        final String headChar = interpretHeadChar(tryDetectCharsetNameDeclared(gedcomStream, cBytesToCheck, charsetBestGuess));
        if (headChar.isEmpty()) {
            log.warning("Did not recognize that value for CHAR.");
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(headChar));
        } catch (final Exception ignore) {
            log.log(Level.WARNING, String.format("Invalid Charset name: %s", headChar), ignore);
            return Optional.empty();
        }
    }

    private static final int START = 0;
    private static final int IN_HEAD = 1;
    private static final int OUT_HEAD = 2;
    private static final Pattern HEAD_LINE = Pattern.compile("0\\s+HEAD.*");
    private static final Pattern CHAR_LINE = Pattern.compile("1\\s+CHAR\\s+(.*)");
    private static final Pattern REC0_LINE = Pattern.compile("0\\s+.*");

    private static String tryDetectCharsetNameDeclared(final BufferedInputStream gedcomStream, final int cBytesToCheck, final Charset charsetBestGuess) throws IOException {
        final BufferedReader gedcomReader = createNiceLineReader(gedcomStream, charsetBestGuess);
        int sane = cBytesToCheck / 2; // just to be safe
        int state = START;
        for (String line = gedcomReader.readLine(); line != null && sane > 0; line = gedcomReader.readLine()) {
            sane -= (line.length()+2);
            line = line.trim();
            switch (state) {
                case START: {
                    if (HEAD_LINE.matcher(line).matches()) {
                        log.fine("Found HEAD line. Good.");
                        state = IN_HEAD;
                    }
                }
                break;
                case IN_HEAD: {
                    if (REC0_LINE.matcher(line).matches()) {
                        state = OUT_HEAD;
                    } else {
                        final Matcher matcher = CHAR_LINE.matcher(line);
                        if (matcher.matches()) {
                            final String charsetNameDeclared = matcher.group(1).trim();
                            log.info(String.format("Found CHAR line with value: %s", charsetNameDeclared));
                            return charsetNameDeclared.toUpperCase();
                        }
                    }
                }
                break;
                case OUT_HEAD: {
                    log.warning("Could not find CHAR line.");
                    return "";
                }
            }
        }
        log.warning("Could not find HEAD line.");
        return "";
    }

    private static BufferedReader createNiceLineReader(final BufferedInputStream gedcomStream, final Charset charsetBestGuess) throws IOException {
        return new BufferedReader(new InputStreamReader(new UnicodeBOMInputStream(gedcomStream), charsetBestGuess));
    }

    private static BufferedInputStream getStandardInput() {
        return new BufferedInputStream(new FileInputStream(FileDescriptor.in));
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

    private static String interpretHeadChar(final String headChar) {
        if (!mapChar.containsKey(headChar)) {
            return "";
        }

        return mapChar.get(headChar);
    }
}

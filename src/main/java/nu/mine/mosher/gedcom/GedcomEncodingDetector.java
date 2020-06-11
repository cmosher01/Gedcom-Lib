package nu.mine.mosher.gedcom;

import nu.mine.mosher.logging.Jul;
import org.mozilla.universalchardet.UnicodeBOMInputStream;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GedcomEncodingDetector {
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
            Jul.log().log(Level.INFO, String.format("First guess at character encoding: %s", charsetDetected.get().displayName()));
        } else {
            Jul.log().log(Level.INFO, "First guess at character encoding failed.");
            Jul.log().log(Level.INFO, String.format("First guess at character encoding defaulting to: %s", Charset.defaultCharset().displayName()));
        }

        final Optional<Charset> charsetDeclared = detectCharsetDeclared(this.gedcom, charsetDetected.orElse(Charset.defaultCharset()));
        if (charsetDeclared.isPresent()) {
            Jul.log().log(Level.INFO, String.format("Found declared character encoding: %s", charsetDeclared.get().displayName()));
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

        Jul.log().log(Level.INFO, String.format("Will use character encoding: %s", charsetResult.displayName()));

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
            Jul.log().log(Level.INFO, "Character detector returned null.");
            return Optional.empty();
        }
        if (c.isEmpty()) {
            Jul.log().log(Level.INFO, "Character detector returned empty string.");
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(c));
        } catch (final Exception ignore) {
            Jul.log().log(Level.WARNING, String.format("Character detector returned invalid Charset name: %s", c), ignore);
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
            Jul.log().log(Level.WARNING, "Did not recognize that value for CHAR.");
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(headChar));
        } catch (final Exception ignore) {
            Jul.log().log(Level.WARNING, String.format("Invalid Charset name: %s", headChar), ignore);
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
                        Jul.log().log(Level.FINE, "Found HEAD line. Good.");
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
                            Jul.log().log(Level.INFO, String.format("Found CHAR line with value: %s", charsetNameDeclared));
                            return charsetNameDeclared.toUpperCase();
                        }
                    }
                }
                break;
                case OUT_HEAD: {
                    Jul.log().log(Level.WARNING, "Could not find CHAR line.");
                    return "";
                }
            }
        }
        Jul.log().log(Level.WARNING, "Could not find HEAD line.");
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
    private static final Map<String, String> mapChar;
    static {
        final Map<String, String> m = new HashMap<String, String>();

        m.put("IBMPC", "Cp437");
        m.put("IBM-PC", "Cp437");
        m.put("IBM", "Cp437");
        m.put("PC", "Cp437");
        m.put("OEM", "Cp437");

        m.put("MSDOS", "Cp850");
        m.put("MS-DOS", "Cp850");
        m.put("DOS", "Cp850");
        m.put("IBM DOS", "Cp850");

        m.put("ANSI", "windows-1252");
        m.put("WINDOWS", "windows-1252");
        m.put("WIN", "windows-1252");
        m.put("IBM WINDOWS", "windows-1252");
        m.put("IBM_WINDOWS", "windows-1252");

        m.put("ASCII", "windows-1252");
        m.put("CP1252", "windows-1252");

        m.put("ISO-8859-1", "windows-1252");
        m.put("ISO8859-1", "windows-1252");
        m.put("ISO-8859", "windows-1252");
        m.put("LATIN1", "windows-1252");

        m.put("MAC", "MacRoman");
        m.put("MACINTOSH", "MacRoman");

        m.put("UNICODE", "UTF-16");
        m.put("UTF-8", "UTF-8");

        m.put("ANSEL", "x-gedcom-ansel");

        mapChar = Collections.unmodifiableMap(m);
    }

    private static String interpretHeadChar(final String headChar) {
        return Objects.toString(mapChar.get(headChar), "");
    }
}

package nu.mine.mosher.gedcom;

import java.security.*;

/**
 * Generates a unique ID, containing only uppercase letters (A-Z),
 * and 20 characters long, suitable for use a GEDCOM ID.
 */
class UidGen {
    private static final String ALPHABET;
    static {
        String s = "";
        for (char c = 'A'; c <= 'Z'; ++c) {
            s += c;
        }
        ALPHABET = s;
    }

    public static final String RNG_ALGORITHM = "NativePRNGNonBlocking";
    private static final SecureRandom RNG;
    static {
        try {
            RNG = SecureRandom.getInstance(RNG_ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String generate() {
        final StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < 20; ++i) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}

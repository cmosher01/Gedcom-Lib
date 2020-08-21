package nu.mine.mosher.gedcom;

import java.security.SecureRandom;
import java.util.*;

public class GedcomUidGenerator {
    private final int length;
    private final Set<String> generated = new HashSet<>(1024);
    private final Random rnd = new SecureRandom();



    public GedcomUidGenerator(final int length) {
        this.length = length;
    }



    public String generateIdWithAts() {
        return "@" + generateId() + "@";
    }

    public String generateId() {
        int sanity = 100000;
        String s = generateCandidateIdOfLength();
        while (sanity > 0 && this.generated.contains(s)) {
            --sanity;
            s = generateCandidateIdOfLength();
        }
        if (sanity <= 0) {
            throw new IllegalStateException("Cannot generate universally unique IDs as requested.");
        }
        this.generated.add(s);
        return s;
    }



    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private String generateCandidateIdOfLength() {
        final StringBuilder sb = new StringBuilder(this.length);
        for (int i = 0; i < this.length; ++i) {
            sb.append(ALPHABET.charAt(this.rnd.nextInt(ALPHABET.length())));
        }
        return sb.toString();

    }
}

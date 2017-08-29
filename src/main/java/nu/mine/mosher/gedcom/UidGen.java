package nu.mine.mosher.gedcom;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates a unique ID, containing only letters (A-Z, a-z),
 * and 20 characters long, suitable for use a GEDCOM ID.
 */
public class UidGen {
    public static String generateUid() {
        String s = generateCandidateUid();
        while (s.contains("_") || s.contains("-")) {
            s = generateCandidateUid();
        }
        return s.substring(0, 20);
    }

    private static String generateCandidateUid() {
        return base64encode(rbFromUuid(UUID.randomUUID()));
    }

    private static byte[] rbFromUuid(final UUID uuid) {
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    private static String base64encode(final byte[] rb) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rb);
    }
}

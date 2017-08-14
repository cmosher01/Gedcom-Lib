package nu.mine.mosher.gedcom.ansel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class AnselCharacterMap {
    private static final String RESOURCE = "ansel.csv";

    public static final Map<Integer, Integer> map = unmodifiableMap(readMapFromResource(RESOURCE));

    private static Map<Integer, Integer> readMapFromResource(final String resource) {
        try {
            return tryReadMapFromResource(resource);
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<Integer, Integer> tryReadMapFromResource(final String resource) throws IOException {
        return new BufferedReader(new InputStreamReader(AnselCharacterMap.class.getResourceAsStream(resource))).lines()
            .map(AnselCharacterMap::csv)
            .map(Arrays::stream)
            .map(m -> m.mapToInt(AnselCharacterMap::hex))
            .map(IntStream::boxed)
            .map(m -> m.collect(toList()))
            .collect(toMap(k -> k.get(0), v -> v.get(1)));
    }

    private static String[] csv(String s) {
        return s.split(",", -1);
    }

    private static Integer hex(final String s) {
        try {
            return Integer.parseInt(s, 0x10);
        } catch (final Throwable e) {
            return -1;
        }
    }
}

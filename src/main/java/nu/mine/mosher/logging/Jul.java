package nu.mine.mosher.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

public final class Jul {
    private static final Level LEVEL_DEFAULT = Level.WARNING;
    private static final String FORMAT_LOG = "%1$s [%2$-7s] %3$s%n%4$s"; // TS Level Msg Throw
    private static final String FORMAT_TS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final Logger logger = Logger.getLogger("");

    private static boolean initialized;


    private Jul() {
        throw new IllegalStateException();
    }

    public static Logger log() {
        initialize();
        return Jul.logger;
    }

    public static void setLevel(final Level v) {
        log();
        Jul.logger.setLevel(v);
        handlers().forEach(h -> h.setLevel(v));
    }

    public static void verbose(final boolean v) {
        setLevel(v ? Level.ALL : LEVEL_DEFAULT);
    }

    public static void thrown(final Throwable thrown) {
        Jul.log().log(Level.SEVERE, thrown.toString(), thrown);
    }


    private static List<Handler> handlers() {
        return asList(Jul.logger.getHandlers());
    }

    private static synchronized void initialize() {
        if (Jul.initialized) {
            return;
        }

        Jul.initialized = true;
        handlers().forEach(Formatter::adorn);
        verbose(false);
    }

    private static class Formatter extends java.util.logging.Formatter {
        private static void adorn(final Handler h) {
            h.setFormatter(new Formatter());
        }

        @Override
        public String format(final LogRecord r) {
            return String.format(FORMAT_LOG, ts(r.getMillis()), r.getLevel(), r.getMessage(), thr(r.getThrown()));
        }

        private String ts(final long millis) {
            final DateFormat df = new SimpleDateFormat(FORMAT_TS);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            return df.format(new Date(millis));
        }

        private String thr(final Throwable t) {
            final StringWriter sw = new StringWriter();
            if (t != null) {
                t.printStackTrace(new PrintWriter(sw, true));
            }
            return sw.toString();
        }
    }
}

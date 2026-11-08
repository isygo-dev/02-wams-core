package eu.isygoit.plan.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class LogBucket {
    private static final int HOURS_PER_BUCKET = 6;

    private LogBucket() {
    }

    public static Instant start(Instant timestamp) {
        int bucketHour = (timestamp.atOffset(java.time.ZoneOffset.UTC).getHour() / HOURS_PER_BUCKET)
                * HOURS_PER_BUCKET;
        return timestamp.truncatedTo(ChronoUnit.DAYS).plus(bucketHour, ChronoUnit.HOURS);
    }
}

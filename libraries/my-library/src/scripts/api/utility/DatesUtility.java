package scripts.api.utility;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class DatesUtility {

    public static LocalDateTime fromMilliseconds(long milliseconds) {
        return Instant.ofEpochMilli(milliseconds).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static long getMilliseconds(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long durationFromNow(LocalDateTime target) {
        return durationFromNow(target, ChronoUnit.MILLIS);
    }

    public static long durationFromNow(LocalDateTime target, ChronoUnit unit) {
        return durationFrom(LocalDateTime.now(), target, unit);
    }

    public static LocalDateTime fromNow(long difference, ChronoUnit unit) {
        if (difference > 0) return LocalDateTime.now().plus(difference, unit);
        return LocalDateTime.now().minus(difference, unit);
    }

    public static long durationFrom(LocalDateTime from, LocalDateTime to, ChronoUnit unit) {
        if (from.isBefore(LocalDateTime.now())) return from.until(to, unit) * -1;
        else return to.until(from, ChronoUnit.MILLIS);
    }
}

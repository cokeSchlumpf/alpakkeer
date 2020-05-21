package alpakkeer.core.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface DateTime {

   DateTime add(Duration duration);

   DateTime minus(Duration duration);

   long toEpochSeconds();

   long toEpochSeconds(ZoneId zoneHint);

   long toEpochMillis();

   long toEpochMillis(ZoneId zoneHint);

   LocalDateTime toLocalDateTime();

   LocalDateTime toLocalDateTime(ZoneId zoneHint);

   ZonedDateTime toZonedDateTime();

   ZonedDateTime toZonedDateTime(ZoneId zone);

}

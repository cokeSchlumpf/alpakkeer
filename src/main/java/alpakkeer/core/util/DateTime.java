package alpakkeer.core.util;

import java.time.*;

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

   default Instant toInstant() {
      return toZonedDateTime().toInstant();
   }

   default Instant toInstant(ZoneId zone) {
      return toZonedDateTime(zone).toInstant();
   }

}

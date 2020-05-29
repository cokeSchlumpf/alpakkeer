package alpakkeer.core.util;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.*;

public final class DateTimes {

   private DateTimes() {

   }

   public static DateTime ofLocalDateTime(LocalDateTime date) {
      return LocalDateTimeDateTime.apply(date);
   }

   public static DateTime ofZonedDateTime(ZonedDateTime date) {
      return ZonedDateTimeDateTime.apply(date);
   }

   public static DateTime ofEpochMillis(long millis) {
      return EpochMillisDateTime.apply(millis);
   }

   public static DateTime ofEpochSeconds(long seconds) {
      return EpochMillisDateTime.apply(seconds * 1000);
   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class EpochMillisDateTime implements DateTime {

      long millis;

      @Override
      public DateTime add(Duration duration) {
         return EpochMillisDateTime.apply(millis + duration.toMillis());
      }

      @Override
      public DateTime minus(Duration duration) {
         return EpochMillisDateTime.apply(millis - duration.toMillis());
      }

      @Override
      public long toEpochSeconds() {
         return millis / 1000;
      }

      @Override
      public long toEpochSeconds(ZoneId zoneHint) {
         return millis / 1000;
      }

      @Override
      public long toEpochMillis() {
         return millis;
      }

      @Override
      public long toEpochMillis(ZoneId zoneHint) {
         return millis;
      }

      @Override
      public LocalDateTime toLocalDateTime() {
         return toLocalDateTime(ZoneId.systemDefault());
      }

      @Override
      public LocalDateTime toLocalDateTime(ZoneId zoneHint) {
         return Instant.ofEpochMilli(millis).atZone(zoneHint).toLocalDateTime();
      }

      @Override
      public ZonedDateTime toZonedDateTime() {
         return toZonedDateTime(ZoneId.systemDefault());
      }

      @Override
      public ZonedDateTime toZonedDateTime(ZoneId zone) {
         return Instant.ofEpochMilli(millis).atZone(zone);
      }

   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class LocalDateTimeDateTime implements DateTime {

      LocalDateTime value;

      @Override
      public DateTime add(Duration duration) {
         try {
            return apply(value.plusNanos(duration.toNanos()));
         } catch (ArithmeticException e) {
            return apply(value.plusSeconds(duration.getSeconds()));
         }
      }

      @Override
      public DateTime minus(Duration duration) {
         try {
            return apply(value.minusNanos(duration.toNanos()));
         } catch (ArithmeticException e) {
            return apply(value.minusSeconds(duration.getSeconds()));
         }
      }

      @Override
      public long toEpochSeconds() {
         return toEpochSeconds(ZoneId.systemDefault());
      }

      @Override
      public long toEpochSeconds(ZoneId zoneHint) {
         return toZonedDateTime(zoneHint).toEpochSecond();
      }

      @Override
      public long toEpochMillis() {
         return toEpochMillis(ZoneId.systemDefault());
      }

      @Override
      public long toEpochMillis(ZoneId zoneHint) {
         ZonedDateTime zoned = toZonedDateTime(zoneHint);
         return zoned.toEpochSecond() * 1000 + (zoned.getNano() / 1000000);
      }

      @Override
      public LocalDateTime toLocalDateTime() {
         return value;
      }

      @Override
      public LocalDateTime toLocalDateTime(ZoneId zoneHint) {
         return value;
      }

      @Override
      public ZonedDateTime toZonedDateTime() {
         return toZonedDateTime(ZoneId.systemDefault());
      }

      @Override
      public ZonedDateTime toZonedDateTime(ZoneId zone) {
         return value.atZone(zone);
      }

   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class ZonedDateTimeDateTime implements DateTime {

      ZonedDateTime value;

      @Override
      public DateTime add(Duration duration) {
         try {
            return apply(value.plusNanos(duration.toNanos()));
         } catch (ArithmeticException e) {
            return apply(value.plusSeconds(duration.getSeconds()));
         }
      }

      @Override
      public DateTime minus(Duration duration) {
         try {
            return apply(value.minusNanos(duration.toNanos()));
         } catch (ArithmeticException e) {
            return apply(value.minusSeconds(duration.getSeconds()));
         }
      }

      @Override
      public long toEpochSeconds() {
         return value.toEpochSecond();
      }

      @Override
      public long toEpochSeconds(ZoneId zoneHint) {
         return value.toEpochSecond();
      }

      @Override
      public long toEpochMillis() {
         return toEpochMillis(value.getZone());
      }

      @Override
      public long toEpochMillis(ZoneId zoneHint) {
         return value.toEpochSecond() * 1000 + (value.getNano() / 1000000);
      }

      @Override
      public LocalDateTime toLocalDateTime() {
         return toLocalDateTime(ZoneId.systemDefault());
      }

      @Override
      public LocalDateTime toLocalDateTime(ZoneId zoneHint) {
         return value.withZoneSameInstant(zoneHint).toLocalDateTime();
      }

      @Override
      public ZonedDateTime toZonedDateTime() {
         return value;
      }

      @Override
      public ZonedDateTime toZonedDateTime(ZoneId zone) {
         return value;
      }
   }

}

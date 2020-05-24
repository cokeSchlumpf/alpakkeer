package alpakkeer.core.monitoring.values;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Value
@AllArgsConstructor(staticName = "apply")
public class Marker {

   String title;

   String text;

   Instant from;

   Instant to;

   List<String> tags;

   public static Marker apply(String title, String text, Instant from, Instant to) {
      return apply(title, text, from, to, List.of());
   }

   public static Marker apply(String title, Instant from, Instant to, List<String> tags) {
      return apply(title, null, from, to, tags);
   }

   public static Marker apply(String title, Instant from, Instant to) {
      return apply(title, from, to, List.of());
   }

   public static Marker apply(String title, String text, Instant from, List<String> tags) {
      return apply(title, text, from, null, tags);
   }

   public static Marker apply(String title, String text, Instant from) {
      return apply(title, text, from, List.of());
   }

   public static Marker apply(String title, Instant from, List<String> tags) {
      return apply(title, null, from, null, tags);
   }

   public static Marker apply(String title, Instant from) {
      return apply(title, null, from, null, List.of());
   }

   public Optional<String> getText() {
      return Optional.ofNullable(text);
   }

   public Optional<Instant> getTo() {
      return Optional.ofNullable(to);
   }

}

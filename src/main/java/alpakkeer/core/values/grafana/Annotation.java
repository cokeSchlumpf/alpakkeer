package alpakkeer.core.values.grafana;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor(staticName = "apply")
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class Annotation {

   String text;

   String title;

   boolean isRegion;

   long time;

   Long timeEnd;

   List<String> tags;

   public static Annotation apply(String text, String title, long time, long timeEnd, List<String> tags) {
      return apply(text, title, true, time, timeEnd, tags);
   }

   public static Annotation apply(String text, String title, long time, long timeEnd) {
      return apply(text, title, true, time, timeEnd, List.of());
   }

   public static Annotation apply(String text, String title, long time, List<String> tags) {
      return apply(text, title, false, time, null, tags);
   }

   public static Annotation apply(String text, String title, long time) {
      return apply(text, title, time, List.of());
   }

}

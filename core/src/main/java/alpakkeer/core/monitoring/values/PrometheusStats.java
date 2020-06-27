package alpakkeer.core.monitoring.values;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PrometheusStats {

   Instant moment;

   String name;

   double value;

   Map<String, String> labels;

   public static PrometheusStats apply(Instant moment, String name, double value, Map<String, String> labels) {
      return new PrometheusStats(moment, name, value, Map.copyOf(labels));
   }

   public static PrometheusStats apply(Instant moment, String name, double value) {
      return apply(moment, name, value, Map.of());
   }

}

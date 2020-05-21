package alpakkeer.core.values.grafana;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public class QueryRequest {

   String requestId;

   String timezone;

   int panelId;

   int dashboardId;

   Range range;

   RangeRaw rangeRaw;

   String interval;

   Long intervalMs;

   Long startTime;

   int maxDataPoints;

   List<Target> targets;

   List<Filter> adhocFilters;

   Map<String, ScopedVar> scopedVars;

   public List<Filter> getAdhocFilter() {
      if (adhocFilters == null) {
         return List.of();
      } else {
         return List.copyOf(adhocFilters);
      }
   }

   public List<Target> getTargets() {
      if (targets == null) {
         return List.of();
      } else {
         return List.copyOf(targets);
      }
   }

}

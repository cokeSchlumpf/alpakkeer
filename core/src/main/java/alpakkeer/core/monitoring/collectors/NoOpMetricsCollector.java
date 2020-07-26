package alpakkeer.core.monitoring.collectors;

import alpakkeer.javadsl.AlpakkeerRuntime;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.monitoring.values.Marker;
import alpakkeer.core.monitoring.values.TimeSeries;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor(staticName = "apply")
public final class NoOpMetricsCollector implements MetricsCollector {

   @Override
   public void run(AlpakkeerRuntime runtime) {

   }

   @Override
   public List<MetricStore<List<Marker>>> getMarkerMetrics() {
      return List.of();
   }

   @Override
   public List<MetricStore<TimeSeries>> getTimeSeriesMetrics() {
      return List.of();
   }
}

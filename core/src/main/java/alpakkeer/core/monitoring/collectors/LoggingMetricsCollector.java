package alpakkeer.core.monitoring.collectors;

import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.monitoring.values.Marker;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;

import java.io.StringWriter;
import java.util.List;

@AllArgsConstructor(staticName = "apply")
public final class LoggingMetricsCollector implements MetricsCollector {

   Logger LOG;

   CronExpression interval;

   @Override
   public void run(RuntimeConfiguration runtime) {
      var jobName = "alpakkeer-internal-collectors-logging";
      runtime
         .getScheduler()
         .schedule(
            jobName,
            interval,
            () -> Operators.suppressExceptions(() -> {
               var writer = new StringWriter();
               TextFormat.write004(writer, runtime.getCollectorRegistry().metricFamilySamples());
               LOG.info("Collected Prometheus metrics:\n\n{}", writer.toString());
            }))
         .thenCompose(i -> runtime.getScheduler().getJob(jobName))
         .whenComplete((maybeDetails, ex) -> {
            if (maybeDetails != null && maybeDetails.isPresent()) {
               var details = maybeDetails.get();
               LOG.info(
                  "Initialized Logging metrics collector with `{}`, next collection will run at `{}`",
                  details.getCronExpression(),
                  details.getNextExecution());
            } else if (ex != null) {
               LOG.error("An exception occurred initializing the logging metrics collector", ex);
            } else {
               LOG.warn("Logging metrics collector was not successfully initialized - No scheduled job found");
            }
         });
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

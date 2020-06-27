package alpakkeer.core.monitoring.collectors;

import akka.actor.ActorSystem;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.Metrics;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.monitoring.values.DataPoint;
import alpakkeer.core.monitoring.values.Marker;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class InMemoryMetricsCollector implements MetricsCollector {

   private final Logger LOG = LoggerFactory.getLogger("alpakkeer.metrics");

   private final CronExpression interval;

   private final AtomicReference<EvictingQueue<PrometheusStats>> store;

   private final AtomicReference<Set<String>> metrics;

   private ActorSystem system;

   public static InMemoryMetricsCollector apply(CronExpression interval, int maxSize) {
      return apply(interval, new AtomicReference<>(EvictingQueue.create(maxSize)), new AtomicReference<>(Sets.newHashSet()), null);
   }

   @Override
   public void run(RuntimeConfiguration runtime) {
      this.system = runtime.getSystem();

      var jobName = "alpakkeer-internal-collectors-in-memory";

      runtime
         .getScheduler()
         .schedule(
            jobName,
            interval,
            () -> Operators.suppressExceptions(() -> {
               var newStats = Lists.newArrayList(
                  runtime
                     .getCollectorRegistry()
                     .metricFamilySamples()
                     .asIterator())
                  .stream()
                  .flatMap(family -> family.samples.stream())
                  .map(sample -> {
                     var moment = Optional.ofNullable(sample.timestampMs).map(Instant::ofEpochMilli).orElseGet(Instant::now);
                     var labels = Map.<String, String>of();

                     if (sample.labelNames.size() == sample.labelValues.size()) {
                        labels = Maps.newHashMap();

                        for (int i = 0; i < sample.labelNames.size(); i++) {
                           labels.put(sample.labelNames.get(i), sample.labelValues.get(0));
                        }

                        labels = Map.copyOf(labels);
                     }

                     return PrometheusStats.apply(moment, sample.name, sample.value, labels);
                  })
                  .collect(Collectors.toList());

               metrics.getAndUpdate(set -> {
                  set.addAll(newStats.stream().map(PrometheusStats::getName).collect(Collectors.toList()));
                  return set;
               });

               store.getAndUpdate(queue -> {
                  queue.addAll(newStats);
                  return queue;
               });
            }))
         .thenCompose(i -> runtime.getScheduler().getJob(jobName))
         .whenComplete((maybeDetails, ex) -> {
            if (maybeDetails != null && maybeDetails.isPresent()) {
               var details = maybeDetails.get();
               LOG.info(
                  "Initialized in-memory metrics collector with `{}` and max queue size of `{}`, next collection will run at `{}`",
                  details.getCronExpression(),
                  store.get().remainingCapacity() + store.get().size(),
                  details.getNextExecution());
            } else if (ex != null) {
               LOG.error("An exception occurred initializing the in-memory metrics collector", ex);
            } else {
               LOG.warn("In-memory metrics collector was not successfully initialized - No scheduled job found");
            }
         });
   }

   @Override
   public List<MetricStore<List<Marker>>> getMarkerMetrics() {
      return List.of();
   }

   @Override
   public List<MetricStore<TimeSeries>> getTimeSeriesMetrics() {
      if (system == null) {
         return List.of();
      } else {
         return this.metrics.get()
            .stream()
            .map(name -> Metrics.createTimeSeriesMetricFromDataPoints(name, name, () -> store
               .get()
               .stream()
               .filter(s -> s.getName().equals(name))
               .map(s -> DataPoint.apply(s.getMoment(), s.getValue()))
               .collect(Collectors.toList()), system))
            .collect(Collectors.toList());
      }
   }

}

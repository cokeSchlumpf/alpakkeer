package alpakkeer.core.jobs.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import alpakkeer.core.util.Strings;
import com.google.common.collect.Maps;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class PrometheusJobMonitor<P, C> implements JobMonitor<P, C> {

   private static Logger LOG = LoggerFactory.getLogger(PrometheusJobMonitor.class);

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class RunningJob {

      long startNanos;

   }

   private final String name;

   private final Gauge queuedExecutions;

   private final Gauge jobDuration;

   private final Summary jobDurations;

   private final Map<String, RunningJob> runningExecutions;

   private final Map<String, Gauge> pushPullLatencies;

   private final Map<String, Gauge> pullPushLatencies;

   private final Map<String, Counter> counts;

   private final Map<String, Gauge> countsInterval;

   private final Map<String, Gauge> throughputs;

   private final Map<String, Gauge> latencies;

   private final CollectorRegistry registry;

   public static <P, C> PrometheusJobMonitor<P, C> apply(String name, CollectorRegistry registry) {
      var nameSC = Strings.convert(name).toSnakeCase();

      Gauge queuedJobs = Gauge
         .build()
         .name(String.format("%s__queued_executions", nameSC))
         .help(String.format("Current number of queued executions of job `%s`", nameSC))
         .create()
         .register(registry);

      Gauge jobDuration = Gauge
         .build()
         .name(String.format("%s__job_duration_seconds", nameSC))
         .help(String.format("The duration of the job execution of job `%s` in seconds", nameSC))
         .create()
         .register(registry);

      Summary jobDurations = Summary
         .build()
         .name(String.format("%s__job_durations_seconds", nameSC))
         .help(String.format("A summary of recorded durations of job `%s` in seconds", nameSC))
         .create()
         .register(registry);

      return apply(
         nameSC, queuedJobs, jobDuration, jobDurations, Maps.newConcurrentMap(),
         Maps.newConcurrentMap(), Maps.newConcurrentMap(), Maps.newConcurrentMap(),
         Maps.newConcurrentMap(), Maps.newConcurrentMap(), Maps.newConcurrentMap(),
         registry);
   }

   @Override
   public void onTriggered(String executionId, P properties) {
      var exec = RunningJob.apply(System.nanoTime());
      runningExecutions.put(executionId, exec);
   }

   @Override
   public void onStarted(String executionId) {
      // do nothing
   }

   @Override
   public void onFailed(String executionId, Throwable cause) {
      runningExecutions.remove(executionId);
   }

   @Override
   public void onCompleted(String executionId, C result) {
      onCompleted(executionId);
   }

   @Override
   public void onCompleted(String executionId) {
      if (runningExecutions.containsKey(executionId)) {
         var exec = runningExecutions.remove(executionId);
         var seconds = Duration.ofNanos(System.nanoTime() - exec.startNanos).getSeconds();
         jobDurations.observe(seconds);
         jobDuration.set(seconds);
      }

      finish();
   }

   @Override
   public void onStats(String executionId, String name, CheckpointMonitor.Stats statistics) {
      var nameSC = Strings.convert(name).toSnakeCase();

      try {
         if (!counts.containsKey(nameSC)) {
            counts.put(nameSC, Counter
               .build(
                  String.format("%s__%s__count_sum", this.name, nameSC),
                  "Number of documents processed by this checkpoint")
               .labelNames("execution_id")
               .register(registry));
         }

         if (!countsInterval.containsKey(nameSC)) {
            countsInterval.put(nameSC, Gauge
               .build(
                  String.format("%s__%s__count_current", this.name, nameSC),
                  "Number of documents processed by this checkpoint within measurement interval")
               .register(registry));
         }

         if (!throughputs.containsKey(nameSC)) {
            throughputs.put(nameSC, Gauge
               .build(
                  String.format("%s__%s__throughout_per_second", this.name, nameSC),
                  "Throughput processed by this checkpoint within measurement interval")
               .register(registry));
         }

         if (!pullPushLatencies.containsKey(nameSC)) {
            pullPushLatencies.put(nameSC, Gauge
               .build(
                  String.format("%s__%s__pull_push_latency_in_ns", this.name, nameSC),
                  "Average pull-push latency within last interval")
               .register(registry));
         }

         if (!pushPullLatencies.containsKey(nameSC)) {
            pushPullLatencies.put(nameSC, Gauge
               .build(
                  String.format("%s__%s__push_pull_latency_in_ns", this.name, nameSC),
                  "Average pull-push latency within last interval")
               .register(registry));
         }


         counts.get(nameSC).labels(executionId).inc(statistics.count());
         countsInterval.get(nameSC).set(statistics.count());
         throughputs.get(nameSC).set(statistics.throughputElementsPerSecond());
         pullPushLatencies.get(nameSC).set(statistics.pullPushLatencyNanos());
         pushPullLatencies.get(nameSC).set(statistics.pushPullLatencyNanos());
      } catch (Exception ex) {
         LOG.warn("An exception occurred while updating Prometheus metrics", ex);
      }
   }

   @Override
   public void onStats(String executionId, String name, LatencyMonitor.Stats statistics) {
      var nameSC = Strings.convert(name).toSnakeCase();

      try {
         if (!counts.containsKey(nameSC)) {
            counts.put(nameSC, Counter
               .build(
                  String.format("%s__%s__count_sum", this.name, nameSC),
                  "Number of documents processed by this checkpoint")
               .labelNames("execution_id")
               .register(registry));
         }

         if (!countsInterval.containsKey(nameSC)) {
            countsInterval.put(nameSC, Gauge
               .build(
                  String.format("%s__%s__count_current", this.name, nameSC),
                  "Number of documents processed by this checkpoint within measurement interval")
               .register(registry));
         }

         if (!latencies.containsKey(nameSC)) {
            latencies.put(nameSC, Gauge
               .build(
                  String.format("%s__%s__latency_in_ms", this.name, nameSC),
                  "Average latency of stage within last interval in milliseconds.")
               .register(registry));
         }

         counts.get(nameSC).labels(executionId).inc(statistics.count());
         countsInterval.get(nameSC).set(statistics.count());
         latencies.get(nameSC).set(statistics.avgLatency());
      } catch (Exception ex) {
         LOG.warn("An exception occurred while updating Prometheus metrics", ex);
      }
   }

   @Override
   public void onStopped(String executionId, C result) {
      onStopped(executionId);
   }

   @Override
   public void onStopped(String executionId) {
      runningExecutions.remove(executionId);
      finish();
   }

   @Override
   public void onQueued(int newQueueSize) {
      queuedExecutions.set(newQueueSize);
   }

   @Override
   public void onEnqueued(int newQueueSize) {
      queuedExecutions.set(newQueueSize);
   }

   @Override
   public CompletionStage<Optional<Object>> getStatus() {
      return CompletableFuture.completedFuture(Optional.empty());
   }

   private void finish() {
      counts.values().forEach(Counter::clear);
      countsInterval.values().forEach(Gauge::clear);
      pushPullLatencies.values().forEach(Gauge::clear);
      pullPushLatencies.values().forEach(Gauge::clear);
      throughputs.values().forEach(Gauge::clear);
      latencies.values().forEach(Gauge::clear);
   }

}

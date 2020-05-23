package alpakkeer.core.jobs.monitor;

import akka.actor.ActorSystem;
import alpakkeer.core.monitoring.*;
import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import alpakkeer.core.util.Operators;
import alpakkeer.core.util.Strings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public final class InMemoryHistoryJobMonitor<P, C> implements JobMonitor<P, C>, MetricsMonitor {

   int statsLimit;

   ObjectMapper om;

   ActorSystem system;

   ConcurrentHashMap<String, Running<P>> runningExecutions;

   EvictingQueue<Executed<P, C>> history;

   List<Metric<List<Marker>>> markers;

   public static <P, C> InMemoryHistoryJobMonitor<P, C> apply(
      int limit, int statsLimit, ObjectMapper om, ActorSystem system) {

      var history = EvictingQueue.<Executed<P, C>>create(limit);
      var running = new ConcurrentHashMap<String, Running<P>>();

      var runsSuccessful = Metrics.createMarkerMetric(
         "runs_successful",
         "Markers for successful job executions",
         (from, to) -> history
            .stream()
            .filter(e -> e.getExited().equals(JobResult.COMPLETED))
            .filter(e -> e.getStarted().toEpochMilli() >= from.toEpochMilli())
            .filter(e -> e.getFinished().toEpochMilli() <= to.toEpochMilli())
            .map(e -> {
               var f = e.getStarted();
               var t = e.getFinished();
               var result = Operators.ignoreExceptionsWithDefault(() -> om.writeValueAsString(e.getResult()), String.valueOf(e.getResult()));

               return Marker.apply(e.getExecutionId(), result, f, t);
            })
            .collect(Collectors.toList()));

      var runsFailed = Metrics.createMarkerMetric(
         "runs_failed",
         "Markers for failed job executions",
         (from, to) -> history
            .stream()
            .filter(e -> e.getExited().equals(JobResult.FAILED))
            .filter(e -> e.getStarted().toEpochMilli() >= from.toEpochMilli())
            .filter(e -> e.getFinished().toEpochMilli() <= to.toEpochMilli())
            .map(e -> {
               var f = e.getStarted();
               var t = e.getFinished();

               return Marker.apply(e.getExecutionId(), e.getException(), f, t);
            })
            .collect(Collectors.toList()));

      var runsStopped = Metrics.createMarkerMetric(
         "runs_stopped",
         "Markers for failed job executions",
         (from, to) -> history
            .stream()
            .filter(e -> e.getExited().equals(JobResult.STOPPED))
            .filter(e -> e.getStarted().toEpochMilli() >= from.toEpochMilli())
            .filter(e -> e.getFinished().toEpochMilli() <= to.toEpochMilli())
            .map(e -> {
               var f = e.getStarted();
               var t = e.getFinished();

               return Marker.apply(e.getExecutionId(), f, t);
            })
            .collect(Collectors.toList()));

      return apply(
         statsLimit, om, system, running, history, List.of(runsSuccessful, runsFailed, runsStopped));
   }

   public static <P, C> InMemoryHistoryJobMonitor<P, C> apply(int limit, ObjectMapper om, ActorSystem system) {
      return apply(limit, 10_000, om, system);
   }

   public static <P, C> InMemoryHistoryJobMonitor<P, C> apply(ObjectMapper om, ActorSystem system) {
      return apply(100, om, system);
   }

   private static <P, C> Set<String> getCheckpoints(
      EvictingQueue<Executed<P, C>> history,
      ConcurrentHashMap<String, Running<P>> runningExecutions) {

      var historic = history
         .stream()
         .flatMap(e -> e.getCheckpoints().keySet().stream())
         .collect(Collectors.toSet());

      var current = runningExecutions
         .values()
         .stream()
         .flatMap(r -> r.getCheckpoints().keySet().stream())
         .collect(Collectors.toSet());

      var result = Sets.<String>newHashSet();
      result.addAll(historic);
      result.addAll(current);

      return result;
   }

   private static <P, C> Set<String> getStages(
      EvictingQueue<Executed<P, C>> history,
      ConcurrentHashMap<String, Running<P>> runningExecutions) {

      var historic = history
         .stream()
         .flatMap(e -> e.getStages().keySet().stream())
         .collect(Collectors.toSet());

      var current = runningExecutions
         .values()
         .stream()
         .flatMap(r -> r.getStages().keySet().stream())
         .collect(Collectors.toSet());

      var result = Sets.<String>newHashSet();
      result.addAll(historic);
      result.addAll(current);

      return result;
   }

   private static <P, C> Stream<CheckpointMonitor.Stats> getCheckpointMonitorEvents(
      String checkpoint,
      EvictingQueue<Executed<P, C>> history,
      ConcurrentHashMap<String, Running<P>> runningExecutions) {

      var historic = history
         .stream()
         .flatMap(executed -> executed.getCheckpoints().getOrDefault(checkpoint, EvictingQueue.create(0)).stream());

      var running = runningExecutions
         .values()
         .stream()
         .flatMap(r -> r.getCheckpoints().getOrDefault(checkpoint, EvictingQueue.create(0)).stream());

      return Stream
         .concat(historic, running)
         .sorted(Comparator.<CheckpointMonitor.Stats, Long>comparing(s -> s.moment().toEpochMilli()).reversed());
   }

   private static <P, C> Stream<LatencyMonitor.Stats> getLatencyMonitorEvents(
      String stage,
      EvictingQueue<Executed<P, C>> history,
      ConcurrentHashMap<String, Running<P>> runningExecutions) {

      var historic = history
         .stream()
         .flatMap(executed -> executed.getStages().getOrDefault(stage, EvictingQueue.create(0)).stream());

      var running = runningExecutions
         .values()
         .stream()
         .flatMap(r -> r.getStages().getOrDefault(stage, EvictingQueue.create(0)).stream());

      return Stream
         .concat(historic, running)
         .sorted(Comparator.<LatencyMonitor.Stats, Long>comparing(s -> s.moment().toEpochMilli()).reversed());
   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class Running<P> {

      @JsonIgnore
      long startNanos;

      P properties;

      Instant started;

      Map<String, EvictingQueue<CheckpointMonitor.Stats>> checkpoints;

      Map<String, EvictingQueue<LatencyMonitor.Stats>> stages;

   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class Executed<P, C> {

      @JsonProperty("id")
      String executionId;

      @JsonProperty("properties")
      P properties;

      @JsonProperty("started")
      Instant started;

      @JsonProperty("finished")
      Instant finished;

      @JsonProperty("duration-in-seconds")
      Long durationInSeconds;

      @JsonProperty("exited-with")
      JobResult exited;

      @JsonProperty("result")
      C result;

      @JsonProperty
      String exception;

      @JsonIgnore
      Map<String, EvictingQueue<CheckpointMonitor.Stats>> checkpoints;

      @JsonIgnore
      Map<String, EvictingQueue<LatencyMonitor.Stats>> stages;

   }

   private enum JobResult {
      COMPLETED("completed"), FAILED("failed"), STOPPED("stopped");

      private String value;

      JobResult(String value) {
         this.value = value;
      }

      @JsonValue
      public String getValue() {
         return value;
      }
   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class Status<P, C> {

      List<Running<P>> running;

      List<Executed<P, C>> executed;

   }

   @Override
   public List<Metric<List<Marker>>> getMarkerMetrics() {
      return markers;
   }

   @Override
   public List<Metric<TimeSeries>> getTimeSeriesMetrics() {
      var checkpoints = getCheckpoints(history, runningExecutions);
      var stages = getStages(history, runningExecutions);

      var checkpointCounts = checkpoints
         .stream()
         .map(cp -> Metrics.createTimeSeriesMetricFromDataPoints(
            Strings.convert(cp).toSnakeCase() + "__" + "count",
            "number of elements processed within interval",
            () -> getCheckpointMonitorEvents(cp, history, runningExecutions)
               .map(s -> DataPoint.apply(s.moment(), s.count()))
               .collect(Collectors.toList()),
            system))
         .collect(Collectors.toList());

      var checkpointThroughput = checkpoints
         .stream()
         .map(cp -> Metrics.createTimeSeriesMetricFromDataPoints(
            Strings.convert(cp).toSnakeCase() + "__" + "throughput_elements_per_second",
            "number of elements processed within interval",
            () -> getCheckpointMonitorEvents(cp, history, runningExecutions)
               .map(s -> DataPoint.apply(s.moment(), s.throughputElementsPerSecond()))
               .collect(Collectors.toList()),
            system))
         .collect(Collectors.toList());

      var checkpointPullPushLatency = checkpoints
         .stream()
         .map(cp -> Metrics.createTimeSeriesMetricFromDataPoints(
            Strings.convert(cp).toSnakeCase() + "__" + "pull_push_latency_ns",
            "pull-push latency within interval",
            () -> getCheckpointMonitorEvents(cp, history, runningExecutions)
               .map(s -> DataPoint.apply(s.moment(), s.pullPushLatencyNanos()))
               .collect(Collectors.toList()),
            system))
         .collect(Collectors.toList());

      var checkpointPushPullLatency = checkpoints
         .stream()
         .map(cp -> Metrics.createTimeSeriesMetricFromDataPoints(
            Strings.convert(cp).toSnakeCase() + "__" + "push_pull_latency_ns",
            "push-pull latency within interval",
            () -> getCheckpointMonitorEvents(cp, history, runningExecutions)
               .map(s -> DataPoint.apply(s.moment(), s.pushPullLatencyNanos()))
               .collect(Collectors.toList()),
            system))
         .collect(Collectors.toList());

      var stageLatency = stages
         .stream()
         .map(st -> Metrics.createTimeSeriesMetricFromDataPoints(
            Strings.convert(st).toSnakeCase() + "__" + "latency_ms",
            "The average latency in ms of the stage for the measured interval.",
            () -> getLatencyMonitorEvents(st, history, runningExecutions)
               .map(s -> DataPoint.apply(s.moment(), s.avgLatency()))
               .collect(Collectors.toList()),
            system))
         .collect(Collectors.toList());

      var stageCount = stages
         .stream()
         .map(st -> Metrics.createTimeSeriesMetricFromDataPoints(
            Strings.convert(st).toSnakeCase() + "__" + "count",
            "The number of processed elements within the interval.",
            () -> getLatencyMonitorEvents(st, history, runningExecutions)
               .map(s -> DataPoint.apply(s.moment(), s.avgLatency()))
               .collect(Collectors.toList()),
            system))
         .collect(Collectors.toList());

      var stats = Lists.<Metric<TimeSeries>>newArrayList();
      stats.addAll(checkpointCounts);
      stats.addAll(checkpointThroughput);
      stats.addAll(checkpointPullPushLatency);
      stats.addAll(checkpointPushPullLatency);
      stats.addAll(stageLatency);
      stats.addAll(stageCount);

      return stats;
   }

   @Override
   public void onTriggered(String executionId, P properties) {
      var exec = Running.apply(System.nanoTime(), properties, Instant.now(), Maps.newHashMap(), Maps.newHashMap());
      runningExecutions.put(executionId, exec);
   }

   @Override
   public void onStarted(String executionId) {
      // do nothing
   }

   @Override
   public void onFailed(String executionId, Throwable cause) {
      addFinalStats(executionId);
      addToHistory(executionId, JobResult.FAILED, null, ExceptionUtils.getMessage(cause));
   }

   @Override
   public void onCompleted(String executionId, C result) {
      addFinalStats(executionId);
      addToHistory(executionId, JobResult.COMPLETED, result, null);
   }

   @Override
   public void onCompleted(String executionId) {
      addToHistory(executionId, JobResult.COMPLETED, null, null);
   }

   @Override
   public void onStats(String executionId, String name, CheckpointMonitor.Stats statistics) {
      if (runningExecutions.containsKey(executionId)) {
         var running = runningExecutions.get(executionId);

         if (running.getCheckpoints().containsKey(name)) {
            running.getCheckpoints().get(name).add(statistics);
         } else {
            var queue = EvictingQueue.<CheckpointMonitor.Stats>create(statsLimit);
            queue.add(new CheckpointMonitor.Stats(running.getStarted(), 0, 0, 0, 0));
            queue.add(statistics);
            running.getCheckpoints().put(name, queue);
         }
      }
   }

   @Override
   public void onStats(String executionId, String name, LatencyMonitor.Stats statistics) {
      if (runningExecutions.containsKey(executionId)) {
         var running = runningExecutions.get(executionId);

         if (running.getStages().containsKey(name)) {
            running.getStages().get(name).add(statistics);
         } else {
            var queue = EvictingQueue.<LatencyMonitor.Stats>create(statsLimit);
            queue.add(new LatencyMonitor.Stats(running.getStarted(), 0, 0, 0));
            queue.add(statistics);
            running.getStages().put(name, queue);
         }
      }
   }

   @Override
   public void onStopped(String executionId, C result) {
      addFinalStats(executionId);
      addToHistory(executionId, JobResult.STOPPED, result, null);
   }

   @Override
   public void onStopped(String executionId) {
      addFinalStats(executionId);
      addToHistory(executionId, JobResult.STOPPED, null, null);
   }

   @Override
   public void onQueued(int newQueueSize) {

   }

   @Override
   public void onEnqueued(int newQueueSize) {

   }

   @Override
   public CompletionStage<Optional<Object>> getStatus() {
      return CompletableFuture.completedFuture(Optional.of(Status.apply(
         List.copyOf(runningExecutions.values()),
         history.stream().sorted(Comparator.comparing(Executed::getStarted, Comparator.reverseOrder())).collect(Collectors.toList()))));
   }

   private void addToHistory(String executionId, JobResult completed, C result, String error) {
      if (runningExecutions.containsKey(executionId)) {
         var exec = runningExecutions.remove(executionId);
         var seconds = Duration.ofNanos(System.nanoTime() - exec.startNanos).getSeconds();

         history.add(Executed.apply(
            executionId, exec.getProperties(), exec.getStarted(),
            Instant.now(), seconds, completed, result, error, exec.checkpoints, exec.stages));
      }
   }

   private void addFinalStats(String executionId) {
      if (runningExecutions.containsKey(executionId)) {
         var exec = runningExecutions.get(executionId);
         var checkpoints = exec.checkpoints.keySet();
         var stages = exec.stages.keySet();

         checkpoints.forEach(cp -> exec.getCheckpoints().get(cp).add(new CheckpointMonitor.Stats(
            Instant.now(), 0, 0, 0, 0)));

         stages.forEach(cp -> exec.getStages().get(cp).add(new LatencyMonitor.Stats(
            Instant.now(), 0, 0, 0)));
      }
   }


}

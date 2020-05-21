package alpakkeer.core.jobs.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class InMemoryHistoryJobMonitor<P, C> implements JobMonitor<P, C> {

   int limit;

   ObjectMapper om;

   int statsLimit;

   ConcurrentHashMap<String, Running<P>> runningExecutions;

   EvictingQueue<Executed<P, C>> history;

   public static <P, C> InMemoryHistoryJobMonitor<P, C> apply(int limit, ObjectMapper om) {
      return apply(limit, om, 42, new ConcurrentHashMap<>(), EvictingQueue.create(limit));
   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class Running<P> {

      @JsonIgnore
      long startNanos;

      P properties;

      LocalDateTime started;

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
      LocalDateTime started;

      @JsonProperty("finished")
      LocalDateTime finished;

      @JsonProperty("duration-in-seconds")
      Long durationInSeconds;

      @JsonProperty("exited-with")
      JobResult exited;

      @JsonProperty("result")
      C result;

      @JsonProperty
      String exception;

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
   public void onTriggered(String executionId, P properties) {
      var exec = Running.apply(System.nanoTime(), properties, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
      runningExecutions.put(executionId, exec);
   }

   @Override
   public void onStarted(String executionId) {
      // do nothing
   }

   @Override
   public void onFailed(String executionId, Throwable cause) {
      addToHistory(executionId, JobResult.FAILED, null, ExceptionUtils.getMessage(cause));
   }

   @Override
   public void onCompleted(String executionId, C result) {
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
            queue.add(statistics);
            running.getStages().put(name, queue);
         }
      }
   }

   @Override
   public void onStopped(String executionId, C result) {
      addToHistory(executionId, JobResult.STOPPED, result, null);
   }

   @Override
   public void onStopped(String executionId) {
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
            LocalDateTime.now(), seconds, completed, result, error));
      }
   }


}

package alpakkeer.core.jobs.monitor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class InMemoryHistoryJobMonitor<P> implements JobMonitor<P> {

   int limit;

   ObjectMapper om;

   ConcurrentHashMap<String, Running<P>> runningExecutions;

   EvictingQueue<Executed<P>> history;

   public static <P> InMemoryHistoryJobMonitor<P> apply(int limit, ObjectMapper om) {
      return apply(limit, om, new ConcurrentHashMap<>(), EvictingQueue.create(limit));
   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class Running<P> {

      @JsonIgnore
      long startNanos;

      P properties;

      LocalDateTime started;

   }

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class Executed<P> {

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

      @JsonProperty("result")
      JobResult result;

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
   private static class Status<P> {

      List<Running<P>> running;

      List<Executed<P>> executed;

   }

   @Override
   public void onTriggered(String executionId, P properties) {
      var exec = Running.apply(System.nanoTime(), properties, LocalDateTime.now());
      runningExecutions.put(executionId, exec);
   }

   @Override
   public void onStarted(String executionId) {
      // do nothing
   }

   @Override
   public void onFailed(String executionId, Throwable cause) {
      addToHistory(executionId, JobResult.FAILED, ExceptionUtils.getMessage(cause));
   }

   @Override
   public void onCompleted(String executionId) {
      addToHistory(executionId, JobResult.COMPLETED, null);
   }

   @Override
   public void onStopped(String executionId) {
      addToHistory(executionId, JobResult.STOPPED, null);
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

   private void addToHistory(String executionId, JobResult result, String error) {
      if (runningExecutions.containsKey(executionId)) {
         var exec = runningExecutions.remove(executionId);
         var seconds = Duration.ofNanos(System.nanoTime() - exec.startNanos).getSeconds();

         history.add(Executed.apply(
            executionId, exec.getProperties(), exec.getStarted(),
            LocalDateTime.now(), seconds, result, error));
      }
   }


}

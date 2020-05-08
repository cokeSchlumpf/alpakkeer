package alpakkeer.core.jobs.monitor;

import com.google.common.collect.Maps;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class PrometheusJobMonitor<P, C> implements JobMonitor<P, C> {

   @Value
   @AllArgsConstructor(staticName = "apply")
   private static class RunningJob {

      long startNanos;

   }

   private final Gauge queuedExecutions;

   private final Gauge jobDuration;

   private final Summary jobDurations;

   private final Map<String, RunningJob> runningExecutions;

   public static <P, C> PrometheusJobMonitor<P, C> apply(String name, CollectorRegistry registry) {
      Gauge queuedJobs = Gauge
         .build()
         .name(String.format("alpakkeer__jobs__%s__queued_executions", name))
         .help(String.format("Current number of queued executions of job `%s`", name))
         .create()
         .register(registry);

      Gauge jobDuration = Gauge
         .build()
         .name(String.format("alpakkeer__jobs__%s__job_duration_seconds", name))
         .help(String.format("The duration of the job execution of job `%s` in seconds", name))
         .create()
         .register(registry);

      Summary jobDurations = Summary
         .build()
         .name(String.format("alpakkeer__jobs__%s__job_durations_seconds", name))
         .help(String.format("A summary of recorded durations of job `%s` in seconds", name))
         .create()
         .register(registry);

      return apply(queuedJobs, jobDuration, jobDurations, Maps.newConcurrentMap());
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
   }

   @Override
   public void onStopped(String executionId, C result) {
      onStopped(executionId);
   }

   @Override
   public void onStopped(String executionId) {
      runningExecutions.remove(executionId);
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

}

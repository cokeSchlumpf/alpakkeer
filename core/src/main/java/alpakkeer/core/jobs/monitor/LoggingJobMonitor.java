package alpakkeer.core.jobs.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import alpakkeer.core.util.ObjectMapperFactory;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class LoggingJobMonitor<P, C> implements JobMonitor<P, C> {

   private final String name;

   private final Logger log;

   private final ObjectMapper om;

   public static <P, C> LoggingJobMonitor<P, C> apply(String name) {
      return apply(name, LoggerFactory.getLogger(String.format("alpakkeer.jobs.%s", name)), ObjectMapperFactory.apply().create(true));
   }

   public static <P, C> LoggingJobMonitor<P, C> apply(String name, Logger logger) {
      return apply(name, logger, ObjectMapperFactory.apply().create(true));
   }

   public static <P, C> LoggingJobMonitor<P, C> apply(String name, ObjectMapper om) {
      return apply(name, LoggerFactory.getLogger(String.format("alpakkeer.jobs.%s", name)), om);
   }

   @Override
   public void onTriggered(String executionId, P properties) {
      log.info(
         "Job execution `{}` triggered for job `{}` with properties:\n{}",
         executionId, name,
         Operators.ignoreExceptionsWithDefault(() -> om.writeValueAsString(properties), String.valueOf(properties)));
   }

   @Override
   public void onStarted(String executionId) {
      log.info("Job execution `{}` started for job `{}`", executionId, name);
   }

   @Override
   public void onFailed(String executionId, Throwable cause) {
      log.warn(
         String.format("An exception occurred in execution `%s` of job `%s`", executionId, name),
         cause);
   }

   @Override
   public void onCompleted(String executionId, C result) {
      log.info(
         "Execution `{}` of job `{}` finished successfully with result:\n{}",
         executionId, name,
         Operators.ignoreExceptionsWithDefault(() -> om.writeValueAsString(result), String.valueOf(result)));
   }

   @Override
   public void onCompleted(String executionId) {
      log.info("Execution `{}` of job `{}` finished successfully", executionId, name);
   }

   @Override
   public void onStats(String executionId, String name, CheckpointMonitor.Stats statistics) {
      log.info("{} / {} / {}", executionId, name, statistics);
   }

   @Override
   public void onStats(String executionId, String name, LatencyMonitor.Stats statistics) {
      log.info("{} / {} / {}", executionId, name, statistics);
   }

   @Override
   public void onStopped(String executionId, C result) {
      log.info(
         "Execution `{}` of job `{}` stopped with result:\n{}",
         executionId, name,
         Operators.ignoreExceptionsWithDefault(() -> om.writeValueAsString(result), String.valueOf(result)));
   }

   @Override
   public void onStopped(String executionId) {
      log.info("Execution `{}` of job `{}` stopped.", executionId, name);
   }

   @Override
   public void onQueued(int newQueueSize) {
      log.info("Queued new execution for job `{}`, new queue size: {}.", name, newQueueSize);
   }

   @Override
   public void onEnqueued(int newQueueSize) {
      // do nothing
   }

   @Override
   public CompletionStage<Optional<Object>> getStatus() {
      return CompletableFuture.completedFuture(Optional.empty());
   }

}

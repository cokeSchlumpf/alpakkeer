package alpakkeer.core.processes.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class LoggingProcessMonitor implements ProcessMonitor {

   private final String name;

   private final Logger log;

   public static LoggingProcessMonitor apply(String name) {
      return apply(name, LoggerFactory.getLogger(String.format("alpakkeer.jobs.%s", name)));
   }

   @Override
   public void onStarted(String executionId) {
      log.info("Job execution `{}` started for job `{}`", executionId, name);
   }

   @Override
   public void onFailed(String executionId, Throwable cause, Instant nextRetry) {
      log.warn(
         String.format(
            "An exception occurred in execution `%s` of job `%s`, will retry at `%s`",
            executionId, name, LocalDateTime.from(nextRetry)),
         cause);
   }

   @Override
   public void onCompletion(String executionId, Instant nextStart) {
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
   public void onStopped(String executionId) {
      log.info("Execution `{}` of job `{}` stopped.", executionId, name);
   }

   @Override
   public CompletionStage<Optional<Object>> getStatus() {
      return CompletableFuture.completedFuture(Optional.empty());
   }

}

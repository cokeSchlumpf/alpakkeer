package alpakkeer.core.processes.monitor;

import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ProcessMonitor {

   void onStarted(String executionId);

   void onFailed(String executionId, Throwable cause, Instant nextRetry);

   void onCompletion(String executionId, Instant nextStart);

   void onStats(String executionId, String name, CheckpointMonitor.Stats statistics);

   void onStats(String executionId, String name, LatencyMonitor.Stats statistics);

   void onStopped(String executionId);

   CompletionStage<Optional<Object>> getStatus();

}

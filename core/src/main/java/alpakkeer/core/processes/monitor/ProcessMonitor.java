package alpakkeer.core.processes.monitor;

import alpakkeer.core.stream.StreamMonitor;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ProcessMonitor extends StreamMonitor {

   void onStarted(String executionId);

   void onFailed(String executionId, Throwable cause, Instant nextRetry);

   void onCompletion(String executionId, Instant nextStart);

   void onStopped(String executionId);

   CompletionStage<Optional<Object>> getStatus();

}

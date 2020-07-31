package alpakkeer.core.jobs.monitor;

import alpakkeer.core.stream.StreamMonitor;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface JobMonitor<P, C> extends StreamMonitor {

   void onTriggered(String executionId, P properties);

   void onStarted(String executionId);

   void onFailed(String executionId, Throwable cause);

   void onCompleted(String executionId, C result);

   void onCompleted(String executionId);

   void onStopped(String executionId, C result);

   void onStopped(String executionId);

   void onQueued(int newQueueSize);

   void onEnqueued(int newQueueSize);

   CompletionStage<Optional<Object>> getStatus();

}

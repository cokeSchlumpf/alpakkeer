package alpakkeer.core.jobs.monitor;

import alpakkeer.core.util.Operators;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class CombinedJobMonitor<P> implements JobMonitor<P> {

   private final List<JobMonitor<P>> monitors;

   public static <P> CombinedJobMonitor<P> apply() {
      return CombinedJobMonitor.apply(Lists.newArrayList());
   }

   public CombinedJobMonitor<P> withMonitor(JobMonitor<P> monitor) {
      this.monitors.add(monitor);
      return this;
   }

   @Override
   public void onTriggered(String executionId, P properties) {
      monitors.forEach(m -> m.onTriggered(executionId, properties));
   }

   @Override
   public void onStarted(String executionId) {
      monitors.forEach(m -> m.onStarted(executionId));
   }

   @Override
   public void onFailed(String executionId, Throwable cause) {
      monitors.forEach(m -> m.onFailed(executionId, cause));
   }

   @Override
   public void onCompleted(String executionId) {
      monitors.forEach(m -> m.onCompleted(executionId));
   }

   @Override
   public void onStopped(String executionId) {
      monitors.forEach(m -> m.onStopped(executionId));
   }

   @Override
   public void onQueued(int newQueueSize) {
      monitors.forEach(m -> m.onQueued(newQueueSize));
   }

   @Override
   public void onEnqueued(int newQueueSize) {
      monitors.forEach(m -> m.onEnqueued(newQueueSize));
   }

   @Override
   public CompletionStage<Optional<Object>> getStatus() {
      var allMonitors = monitors
         .stream()
         .map(JobMonitor::getStatus)
         .collect(Collectors.toList());

      return Operators
         .allOf(allMonitors)
         .thenApply(all -> all.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()))
         .thenApply(Optional::of);
   }

}

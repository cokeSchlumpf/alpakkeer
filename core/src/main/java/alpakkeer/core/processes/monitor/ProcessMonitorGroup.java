package alpakkeer.core.processes.monitor;

import alpakkeer.core.jobs.monitor.JobMonitor;
import alpakkeer.core.monitoring.MetricsMonitor;
import alpakkeer.core.monitoring.MetricsMonitors;
import alpakkeer.core.stream.CheckpointMonitor;
import alpakkeer.core.stream.LatencyMonitor;
import alpakkeer.core.util.Operators;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public final class ProcessMonitorGroup implements ProcessMonitor {

   private List<ProcessMonitor> monitors;

   public static ProcessMonitorGroup apply() {
      return apply(Lists.newArrayList());
   }

   public ProcessMonitorGroup withMonitor(ProcessMonitor monitor) {
      this.monitors.add(monitor);
      return this;
   }

   public MetricsMonitors getMetricsMonitors() {
      var mons = monitors
         .stream()
         .filter(m -> m instanceof MetricsMonitor)
         .map(m -> (MetricsMonitor) m)
         .collect(Collectors.toList());

      return MetricsMonitors.apply(mons);
   }

   public List<ProcessMonitor> getMonitors() {
      return List.copyOf(monitors);
   }

   @Override
   public void onStarted(String executionId) {
      monitors.forEach(m -> m.onStarted(executionId));
   }

   @Override
   public void onFailed(String executionId, Throwable cause, Instant nextRetry) {
      monitors.forEach(m -> m.onFailed(executionId, cause, nextRetry));
   }

   @Override
   public void onCompletion(String executionId, Instant nextStart) {
      monitors.forEach(m -> m.onCompletion(executionId, nextStart));
   }

   @Override
   public void onStats(String executionId, String name, CheckpointMonitor.Stats statistics) {
      monitors.forEach(m -> m.onStats(executionId, name, statistics));
   }

   @Override
   public void onStats(String executionId, String name, LatencyMonitor.Stats statistics) {
      monitors.forEach(m -> m.onStats(executionId, name, statistics));
   }

   @Override
   public void onStopped(String executionId) {
      monitors.forEach(m -> m.onStopped(executionId));
   }

   @Override
   public CompletionStage<Optional<Object>> getStatus() {
      var allMonitors = monitors
         .stream()
         .map(ProcessMonitor::getStatus)
         .collect(Collectors.toList());

      return Operators
         .allOf(allMonitors)
         .thenApply(all -> all.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList()))
         .thenApply(Optional::of);
   }
}

package alpakkeer.core.stream;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import lombok.AllArgsConstructor;

import java.time.Duration;

@AllArgsConstructor(staticName = "apply")
public final class StreamMonitoringAdapter {

   private final StreamMonitor monitor;

   private final String executionId;

   public Sink<CheckpointMonitor.Stats, NotUsed> createCheckpointStatsSink(String name, Duration statsInterval) {
      return Flow.of(CheckpointMonitor.Stats.class)
         .via(Pulse.create(statsInterval, true))
         .to(Sink.foreach(stats -> monitor.onStats(executionId, name, stats)));
   }

   public Sink<CheckpointMonitor.Stats, NotUsed> createCheckpointStatsSink(String name) {
      return createCheckpointStatsSink(name, Duration.ofSeconds(30));
   }

   public Sink<LatencyMonitor.Stats, NotUsed> createLatencyStatsSink(String name, Duration statsInterval) {
      return Flow.of(LatencyMonitor.Stats.class)
         .via(Pulse.create(statsInterval, true))
         .to(Sink.foreach(stats -> monitor.onStats(executionId, name, stats)));
   }

   public Sink<LatencyMonitor.Stats, NotUsed> createLatencyStatsSink(String name) {
      return createLatencyStatsSink(name, Duration.ofSeconds(30));
   }

   public <T> Flow<T, T, NotUsed> createCheckpointMonitor(String name, Duration statsInterval) {
      return CheckpointMonitors.create(createCheckpointStatsSink(name, statsInterval));

   }

   public <T> Flow<T, T, NotUsed> createCheckpointMonitor(String name) {
      return createCheckpointMonitor(name, Duration.ofSeconds(30));
   }

   public <In, Out, Mat> Flow<In, Out, Mat> createLatencyMonitor(String name, Flow<In, Out, Mat> flow, Duration statsInterval) {
      return LatencyMonitors.create(flow, createLatencyStatsSink(name, statsInterval), (m, n) -> m);
   }

   public <In, Out, Mat> Flow<In, Out, Mat> createLatencyMonitor(String name, Flow<In, Out, Mat> flow) {
      return createLatencyMonitor(name, flow, Duration.ofSeconds(30));
   }

}

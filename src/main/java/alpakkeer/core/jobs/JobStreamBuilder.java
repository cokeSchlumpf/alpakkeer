package alpakkeer.core.jobs;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import alpakkeer.core.jobs.monitor.JobMonitor;
import alpakkeer.core.stream.*;
import lombok.AllArgsConstructor;

import java.time.Duration;

@AllArgsConstructor(staticName = "apply")
public class JobStreamBuilder<P, C> implements StreamBuilder {

   private final JobMonitor<P, C> monitor;

   private final String executionId;

   @Override
   public Sink<CheckpointMonitor.Stats, NotUsed> createCheckpointStatsSink(String name, Duration statsInterval) {
      return Flow.of(CheckpointMonitor.Stats.class)
         .via(Pulse.create(statsInterval, true))
         .to(Sink.foreach(stats -> monitor.onStats(executionId, name, stats)));
   }

   @Override
   public Sink<CheckpointMonitor.Stats, NotUsed> createCheckpointStatsSink(String name) {
      return createCheckpointStatsSink(name, Duration.ofSeconds(30));
   }

   @Override
   public Sink<LatencyMonitor.Stats, NotUsed> createLatencyStatsSink(String name, Duration statsInterval) {
      return Flow.of(LatencyMonitor.Stats.class)
         .via(Pulse.create(statsInterval, true))
         .to(Sink.foreach(stats -> monitor.onStats(executionId, name, stats)));
   }

   @Override
   public Sink<LatencyMonitor.Stats, NotUsed> createLatencyStatsSink(String name) {
      return createLatencyStatsSink(name, Duration.ofSeconds(30));
   }

   @Override
   public <T> Flow<T, T, NotUsed> createCheckpointMonitor(String name, Duration statsInterval) {
      return CheckpointMonitors.create(createCheckpointStatsSink(name, statsInterval));

   }

   @Override
   public <T> Flow<T, T, NotUsed> createCheckpointMonitor(String name) {
      return createCheckpointMonitor(name, Duration.ofSeconds(30));
   }

   @Override
   public <In, Out, Mat> Flow<In, Out, Mat> createLatencyMonitor(String name, Flow<In, Out, Mat> flow, Duration statsInterval) {
      return LatencyMonitors.create(flow, createLatencyStatsSink(name, statsInterval), (m, n) -> m);
   }

   @Override
   public <In, Out, Mat> Flow<In, Out, Mat> createLatencyMonitor(String name, Flow<In, Out, Mat> flow) {
      return createLatencyMonitor(name, flow, Duration.ofSeconds(30));
   }

}

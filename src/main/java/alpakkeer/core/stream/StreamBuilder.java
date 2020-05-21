package alpakkeer.core.stream;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;

import java.time.Duration;

public interface StreamBuilder {

   Sink<CheckpointMonitor.Stats, NotUsed> createCheckpointStatsSink(String name, Duration statsInterval);

   Sink<CheckpointMonitor.Stats, NotUsed> createCheckpointStatsSink(String name);

   Sink<LatencyMonitor.Stats, NotUsed> createLatencyStatsSink(String name, Duration statsInterval);

   Sink<LatencyMonitor.Stats, NotUsed> createLatencyStatsSink(String name);

   <T> Flow<T, T, NotUsed> createCheckpointMonitor(String name, Duration statsInterval);

   <T> Flow<T, T, NotUsed> createCheckpointMonitor(String name);

   <In, Out, Mat> Flow<In, Out, Mat> createLatencyMonitor(String name, Flow<In, Out, Mat> flow, Duration statsInterval);

   <In, Out, Mat> Flow<In, Out, Mat> createLatencyMonitor(String name, Flow<In, Out, Mat> flow);

}

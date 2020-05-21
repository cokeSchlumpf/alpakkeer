package alpakkeer.core.stream;

import akka.NotUsed;
import akka.japi.function.Procedure;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;

import java.time.Duration;

public final class CheckpointMonitors {

   private CheckpointMonitors() {

   }

   public static <A, Mat> Flow<A, A, Mat> create(Sink<CheckpointMonitor.Stats, Mat> statsSink) {
      return Flow.fromGraph(CheckpointMonitor.apply(statsSink.asScala()));
   }

   public static <A> Flow<A, A, NotUsed> create(Duration statsInterval, Procedure<CheckpointMonitor.Stats> onStats) {
      var flow = Flow.of(CheckpointMonitor.Stats.class)
         .via(Pulse.create(statsInterval, true))
         .to(Sink.foreach(onStats));

      return create(flow);
   }

}

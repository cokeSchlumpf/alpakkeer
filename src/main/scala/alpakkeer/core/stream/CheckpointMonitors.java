package alpakkeer.core.stream;

import akka.NotUsed;
import akka.japi.function.Procedure;
import akka.stream.FlowShape;
import akka.stream.Graph;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;

import java.time.Duration;

public final class CheckpointMonitors {

   private CheckpointMonitors() {

   }

   public static <A, Mat> Graph<FlowShape<A, A>, Mat> create(Sink<CheckpointMonitor.Stats, Mat> statsSink) {
      return CheckpointMonitor.apply(statsSink.asScala());
   }

   public static <A> Graph<FlowShape<A, A>, NotUsed> create(Duration statsInterval, Procedure<CheckpointMonitor.Stats> onStats) {
      var duration = scala.concurrent.duration.Duration.fromNanos(statsInterval.toNanos());
      var flow = Flow
         .fromGraph(new Pulse<CheckpointMonitor.Stats>(duration, true))
         .to(Sink.foreach(onStats));

      return create(flow);
   }

}

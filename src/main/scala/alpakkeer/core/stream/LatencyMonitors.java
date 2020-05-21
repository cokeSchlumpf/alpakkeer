package alpakkeer.core.stream;

import akka.japi.function.Function2;
import akka.japi.function.Procedure;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import alpakkeer.core.util.Operators;

import java.time.Duration;

public final class LatencyMonitors {

   private LatencyMonitors() {

   }

   public static <A, B, Mat1, Mat2, Mat3> Flow<A, B, Mat3> create(
      Flow<A, B, Mat1> flow,
      Sink<LatencyMonitor.Stats, Mat2> statsSink,
      Function2<Mat1, Mat2, Mat3> combineMat) {

      return LatencyMonitor
         .apply(flow.asScala(), statsSink.asScala(), (m1, m2) -> Operators.suppressExceptions(() -> combineMat.apply(m1, m2)))
         .asJava();
   }

   public static <A, B, Mat> Flow<A, B, Mat> create(
      Flow<A, B, Mat> flow,
      Duration statsInterval,
      Procedure<LatencyMonitor.Stats> onStats) {

      var fl = Flow.of(LatencyMonitor.Stats.class)
         .via(Pulse.create(statsInterval, true))
         .to(Sink.foreach(onStats));

      return create(flow, fl, Keep.left());
   }

}

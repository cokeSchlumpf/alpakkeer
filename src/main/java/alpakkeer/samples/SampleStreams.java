package alpakkeer.samples;

import akka.Done;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.CheckpointMonitors;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public final class SampleStreams {

   private SampleStreams() {

   }

   public static RunnableGraph<CompletionStage<Done>> checkpointMonitorSample() {
      return Source
         .range(0, Integer.MAX_VALUE)
         .takeWithin(Duration.ofMinutes(1))
         .throttle(42, Duration.ofMillis(320))
         .via(CheckpointMonitors.create(Duration.ofSeconds(10), System.out::println))
         .toMat(Sink.ignore(), Keep.right());
   }

}

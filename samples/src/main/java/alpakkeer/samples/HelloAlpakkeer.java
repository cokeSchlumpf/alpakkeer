package alpakkeer.samples;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.jobs.ContextStores;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.javadsl.Alpakkeer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class HelloAlpakkeer {

   public static void main(String... args) {
      var flow = Flow.<String>create().map(String::toUpperCase)
      Alpakkeer
         .create()
         .configure(r -> r.withContextStore(ContextStores.apply().create()))
         .withProcess(process -> process
            .create("hello-world")
            .runGraph(b -> b
               .messaging()
               .recordsSource("topic", String.class)
               .throttle(1, Duration.ofSeconds(1))
               .map(r -> r.getValue())
               .via(b.getMonitoring().createLatencyMonitor("sub-flow", flow, Duration.ofSeconds(10)))
               .toMat(Sink.foreach(System.out::println), Keep.right()))
            .withLoggingMonitor()
            .initializeStopped()
         .withInitialRetryBackoff(Duration.ofSeconds(30))
         .withRetryBackoffResetTimeout(Duration.ofMinutes(10)))

         .withJob(jobs -> jobs
            .create("Hello World")
            .runGraph(b -> Source
               .single("Hello World")
               .toMat(Sink.foreach(System.out::println), Keep.right()))
            .withScheduledExecution(CronExpression.everySeconds(10))
            .withHistoryMonitor()
            .withLoggingMonitor()
            .withApiEndpoint((api, job) -> {
               api.get("process", ctx -> {
                  ctx.result(job.start().toCompletableFuture());
               });
            })
            .withConfiguration())
         .start();
   }

}

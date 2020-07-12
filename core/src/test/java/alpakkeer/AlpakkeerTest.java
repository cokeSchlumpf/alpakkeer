package alpakkeer;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class AlpakkeerTest {

   @Test
   public void simpleTest() throws ExecutionException, InterruptedException {
      var alpakkeer = Alpakkeer
         .create()
         .withJob(
            jobs -> jobs
               .create("hello-world")
               .runGraph(sb -> Source
                  .single("Hello World")
                  .toMat(Sink.foreach(System.out::println), Keep.right()))
               .withLoggingMonitor())
         .start();

      alpakkeer
         .getResources()
         .getJob("hello-world")
         .start()
         .toCompletableFuture()
         .get();
   }

}

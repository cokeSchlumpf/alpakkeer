package alpakkeer;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.javadsl.Alpakkeer;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

      alpakkeer.stop().toCompletableFuture().get();
   }

   @Test
   public void messagingTest() throws InterruptedException, ExecutionException {
      var list = Lists.<String>newArrayList();

      var alpakkeer = Alpakkeer
         .create()
         .withJob(
            jobs -> jobs
               .create("hello-world")
               .runGraph(sb -> Source
                  .from(Lists.newArrayList("Hallo", "Welt"))
                  .toMat(sb.messaging().itemsSink("test"), Keep.right()))
               .withLoggingMonitor())
         .withProcess(p -> p
            .create("process")
            .runGraph(sb -> sb
               .messaging()
               .recordsSource("test", String.class)
               .map(record -> {
                  list.add(record.getValue());
                  record.getContext().commit();
                  return record;
               })
               .toMat(Sink.ignore(), Keep.right()))
            .withLoggingMonitor())
         .start();

      alpakkeer
         .getResources()
         .getJob("hello-world")
         .start();

      Thread.sleep(2000);

      assertTrue(list.contains("Hallo"));
      assertTrue(list.contains("Welt"));
      assertEquals(2, list.size());

      alpakkeer.stop().toCompletableFuture().get();
   }

}

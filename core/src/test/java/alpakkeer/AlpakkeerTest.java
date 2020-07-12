package alpakkeer;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.RecordEnvelope;
import alpakkeer.core.stream.Records;
import com.google.common.collect.Lists;
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

   @Test
   public void messagingTest() throws InterruptedException {
      var list = Lists.<String>newArrayList();

      var alpakkeer = Alpakkeer
         .create()
         .withJob(
            jobs -> jobs
               .create("hello-world")
               .runGraph(sb -> Source
                  .from(Lists.newArrayList("Hallo", "Welt"))
                  .map(Records::apply)
                  .map(RecordEnvelope::apply)
                  .toMat(sb.messaging().toTopic("test"), Keep.right()))
               .withLoggingMonitor())
         .withProcess(p -> p
            .create("process")
            .runGraph(sb -> sb
               .messaging()
               .fromTopicAsSimpleRecord("test", String.class)
               .map(record -> {
                  list.add(record.getRecord().getValue());
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

      assert (list.contains("Hallo"));
      assert (list.contains("Welt"));
      assert (list.size() == 2);
   }

}

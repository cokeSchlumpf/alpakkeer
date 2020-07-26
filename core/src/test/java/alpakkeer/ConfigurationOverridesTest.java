package alpakkeer;

import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.javadsl.Alpakkeer;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

public class ConfigurationOverridesTest {

   @AllArgsConstructor(staticName = "apply")
   @NoArgsConstructor(access = AccessLevel.PRIVATE)
   public static class TestProperties {

      int count;

      String name;

   }

   @Test
   public void testOverrides() throws InterruptedException, ExecutionException {
      var result = Lists.<String>newArrayList();

      var alpakkeer = Alpakkeer
         .create()
         .withJob(jobs -> jobs
            .create("goofy", TestProperties.apply(3, "micky"))
            .runGraph(sb -> Source
               .single(sb.getProperties())
               .mapConcat(p -> IntStream.range(0, p.count).mapToObj(i -> p.name + " " + (i + 1)).collect(Collectors.toList()))
               .map(e -> {
                  result.add(e);
                  return e;
               })
               .toMat(Sink.foreach(System.out::println), Keep.right()))
            .withScheduledExecution(CronExpression.everyMinute())
            .withConfiguration())
         .start();

      Thread.sleep(10000);
      alpakkeer.stop().toCompletableFuture().get();

      assertTrue(result.contains("goofy 12"));
      assertTrue(result.contains("goofy 1"));
   }

}

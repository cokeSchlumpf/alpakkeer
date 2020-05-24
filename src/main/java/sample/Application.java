package sample;

import alpakkeer.Alpakkeer;
import alpakkeer.core.jobs.ContextStores;
import alpakkeer.core.monitoring.MetricCollectors;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import alpakkeer.samples.SampleStreams;
import io.prometheus.client.CollectorRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDateTime;

public class Application {

   @Value
   @AllArgsConstructor(staticName = "apply")
   @NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
   public static class MyProperties {
      String text;
   }

   public static void main(String... args) {
      var alpakkeer = Alpakkeer
         .create()
         .configure(cfg -> {
            cfg.withContextStore(ContextStores.apply().create());
            cfg.withCollectorRegistry(CollectorRegistry.defaultRegistry);
            cfg.addMetricsCollector(MetricCollectors.createInMemory(CronExpression.everySeconds(1)));
         })
         .withJob(builder -> builder
            .create("sample-job", MyProperties.apply("hello"), LocalDateTime.now())
            .runGraph((id, props, context, sb) -> SampleStreams
               .twitter(sb)
               .mapMaterializedValue(i -> i.thenApply(d -> LocalDateTime.now())))
            // .withHistoryMonitor(100)
            .withPrometheusMonitor()
            .withScheduledExecution(CronExpression.everyMinute())
            .build())
         .start();

      Operators.suppressExceptions(() -> Thread.sleep(Duration.ofMinutes(300).toMillis()));
      alpakkeer.stop();
   }

}

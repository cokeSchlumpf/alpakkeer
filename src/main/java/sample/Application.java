package sample;

import alpakkeer.Alpakkeer;
import alpakkeer.core.jobs.ContextStores;
import alpakkeer.core.monitoring.MetricCollectors;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import alpakkeer.samples.SampleStreams;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;
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
      Counter counter = Counter
         .build("test", "foo bar else")
         .register(CollectorRegistry.defaultRegistry);

      counter.inc();
      counter.inc(3.8);

      Gauge gauge = Gauge
         .build("test_2", "lorem ipsum")
         .register(CollectorRegistry.defaultRegistry);

      gauge.set(3.8);
      gauge.set(25);

      Summary summary = Summary
         .build("test_3", "lorem ipsumm")
         .labelNames("egon")
         .register(CollectorRegistry.defaultRegistry);

      for (int i = 0; i < 100; i++) {
         summary.labels("foo").observe(i);
      }

      CollectorRegistry
         .defaultRegistry
         .metricFamilySamples()
         .asIterator()
         .forEachRemaining(family -> {
            System.out.println(family.name);
            System.out.println(family.type);
            System.out.println(family.help);
            System.out.println(family.samples.size());
            System.out.println(family.samples);
         });

      var alpakkeer = Alpakkeer
         .create()
         .configure(cfg -> {
            cfg.withContextStore(ContextStores.apply().create());
            cfg.withCollectorRegistry(CollectorRegistry.defaultRegistry);
            cfg.addMetricsCollector(MetricCollectors.createInMemory());
         })
         .withJob(builder -> builder
            .create("sample-job", MyProperties.apply("hello"), LocalDateTime.now())
            .runGraph((id, props, context, sb) -> SampleStreams
               .twitter(sb)
               .mapMaterializedValue(i -> i.thenApply(d -> LocalDateTime.now())))
            .withHistoryMonitor(100)
            .withScheduledExecution(CronExpression.everyMinute())
            .build())
         .start();

      Operators.suppressExceptions(() -> Thread.sleep(Duration.ofMinutes(300).toMillis()));
      alpakkeer.stop();
   }

}

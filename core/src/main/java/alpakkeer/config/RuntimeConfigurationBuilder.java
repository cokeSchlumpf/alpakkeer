package alpakkeer.config;

import akka.actor.ActorSystem;
import alpakkeer.core.jobs.ContextStore;
import alpakkeer.core.jobs.ContextStores;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.CronSchedulers;
import alpakkeer.core.util.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.prometheus.client.CollectorRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public class RuntimeConfigurationBuilder {

   ActorSystem system;

   ObjectMapper objectMapper;

   CollectorRegistry collectorRegistry;

   ContextStore contextStore;

   List<MetricsCollector> metricsCollectors;

   CronScheduler scheduler;

   public static RuntimeConfigurationBuilder apply() {
      return apply(null, null, null, null, Lists.newArrayList(), null);
   }

   public RuntimeConfigurationBuilder addMetricsCollector(MetricsCollector collector) {
      metricsCollectors.add(collector);
      return this;
   }

   public RuntimeConfigurationBuilder withActorSystem(ActorSystem system) {
      this.system = system;
      return this;
   }

   public RuntimeConfigurationBuilder withObjectMapper(ObjectMapper om) {
      this.objectMapper = om;
      return this;
   }

   public RuntimeConfigurationBuilder withCollectorRegistry(CollectorRegistry collectorRegistry) {
      this.collectorRegistry = collectorRegistry;
      return this;
   }

   public RuntimeConfigurationBuilder withContextStore(ContextStore contextStore) {
      this.contextStore = contextStore;
      return this;
   }

   public RuntimeConfigurationBuilder withScheduler(CronScheduler scheduler) {
      this.scheduler = scheduler;
      return this;
   }

   public RuntimeConfiguration build() {
      return RuntimeConfiguration.apply(
         Optional.ofNullable(system).orElseGet(() -> ActorSystem.apply("alpakkeer")),
         Optional.ofNullable(objectMapper).orElseGet(() -> ObjectMapperFactory.apply().create(true)),
         Optional.ofNullable(collectorRegistry).orElse(CollectorRegistry.defaultRegistry),
         Optional.ofNullable(contextStore).orElseGet(() -> ContextStores.apply().create()),
         List.copyOf(metricsCollectors),
         Optional.ofNullable(scheduler).orElseGet(CronSchedulers::apply));
   }

}

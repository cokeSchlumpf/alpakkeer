package alpakkeer.javadsl;

import akka.actor.ActorSystem;
import akka.japi.Function;
import alpakkeer.api.AlpakkeerOpenApi;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.jobs.context.ContextStore;
import alpakkeer.core.jobs.context.ContextStores;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.CronSchedulers;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import alpakkeer.core.stream.messaging.StreamMessagingAdapters;
import alpakkeer.core.util.ObjectMapperFactory;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.javalin.Javalin;
import io.prometheus.client.CollectorRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public final class AlpakkeerRuntimeBuilder {

   Javalin app;

   ActorSystem system;

   ObjectMapper objectMapper;

   CollectorRegistry collectorRegistry;

   Function<AlpakkeerBaseRuntime, ContextStore> contextStore;

   List<MetricsCollector> metricsCollectors;

   CronScheduler scheduler;

   Function<AlpakkeerBaseRuntime, StreamMessagingAdapter> streamMessagingAdapter;

   public static AlpakkeerRuntimeBuilder apply() {
      return apply(null, null, null, null, null, Lists.newArrayList(), null, null);
   }

   public AlpakkeerRuntimeBuilder addMetricsCollector(MetricsCollector collector) {
      metricsCollectors.add(collector);
      return this;
   }

   public AlpakkeerRuntimeBuilder withJavalinApp(Javalin app) {
      this.app = app;
      return this;
   }

   public AlpakkeerRuntimeBuilder withActorSystem(ActorSystem system) {
      this.system = system;
      return this;
   }

   public AlpakkeerRuntimeBuilder withObjectMapper(ObjectMapper om) {
      this.objectMapper = om;
      return this;
   }

   public AlpakkeerRuntimeBuilder withCollectorRegistry(CollectorRegistry collectorRegistry) {
      this.collectorRegistry = collectorRegistry;
      return this;
   }

   public AlpakkeerRuntimeBuilder withContextStore(ContextStore contextStore) {
      this.contextStore = r -> contextStore;
      return this;
   }

   public AlpakkeerRuntimeBuilder withContextStore(Function<AlpakkeerBaseRuntime, ContextStore> contextStoreFactory) {
      this.contextStore = contextStoreFactory;
      return this;
   }

   public AlpakkeerRuntimeBuilder withMessagingAdapter(StreamMessagingAdapter messagingAdapter) {
      this.streamMessagingAdapter = r -> messagingAdapter;
      return this;
   }

   public AlpakkeerRuntimeBuilder withMessagingAdapter(Function<AlpakkeerBaseRuntime, StreamMessagingAdapter> messagingAdapterFactory) {
      this.streamMessagingAdapter = messagingAdapterFactory;
      return this;
   }

   public AlpakkeerRuntimeBuilder withScheduler(CronScheduler scheduler) {
      this.scheduler = scheduler;
      return this;
   }

   public AlpakkeerRuntime build(AlpakkeerConfiguration config) {
      var system = Optional.ofNullable(this.system).orElseGet(() -> ActorSystem.apply("alpakkeer"));
      var objectMapper = Optional.ofNullable(this.objectMapper).orElseGet(() -> ObjectMapperFactory.apply().create(true));
      var javalin = Optional.ofNullable(app).orElseGet(() -> Javalin
         .create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.registerPlugin(AlpakkeerOpenApi.apply(config));
            cfg.enableCorsForAllOrigins();
         })
         .start(config.getApi().getHostname(), config.getApi().getPort()));

      var baseRuntime = AlpakkeerBaseRuntime.apply(javalin, config, system, objectMapper);

      var streamMessaging = Operators.suppressExceptions(() -> Optional
         .ofNullable(this.streamMessagingAdapter)
         .orElseGet(() -> StreamMessagingAdapters::createFromConfiguration)
         .apply(baseRuntime));

      var contextStore = Operators.suppressExceptions(() -> Optional
         .ofNullable(this.contextStore)
         .orElseGet(() -> ContextStores::createFromConfiguration)
         .apply(baseRuntime));

      return AlpakkeerRuntime.apply(
         javalin,
         config,
         system,
         objectMapper,
         Optional.ofNullable(collectorRegistry).orElse(CollectorRegistry.defaultRegistry),
         contextStore,
         List.copyOf(metricsCollectors),
         Optional.ofNullable(scheduler).orElseGet(CronSchedulers::apply),
         streamMessaging);
   }

}

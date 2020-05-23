package alpakkeer.config;

import akka.actor.ActorSystem;
import akka.stream.javadsl.RunnableGraph;
import alpakkeer.core.jobs.ContextStore;
import alpakkeer.core.jobs.ContextStores;
import alpakkeer.core.util.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public class RuntimeConfigurationBuilder {

   ActorSystem system;

   ObjectMapper objectMapper;

   CollectorRegistry collectorRegistry;

   ContextStore contextStore;

   public static RuntimeConfigurationBuilder apply() {
      return apply(null, null, null, null);
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

   public RuntimeConfiguration build() {
      return RuntimeConfiguration.apply(
         Optional.ofNullable(system).orElseGet(() -> ActorSystem.apply("alpakkeer")),
         Optional.ofNullable(objectMapper).orElseGet(() -> ObjectMapperFactory.apply().create(true)),
         Optional.ofNullable(collectorRegistry).orElse(CollectorRegistry.defaultRegistry),
         Optional.ofNullable(contextStore).orElseGet(() -> ContextStores.apply().create()));
   }

}

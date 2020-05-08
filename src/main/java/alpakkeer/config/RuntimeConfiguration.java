package alpakkeer.config;

import alpakkeer.core.jobs.ContextStore;
import alpakkeer.core.jobs.ContextStores;
import alpakkeer.core.util.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "apply")
public final class RuntimeConfiguration {

   ObjectMapper objectMapper;

   CollectorRegistry collectorRegistry;

   ContextStore contextStore;

   public static RuntimeConfiguration apply() {
      return apply(
         ObjectMapperFactory.apply().create(true),
         CollectorRegistry.defaultRegistry,
         ContextStores.apply().create());
   }

   public RuntimeConfiguration withObjectMapper(ObjectMapper om) {
      this.objectMapper = om;
      return this;
   }

   public RuntimeConfiguration withCollectorRegistry(CollectorRegistry registry) {
      this.collectorRegistry = registry;
      return this;
   }

   public RuntimeConfiguration withContextStore(ContextStore contextStore) {
      this.contextStore = contextStore;
      return this;
   }

}

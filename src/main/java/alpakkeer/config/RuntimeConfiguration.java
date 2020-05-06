package alpakkeer.config;

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

   public static RuntimeConfiguration apply() {
      return apply(ObjectMapperFactory.apply().create(true), CollectorRegistry.defaultRegistry);
   }

   public RuntimeConfiguration withObjectMapper(ObjectMapper om) {
      this.objectMapper = om;
      return this;
   }

   public RuntimeConfiguration withCollectorRegistry(CollectorRegistry registry) {
      this.collectorRegistry = registry;
      return this;
   }

}

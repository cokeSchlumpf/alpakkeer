package alpakkeer.config;

import akka.actor.ActorSystem;
import alpakkeer.core.jobs.ContextStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "apply")
public final class RuntimeConfiguration {

   ActorSystem system;

   ObjectMapper objectMapper;

   CollectorRegistry collectorRegistry;

   ContextStore contextStore;

}

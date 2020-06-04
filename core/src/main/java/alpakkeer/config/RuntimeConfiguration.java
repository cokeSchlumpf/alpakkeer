package alpakkeer.config;

import akka.actor.ActorSystem;
import alpakkeer.core.jobs.ContextStore;
import alpakkeer.core.monitoring.MetricsCollector;
import alpakkeer.core.scheduler.CronScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.prometheus.client.CollectorRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(staticName = "apply")
public final class RuntimeConfiguration {

   Javalin app;

   AlpakkeerConfiguration configuration;

   ActorSystem system;

   ObjectMapper objectMapper;

   CollectorRegistry collectorRegistry;

   ContextStore contextStore;

   List<MetricsCollector> metricsCollectors;

   CronScheduler scheduler;

}

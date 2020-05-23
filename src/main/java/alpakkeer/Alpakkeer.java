package alpakkeer;

import akka.actor.ActorSystem;
import alpakkeer.api.AlpakkeerAPI;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.util.Templates;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Alpakkeer {

   private static final Logger LOG = LoggerFactory.getLogger(Alpakkeer.class);

   private final ActorSystem system;

   private final CronScheduler scheduler;

   private final AlpakkeerAPI api;

   static Alpakkeer apply(RuntimeConfiguration runtimeConfiguration, CronScheduler scheduler, Resources resources) {
      var config = AlpakkeerConfiguration.apply();
      var api = AlpakkeerAPI.apply(config, scheduler, resources, runtimeConfiguration.getObjectMapper());

      var banner = Templates.renderTemplateFromResources("banner.twig", ImmutableMap.<String, Object>builder()
         .put("version", config.getVersion())
         .put("environment", config.getEnvironment())
         .build());

      LOG.info(banner);

      return new Alpakkeer(runtimeConfiguration.getSystem(), scheduler, api);
   }

   public static AlpakkeerBuilder create() {
      return AlpakkeerBuilder.apply();
   }

   public static void main(String... args) {

   }

   public void stop() {
      LOG.info("Terminating Alpakkeer");

      scheduler
         .terminate()
         .thenAccept(done -> LOG.info("... scheduler stopped"));

      system.terminate();
      system
         .getWhenTerminated()
         .thenAccept(terminated -> LOG.info("... actor system stopped"));

      api.stop();
      LOG.info("... API server stopped");
   }

}

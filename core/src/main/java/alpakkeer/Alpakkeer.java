package alpakkeer;

import alpakkeer.api.AlpakkeerAPI;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.util.Templates;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central entry-point for using Alpakkeer DSL.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Alpakkeer {

   private static final Logger LOG = LoggerFactory.getLogger(Alpakkeer.class);

   private final AlpakkeerRuntime runtimeConfiguration;

   private final Resources resources;

   private final AlpakkeerAPI api;

   /**
    * Creates a new instance.
    *
    * @param runtimeConfiguration Alpakkeer's runtime configuration
    * @param resources            Initial resources for Alpakkeer
    * @return The new alpakkeer instance
    */
   static Alpakkeer apply(AlpakkeerConfiguration config, AlpakkeerRuntime runtimeConfiguration, Resources resources) {
      var api = AlpakkeerAPI.apply(runtimeConfiguration, resources);

      if (runtimeConfiguration.getConfiguration().isShowBanner()) {
         var banner = Templates.renderTemplateFromResources("banner.twig", ImmutableMap.<String, Object>builder()
            .put("version", config.getVersion())
            .put("environment", config.getEnvironment())
            .build());

         LOG.info(banner);
      }

      return new Alpakkeer(runtimeConfiguration, resources, api);
   }

   /**
    * Use this method start the definition of an Alpakkeer application.
    *
    * @return The Alpakkeer builder DSL
    */
   public static AlpakkeerBuilder create() {
      return AlpakkeerBuilder.apply();
   }

   /**
    * Returns the resource manager of this Alpakkeer instance.
    *
    * @return The resource manager
    */
   public Resources getResources() {
      return resources;
   }

   /**
    * Gracefully shuts down all processes and the system as a whole.
    */
   public void stop() {
      LOG.info("Terminating Alpakkeer");

      runtimeConfiguration
         .getScheduler()
         .terminate()
         .thenAccept(done -> LOG.info("... scheduler stopped"));

      runtimeConfiguration
         .getSystem()
         .terminate();

      runtimeConfiguration
         .getSystem()
         .getWhenTerminated()
         .thenAccept(terminated -> LOG.info("... actor system stopped"));

      api.stop();
      LOG.info("... API server stopped");
   }

}

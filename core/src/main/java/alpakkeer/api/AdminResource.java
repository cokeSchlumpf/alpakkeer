package alpakkeer.api;

import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.config.Configs;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.model.JobDetails;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public final class AdminResource {

   @Value
   @AllArgsConstructor(staticName = "apply")
   public static class About {

      String environment;

      String version;

   }

   private final AlpakkeerConfiguration config;

   private final CronScheduler scheduler;

   public Handler getAbout() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Application Info");
            op.description("Returns basic meta information of the application.");
            op.addTagsItem("Admin");
         })
         .json("200", About.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         ctx.json(About.apply(config.getEnvironment(), config.getVersion()));
      });
   }

   public Handler getConfig() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Application Configuration");
            op.description("Returns the active application configuration if enabled. This function should be disabled in most environments as it may expose passwords and other secrets!");
            op.addTagsItem("Admin");
         })
         .result("200", String.class, "text/plain");

      return OpenApiBuilder.documented(docs, ctx -> {
         if (config.isExposeConfig()) {
            ctx.result(Configs.asString(Configs.application));
         } else {
            ctx.result("Configuration is not exposed. Change `alpakkeer.expose-config`-Setting to expose the configuration.");
         }
      });
   }

   public Handler getJobs() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("CronJobs");
            op.description("Returns a list of scheduled CronJobs of the system.");
            op.addTagsItem("Admin");
         })
         .jsonArray("200", JobDetails.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         ctx.json(scheduler.getJobs().toCompletableFuture());
      });
   }

}

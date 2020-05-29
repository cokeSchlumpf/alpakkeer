package alpakkeer.api;

import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.jobs.model.JobStatus;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.model.JobDetails;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
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

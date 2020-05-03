package alpakkeer.api;

import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.model.JobDetails;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class AdminResource {

   private final CronScheduler scheduler;

   @OpenApi(
      summary = "List current cron-jobs",
      description = "Returns a list of scheduled cron-jobs of the system.",
      responses = {
         @OpenApiResponse(status = "200", content = @OpenApiContent(from = JobDetails.class, isArray = true))
      })
   public void getJobs(Context ctx) {
      ctx.json(scheduler.getJobs().toCompletableFuture());
   }

}

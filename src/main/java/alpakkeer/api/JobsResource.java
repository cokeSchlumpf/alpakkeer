package alpakkeer.api;

import alpakkeer.core.jobs.Job;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class JobsResource {

   @Value
   @AllArgsConstructor(staticName = "apply")
   public static class StartExecution {

   }

   public static final String PARAM_NAME = "name";

   private final Resources resources;

   public void getJobs(Context ctx) {
      var eventualStatusList = resources.getJobs().stream().map(Job::getStatusUnchecked).collect(Collectors.toList());
      ctx.json(Operators.allOf(eventualStatusList).toCompletableFuture());
   }

   public void getJob(Context ctx) {
      var name = Name.apply(ctx.pathParam(PARAM_NAME));

      resources.getJob(name).ifPresentOrElse(
         job -> ctx.json(job.getStatus().toCompletableFuture()),
         () -> notFound(name));
   }

   public void getDefaultProperties(Context ctx) {
      var name = Name.apply(ctx.pathParam(PARAM_NAME));

      resources.getJob(name).ifPresentOrElse(
         job -> ctx.json(job.getDefinition().getDefaultProperties()),
         () -> notFound(name));
   }

   private void notFound(Name name) {
      throw new NotFoundResponse(String.format("No job found with name `%s`", name.getValue()));
   }

}

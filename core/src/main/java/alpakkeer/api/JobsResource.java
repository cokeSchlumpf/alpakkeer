package alpakkeer.api;

import alpakkeer.core.jobs.Job;
import alpakkeer.core.jobs.exceptions.AlreadyRunningException;
import alpakkeer.core.jobs.model.JobStatus;
import alpakkeer.core.jobs.model.JobStatusDetails;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.util.Json;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class JobsResource {

   @Value
   @NoArgsConstructor(force = true)
   @AllArgsConstructor(staticName = "apply")
   public static class StartExecution<P> {

      boolean queue;

      P properties;

   }

   public static final String PARAM_NAME = "name";

   private final Resources resources;

   private final ObjectMapper om;

   public Handler getJobs() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Jobs Overview");
            op.description("Returns a list of all existing jobs including some general meta information.");
            op.addTagsItem("Jobs");
         })
         .jsonArray("200", JobStatus.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var eventualStatusList = resources.getJobs().stream().map(Job::getStatus).collect(Collectors.toList());
         ctx.json(Operators.allOf(eventualStatusList).toCompletableFuture());
      });
   }

   public Handler getJob() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Job Details");
            op.description("Returns detailed information of a specific job.");
            op.addTagsItem("Jobs");
         })
         .pathParam("name", String.class, p -> p.description("The name of the job"))
         .json("200", JobStatusDetails.class);


      return OpenApiBuilder.documented(docs, ctx -> {
         var name = Name.apply(ctx.pathParam(PARAM_NAME));

         resources.getJob(name).ifPresentOrElse(
            job -> ctx.json(job.getStatusDetails().toCompletableFuture()),
            () -> notFound(name));
      });
   }

   public Handler getTriggerJobExample() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Run Example Request");
            op.description(
               "Returns an example request body for running the job, including default execution properties. " +
                  "These might be modified before sending the request.");
            op.addTagsItem("Jobs");
         })
         .pathParam("name", String.class, p -> p.description("The name of the job"))
         .json("200", StartExecution.class);


      return OpenApiBuilder.documented(docs, ctx -> {
         var name = Name.apply(ctx.pathParam(PARAM_NAME));

         resources.getJob(name).ifPresentOrElse(
            job -> ctx.json(StartExecution.apply(true, job.getDefinition().getDefaultProperties())),
            () -> notFound(name));
      });
   }

   @SuppressWarnings("unchecked")
   public Handler triggerJob() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Trigger Job");
            op.description(
               "Triggers a job execution. Use Option method to get example properties for the job.");
            op.addTagsItem("Jobs");
         })
         .pathParam("name", String.class, p -> p.description("The name of the job"))
         .body(StartExecution.class)
         .result("202", List.of(), r -> r.setDescription("If the request is queued for start processing"))
         .result("425", List.of(), r -> r.setDescription("If the job is already running and queue was set to False"));

      return OpenApiBuilder.documented(docs, ctx -> {
         var name = Name.apply(ctx.pathParam(PARAM_NAME));

         resources.getJob(name).ifPresentOrElse(
            job -> {
               var json = ctx.body();
               var request = Json.apply(om).deserializeGenericClassFromJson(json, StartExecution.class, job.getDefinition().getDefaultProperties().getClass());
               var result = ((Job<Object, Object>) job).start(request.getProperties(), request.isQueue());

               result.whenComplete((done, ex) -> {
                  if (ex != null && Operators.isCause(AlreadyRunningException.class, ex)) {
                     ctx.status(425);
                  } else if (ex != null) {
                     ctx.status(500);
                  } else {
                     ctx.status(202);
                  }
               });
            },
            () -> notFound(name));
      });
   }

   public Handler stop() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Stop Job Execution");
            op.description("Stops the current job execution.");
            op.addTagsItem("Jobs");
         })
         .pathParam("name", String.class, p -> p.description("The name of the job"))
         .queryParam("clear", Boolean.class, false, p -> p.description("Set this parameter to clear the whole queue"))
         .result("200");

      return OpenApiBuilder.documented(docs, ctx -> {
         var name = Name.apply(ctx.pathParam(PARAM_NAME));
         var clearQueue = ctx.queryParam("clear") != null;

         resources.getJob(name).ifPresentOrElse(
            job -> ctx.result(job.cancel(clearQueue).thenApply(d -> "Ok").toCompletableFuture()),
            () -> notFound(name)
         );
      });
   }

   private void notFound(Name name) {
      throw new NotFoundResponse(String.format("No job found with name `%s`", name.getValue()));
   }


}

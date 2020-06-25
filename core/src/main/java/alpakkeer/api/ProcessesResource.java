package alpakkeer.api;

import alpakkeer.core.processes.Process;
import alpakkeer.core.processes.model.ProcessStatus;
import alpakkeer.core.processes.model.ProcessStatusDetails;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.util.Operators;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import lombok.AllArgsConstructor;

import java.util.stream.Collectors;

@AllArgsConstructor
public final class ProcessesResource {

   public static final String PARAM_NAME = "name";

   private final Resources resources;

   public Handler getProcesses() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Processes Overview");
            op.description("Returns a list of all existing processes including some general meta information.");
            op.addTagsItem("Processes");
         })
         .jsonArray("200", ProcessStatus.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var eventualStatusList = resources.getProcesses().stream().map(Process::getStatus).collect(Collectors.toList());
         ctx.json(Operators.allOf(eventualStatusList).toCompletableFuture());
      });
   }

   public Handler getProcess() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Process Details");
            op.description("Returns detailed information of a specific process.");
            op.addTagsItem("Processes");
         })
         .pathParam("name", String.class, p -> p.description("The name of the process"))
         .json("200", ProcessStatusDetails.class);


      return OpenApiBuilder.documented(docs, ctx -> {
         var name = ctx.pathParam(PARAM_NAME);

         resources.findProcess(name).ifPresentOrElse(
            job -> ctx.json(job.getStatusDetails().toCompletableFuture()),
            () -> notFound(name));
      });
   }

   public Handler start() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Start Process");
            op.description("Starts the process execution.");
            op.addTagsItem("Processes");
         })
         .pathParam("name", String.class, p -> p.description("The name of the process"))
         .result("200");

      return OpenApiBuilder.documented(docs, ctx -> {
         var name = ctx.pathParam(PARAM_NAME);

         resources.findProcess(name).ifPresentOrElse(
            process -> ctx.result(process.start().thenApply(d -> "Ok").toCompletableFuture()),
            () -> notFound(name)
         );
      });
   }

   public Handler stop() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Stop Process");
            op.description("Stops the process execution.");
            op.addTagsItem("Processes");
         })
         .pathParam("name", String.class, p -> p.description("The name of the process"))
         .result("200");

      return OpenApiBuilder.documented(docs, ctx -> {
         var name = ctx.pathParam(PARAM_NAME);

         resources.findProcess(name).ifPresentOrElse(
            process -> ctx.result(process.stop().thenApply(d -> "Ok").toCompletableFuture()),
            () -> notFound(name)
         );
      });
   }

   private void notFound(String name) {
      throw new NotFoundResponse(String.format("No process found with name `%s`", name));
   }


}

package alpakkeer.api;

import alpakkeer.core.monitoring.Marker;
import alpakkeer.core.monitoring.Metric;
import alpakkeer.core.monitoring.TimeSeries;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.util.Operators;
import alpakkeer.core.util.Strings;
import alpakkeer.core.values.grafana.*;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import lombok.AllArgsConstructor;
import scala.Tuple2;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class MetricsResource {

   public static final String PARAM_NAME = "name";

   private final Resources resources;

   public Handler getHealth() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Test connection");
            op.description("Returns HTTP 200 for Grafana's `Test connection` call.");
            op.addTagsItem("Metrics");
         })
         .jsonArray("200", String.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var metrics = getTimeSeriesMetrics();
         ctx.json(metrics.keySet());
      });
   }

   public Handler search() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Search");
            op.description("Returns available metrics for the job.");
            op.addTagsItem("Metrics");
         })
         .body(SearchRequest.class)
         .jsonArray("200", String.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var metrics = getTimeSeriesMetrics();
         ctx.json(metrics.keySet());
      });
   }

   public Handler searchAnnotations() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Search Annotations");
            op.description("Returns available annotations for the job.");
            op.addTagsItem("Metrics");
         })
         .jsonArray("200", String.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var metrics = getMarkers();
         ctx.json(metrics.keySet());
      });
   }

   public Handler query() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Query");
            op.description("Returns job metrics based on input");
            op.addTagsItem("Metrics");
         })
         .body(SearchRequest.class)
         .jsonArray("200", TimeSeries.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var metrics = getTimeSeriesMetrics();
         var request = ctx.bodyAsClass(QueryRequest.class);
         var from = request.getRange().getFrom().toInstant();
         var to = request.getRange().getTo().toInstant();

         var result = Operators.allOf(request
            .getTargets()
            .stream()
            .map(target -> {
               if (target.getType().toLowerCase().equals("timeseries") && metrics.containsKey(target.getTarget())) {
                  CompletionStage<Object> gts = metrics.get(target.getTarget()).query(from, to).thenApply(ts ->
                     alpakkeer.core.values.grafana.TimeSeries.apply(
                        target.getTarget(),
                        ts.getData().stream().map(dp -> DataPoint.apply(
                           dp.getMoment().toEpochMilli(),
                           dp.getValue())).collect(Collectors.toList())));

                  return Optional.of(gts);
               } else {
                  return Optional.<CompletionStage<Object>>empty();
               }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList()));

         ctx.json(result.toCompletableFuture());
      });
   }

   public Handler queryAnnotations() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Annotations");
            op.description("Returns queried markers as Grafana annotations.");
            op.addTagsItem("Metrics");
         })
         .body(AnnotationRequest.class)
         .jsonArray("200", Annotation.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var markers = getMarkers();
         var request = ctx.bodyAsClass(AnnotationRequest.class);
         var from = request.getRange().getFrom().toInstant();
         var to = request.getRange().getTo().toInstant();

         var result = Operators.allOf(List.of(request.getAnnotation().getQuery().split(","))
            .stream()
            .map(String::trim)
            .filter(markers::containsKey)
            .map(target -> markers
               .get(target)
               .query(from, to)
               .thenApply(mks -> mks
                  .stream()
                  .map(marker -> Annotation.apply(
                     marker.getTitle(),
                     marker.getText().orElse(null),
                     marker.getTo().isPresent(),
                     marker.getFrom().toEpochMilli(),
                     marker.getTo().map(Instant::toEpochMilli).orElse(null),
                     marker.getTags()))
                  .collect(Collectors.toList())))
            .collect(Collectors.toList()))
            .thenApply(l -> l.stream().flatMap(Collection::stream).collect(Collectors.toList()));

         ctx.json(result.toCompletableFuture());
      });
   }

   private Map<String, Metric<List<Marker>>> getMarkers() {
      return resources
         .getJobs()
         .stream()
         .flatMap(job -> job
            .getDefinition()
            .getMonitors()
            .getMetricsMonitors()
            .getMarkerMetrics()
            .stream()
            .map(m -> Tuple2.apply(job.getDefinition().getName().getValue(), m)))
         .collect(Collectors.toMap(
            t -> {
               var jobName = t._1();
               var metricName = t._2().getName();

               return String.format(
                  "%s__%s",
                  Strings.convert(jobName).toSnakeCase(),
                  Strings.convert(metricName).toSnakeCase());
            },
            t -> t._2
         ));
   }

   private Map<String, Metric<TimeSeries>> getTimeSeriesMetrics() {
      return resources
         .getJobs()
         .stream()
         .flatMap(job -> job
            .getDefinition()
            .getMonitors()
            .getMetricsMonitors()
            .getTimeSeriesMetrics()
            .stream()
            .map(m -> Tuple2.apply(job.getDefinition().getName().getValue(), m)))
         .collect(Collectors.toMap(
            t -> {
               var jobName = t._1();
               var metricName = t._2().getName();

               return String.format(
                  "%s__%s",
                  Strings.convert(jobName).toSnakeCase(),
                  Strings.convert(metricName).toSnakeCase());
            },
            t -> t._2
         ));
   }

}

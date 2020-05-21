package alpakkeer.api;

import alpakkeer.core.util.DateTime;
import alpakkeer.core.util.DateTimes;
import alpakkeer.core.values.grafana.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

@AllArgsConstructor
public final class GrafanaResource {

   private final ObjectMapper om;

   public Handler getAnnotations() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Annotations");
            op.description("Returns annotation.");
            op.addTagsItem("Grafana");
         })
         .body(AnnotationRequest.class)
         .jsonArray("200", Annotation.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var request = ctx.bodyAsClass(AnnotationRequest.class);

         DateTime to = DateTimes.ofZonedDateTime(request.getRange().getTo());

         ctx.json(List.of(
            Annotation.apply("Test", "Test test test", to.minus(Duration.ofMinutes(2)).toEpochMillis()),
            Annotation.apply("Lorem", "ipsum dolor", to.minus(Duration.ofMinutes(1)).toEpochMillis()),
            Annotation.apply(
               "Foo", "resr",
               to.minus(Duration.ofMinutes(5)).toEpochMillis(),
               to.minus(Duration.ofMinutes(4)).toEpochMillis())));
      });

      /*
      1590043204108
      1590035944000
       */
   }

   public Handler getAnnotationsHeader() {
      return ctx -> {
         ctx.header("Access-Control-Allow-Origin", "*");
         ctx.header("Access-Control-Allow-Methods", "POST");
         ctx.header("Access-Control-Allow-Headers", "accept, content-type");
         ctx.status(200);
      };
   }

   public Handler getHealth() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Test connection");
            op.description("Returns HTTP 200 for Grafana's `Test connection` call.");
            op.addTagsItem("Grafana");
         })
         .result("200");

      return OpenApiBuilder.documented(docs, ctx -> {
         ctx.status(200);
      });
   }

   public Handler getTagKeys() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Tag Keys");
            op.description("Returns tag keys for ad hoc filters.");
            op.addTagsItem("Grafana");
         })
         .jsonArray("200", TagKey.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         System.out.println("Request keys ...");
         ctx.json(List.of(TagKey.apply("string", "test")));
      });
   }

   public Handler getTagValues() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Tag Values");
            op.description("Returns tag values for ad hoc filters.");
            op.addTagsItem("Grafana");
         })
         .body(TagKeyValueRequest.class)
         .jsonArray("200", TagValue.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var request = ctx.bodyAsClass(TagKeyValueRequest.class);
         System.out.println("Request values for " + request.getKey());
         ctx.json(List.of(TagValue.apply("lorem"), TagValue.apply("ipsum")));
      });
   }

   public Handler search() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Search");
            op.description("Returns available metrics when invoked.");
            op.addTagsItem("Grafana");
         })
         .body(SearchRequest.class)
         .jsonArray("200", SearchResponseItem.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         ctx.json(List.of(
            SearchResponseItem.apply("foo", 1),
            SearchResponseItem.apply("bar", 2)));
      });
   }

   public Handler query() {
      var docs = OpenApiBuilder
         .document()
         .operation(op -> {
            op.summary("Query");
            op.description("Returns metrics based on input");
            op.addTagsItem("Grafana");
         })
         .body(SearchRequest.class)
         .jsonArray("200", TimeSeries.class);

      return OpenApiBuilder.documented(docs, ctx -> {
         var request = ctx.bodyAsClass(QueryRequest.class);
         var result = List.of(
            TimeSeries.apply(
               "foo", List.of(
                  DataPoint.apply(request.getStartTime(), 2.0),
                  DataPoint.apply(request.getStartTime() - 1000 * 60, 1.5),
                  DataPoint.apply(request.getStartTime() - 1000 * 60 * 2, 2.3))),
            TimeSeries.apply(
               "bar", List.of(
                  DataPoint.apply(request.getStartTime(), 2.0),
                  DataPoint.apply(request.getStartTime() - 1000 * 60 * 3, 2.7))));

         ctx.json(result);
      });
   }

}

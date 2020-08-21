package alpakkeer.core.jobs.context;

import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.ActorAttributes;
import akka.stream.Supervision;
import akka.stream.alpakka.slick.javadsl.*;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.config.PostgresContextStoreConfiguration;
import alpakkeer.core.util.Operators;
import alpakkeer.core.util.Templates;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.PartialFunction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class PostgresContextStore implements ContextStore {

   private static final Logger LOG = LoggerFactory.getLogger(PostgresContextStore.class);

   private static final DateTimeFormatter POSTGRES_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

   private final PostgresContextStoreConfiguration config;

   private final ActorSystem system;

   private final ObjectMapper om;

   private final SlickSession session;

   public static PostgresContextStore apply(PostgresContextStoreConfiguration config, ActorSystem system, ObjectMapper om) {
      return apply(config, system, om, SlickSession.forConfig(config.getDatabase()));
   }

   @Override
   public <C> CompletionStage<Done> saveContext(String name, C context) {
      var value = Operators.suppressExceptions(() -> om.writeValueAsString(context));
      var className = context.getClass().getName();
      var params = Maps.<String, Object>newHashMap();

      params.put("schema", config.getSchema());
      params.put("table", config.getTable());
      params.put("job", name);
      params.put("inserted", POSTGRES_DATETIME_FORMAT.format(LocalDateTime.now()));
      params.put("type", className);
      params.put("value", value);

      var query = Templates.renderTemplateFromResources("db/sql/context-store/upsert-context.sql", params);

      final Function<Throwable, Supervision.Directive> decider =
         exc -> {
            var msg = String.format(
               "An exception occurred while updating context for job `%s`. Executed query is:\n" +
                  "%s",
               name,
               query.replaceAll("(?m)^", "   |   "));

            LOG.error(msg, exc);
            return (Supervision.Directive) Supervision.stop();
         };

      return Source
         .single(query)
         .toMat(Slick.sink(session, q -> q), Keep.right())
         .withAttributes(ActorAttributes.withSupervisionStrategy(decider))
         .run(system);
   }

   @Override
   public <C> CompletionStage<Optional<C>> readLatestContext(String name) {
      var params = Maps.<String, Object>newHashMap();
      params.put("schema", config.getSchema());
      params.put("table", config.getTable());
      params.put("job", name);

      var query = Templates.renderTemplateFromResources("db/sql/context-store/select-context.sql", params);

      return Slick
         .source(session, query, this::<C>fromSlickRow)
         .recoverWith(PartialFunction.fromFunction(e -> {
            var msg = String.format(
               "An exception occurred reading context for job `%s` - Will fallback to initial context.", name);
            LOG.warn(msg, e);
            return Source.empty();
         }))
         .toMat(Sink.headOption(), Keep.right())
         .run(system);
   }

   @SuppressWarnings("unchecked")
   private <C> C fromSlickRow(SlickRow row) {
      var className = row.nextString();
      var value = row.nextString();

      var msg = String.format(
         "Unable to de-serialize the following JSON into instance of `%s`:\n%s",
         className,
         value.replaceAll("(?m)^", "   |   "));

      var clazz = Operators.suppressExceptions(() -> Class.forName(className), msg);

      return (C) Operators.suppressExceptions(() -> om.readValue(value, clazz));
   }

}

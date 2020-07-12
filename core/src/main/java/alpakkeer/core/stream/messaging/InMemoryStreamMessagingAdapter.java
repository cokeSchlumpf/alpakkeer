package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.RecordEnvelope;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.CommittableRecordContexts;
import alpakkeer.core.stream.context.RecordContext;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class InMemoryStreamMessagingAdapter implements StreamMessagingAdapter {

   private final ObjectMapper om;

   private final Map<String, List<String>> topics;

   public static InMemoryStreamMessagingAdapter apply(ObjectMapper om) {
      return apply(om, Maps.newHashMap());
   }

   private synchronized CompletionStage<Done> putDocument$internal(String topic, RecordEnvelope<?, ?> recordEnvelope) {
      return Operators.suppressExceptions(() -> {
         var json = om.writeValueAsString(recordEnvelope.getRecord());

         if (!topics.containsKey(topic)) {
            topics.put(topic, Lists.newArrayList());
         }

         topics.get(topic).add(json);

         if (recordEnvelope.getContext() instanceof CommittableRecordContext) {
            return ((CommittableRecordContext) recordEnvelope.getContext()).commit();
         } else {
            return CompletableFuture.completedFuture(Done.getInstance());
         }
      });
   }

   private synchronized <R extends Record> Optional<RecordEnvelope<R, CommittableRecordContext>> getDocument$internal(String topic, Class<R> recordType) {
      return Operators.suppressExceptions(() -> {
            var context = CommittableRecordContexts.createFromRunnable(() -> {});

            if (topics.containsKey(topic) && !topics.get(topic).isEmpty()) {
               var record = om.readValue(topics.get(topic).remove(0), recordType);
               return Optional.of(RecordEnvelope.apply(record, context));
            } else {
               return Optional.empty();
            }
      });
   }


   @Override
   public <R extends Record, C extends RecordContext> CompletionStage<Done> put(String topic, RecordEnvelope<R, C> record) {
      return putDocument$internal(topic, record);
   }

   @Override
   public <R extends Record, C extends RecordContext> Sink<RecordEnvelope<R, C>, CompletionStage<Done>> toTopic(String topic) {
      return Flow
         .<RecordEnvelope<R, C>>create()
         .toMat(Sink.foreach(record -> putDocument$internal(topic, record)), Keep.right());
   }

   @Override
   public <R extends Record> CompletionStage<Optional<RecordEnvelope<R, CommittableRecordContext>>> getDocument(String topic, Class<R> recordType) {
      return CompletableFuture.completedFuture(getDocument$internal(topic, recordType));
   }

   @Override
   public <R extends Record> Source<RecordEnvelope<R, CommittableRecordContext>, NotUsed> fromTopic(String topic, Class<R> recordType) {
      return Source
         .repeat("tick")
         .map(s -> getDocument$internal(topic, recordType))
         .grouped(500)
         .throttle(1, Duration.ofSeconds(1))
         .mapConcat(l -> l)
         .filter(Optional::isPresent)
         .map(Optional::get);
   }
}

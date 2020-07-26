package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.CommittableRecordContexts;
import alpakkeer.core.stream.context.NoRecordContext;
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

   private synchronized CompletionStage<Done> putDocument$internal(String topic, Record<?, ?> record) {
      return Operators.suppressExceptions(() -> {
         var json = om.writeValueAsString(record);

         if (!topics.containsKey(topic)) {
            topics.put(topic, Lists.newArrayList());
         }

         topics.get(topic).add(json);

         if (record.getContext() instanceof CommittableRecordContext) {
            return ((CommittableRecordContext) record.getContext()).commit();
         } else {
            return CompletableFuture.completedFuture(Done.getInstance());
         }
      });
   }

   @SuppressWarnings("unchecked")
   private synchronized <R> Optional<Record<R, CommittableRecordContext>> getDocument$internal(String topic) {
      return Operators.suppressExceptions(() -> {
            var context = CommittableRecordContexts.createFromRunnable(() -> {});

            if (topics.containsKey(topic) && !topics.get(topic).isEmpty()) {
               var record = (Record<R, NoRecordContext>) om.readValue(topics.get(topic).remove(0), Record.class);
               return Optional.of(record.withContext(context));
            } else {
               return Optional.empty();
            }
      });
   }

   @Override
   public <R, C extends RecordContext> CompletionStage<Done> putRecord(String topic, Record<R, C> record) {
      return putDocument$internal(topic, record);
   }

   @Override
   public <R, C extends RecordContext> Sink<Record<R, C>, CompletionStage<Done>> recordsSink(String topic) {
      return Flow
         .<Record<R,C>>create()
         .toMat(Sink.foreach(record -> putDocument$internal(topic, record)), Keep.right());
   }

   @Override
   public <T> CompletionStage<Optional<Record<T, CommittableRecordContext>>> getNextRecord(String topic, Class<T> recordType, String consumerGroup) {
      // TODO: Implement consumer id
      return CompletableFuture.completedFuture(getDocument$internal(topic));
   }

   @Override
   public <T> Source<Record<T, CommittableRecordContext>, NotUsed> recordsSource(String topic, Class<T> recordType, String consumerGroup) {
      // TODO: Implement consumer id
      return Source
         .repeat("tick")
         .map(s -> this.<T>getDocument$internal(topic))
         .grouped(500)
         .throttle(1, Duration.ofSeconds(1))
         .mapConcat(l -> l)
         .filter(Optional::isPresent)
         .map(Optional::get);
   }

}

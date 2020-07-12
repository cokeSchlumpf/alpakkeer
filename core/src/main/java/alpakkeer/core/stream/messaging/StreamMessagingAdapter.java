package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.RecordEnvelope;
import alpakkeer.core.stream.Records;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.RecordContext;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface StreamMessagingAdapter {

   <R extends Record, C extends RecordContext> CompletionStage<Done> put(String topic, RecordEnvelope<R, C> record);

   <R extends Record, C extends RecordContext> Sink<RecordEnvelope<R, C>, CompletionStage<Done>> toTopic(String topic);

   <R extends Record> CompletionStage<Optional<RecordEnvelope<R, CommittableRecordContext>>> get(String topic, Class<R> recordType);

   @SuppressWarnings("unchecked")
   default <T> CompletionStage<Optional<RecordEnvelope<Records.SimpleRecord<T>, CommittableRecordContext>>> getAsSimpleRecord(String topic, Class<T> recordType) {
      return get(topic, Records.SimpleRecord.class)
         .thenApply(optRecord -> optRecord.map(r -> r.withRecord((Records.SimpleRecord<T>) r.getRecord())));
   }

   <R extends Record> Source<RecordEnvelope<R, CommittableRecordContext>, NotUsed> fromTopic(String topic, Class<R> recordType);

   @SuppressWarnings("unchecked")
   default <T> Source<RecordEnvelope<Records.SimpleRecord<T>, CommittableRecordContext>, NotUsed> fromTopicAsSimpleRecord(String topic, Class<T> recordType) {
      return fromTopic(topic, Records.SimpleRecord.class)
         .map(r -> r.withRecord((Records.SimpleRecord<T>) r.getRecord()));
   }

}

package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.RecordEnvelope;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.RecordContext;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface StreamMessagingAdapter {

   <R extends Record, C extends RecordContext> CompletionStage<Done> put(String topic, RecordEnvelope<R, C> record);

   <R extends Record, C extends RecordContext> Sink<RecordEnvelope<R, C>, CompletionStage<Done>> toTopic(String topic);

   <R extends Record> CompletionStage<Optional<RecordEnvelope<R, CommittableRecordContext>>> getDocument(String topic, Class<R> recordType);

   <R extends Record> Source<RecordEnvelope<R, CommittableRecordContext>, NotUsed> fromTopic(String topic, Class<R> recordType);

}

package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.RecordContext;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class PlainKafkaStreamMessagingAdapter implements StreamMessagingAdapter {

   @Override
   public <R, C extends RecordContext> CompletionStage<Done> putRecord(String topic, Record<R, C> record) {
      return null;
   }

   @Override
   public <R, C extends RecordContext> Sink<Record<R, C>, CompletionStage<Done>> recordsSink(String topic) {
      return null;
   }

   @Override
   public <T> CompletionStage<Optional<Record<T, CommittableRecordContext>>> getNextRecord(String topic, Class<T> recordType) {
      return null;
   }

   @Override
   public <T> Source<Record<T, CommittableRecordContext>, NotUsed> recordsSource(String topic, Class<T> recordType) {
      return null;
   }

}

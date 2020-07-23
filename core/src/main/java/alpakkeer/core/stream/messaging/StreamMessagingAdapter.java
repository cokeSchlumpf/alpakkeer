package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.japi.Function;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.RecordContext;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public interface StreamMessagingAdapter {

   /**
    * Puts a single record into a messaging topic.
    *
    * @param topic The name of the topic
    * @param record The record to put into the topic
    * @param <R> The type of the record
    * @param <C> The record context
    * @return CompletionStage which completes when record is put
    */
   <R, C extends RecordContext> CompletionStage<Done> putRecord(String topic, Record<R, C> record);

   /**
    * Put a single item into a messaging topic.
    *
    * @param topic The name of the topic to put the item
    * @param item The item to put into the topic
    * @param <T> The type of the item
    * @return CompletionStage which completes when item is put
    */
   default <T> CompletionStage<Done> putItem(String topic, T item) {
      return putRecord(topic, Record.apply(item, UUID.randomUUID().toString()));
   }

   /**
    * A sink which publishes records to a messaging topic.
    *
    * @param topic The name of the topic
    * @param <R> The type of the record
    * @param <C> The context type of the record
    * @return An Akka Streams sink
    */
   <R, C extends RecordContext> Sink<Record<R, C>, CompletionStage<Done>> recordsSink(String topic);


   /**
    * A sink which publishes items to a messaging topic. The key of the message is generated with a random
    * key-generator.
    *
    * @param topic The name of the topic
    * @param <T> The type of the item
    * @return An Akka Streams sink
    */
   default <T> Sink<T, CompletionStage<Done>> itemsSink(String topic) {
      return Flow
         .<T>create()
         .map(Record::apply)
         .toMat(recordsSink(topic), Keep.right());
   }

   /**
    * A sink which publishes items to a messaging topic.
    *
    * @param topic The name of the topic
    * @param key A function to generate the message key from the item
    * @param <T> The type of the item
    * @return An Akka Streams sink
    */
   default <T> Sink<T, CompletionStage<Done>> itemsSink(String topic, Function<T, String> key) {
      return Flow
         .<T>create()
         .map(r -> Record.apply(r, key.apply(r)))
         .toMat(recordsSink(topic), Keep.right());
   }

   /**
    * Read the next record from a topic.
    *
    * @param topic The name of the topic
    * @param recordType The type of the record; will be used for de-serialization
    * @param <T> The type of the record
    * @return An optional record wrapped in its envelope
    */
   <T> CompletionStage<Optional<Record<T, CommittableRecordContext>>> getNextRecord(String topic, Class<T> recordType);

   /**
    * Creates an Akka Streams source to stream messages from a topic.
    *
    * @param topic The name of the topic
    * @param recordType The type of the record; will be used for de-serialization
    * @param <T> The type of the record
    * @return An Akka Streams source of records
    */
   <T> Source<Record<T, CommittableRecordContext>, NotUsed> recordsSource(String topic, Class<T> recordType);

}

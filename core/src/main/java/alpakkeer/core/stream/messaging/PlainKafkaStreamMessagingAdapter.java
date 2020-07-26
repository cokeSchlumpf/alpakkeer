package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.config.KafkaMessagingAdapterConfiguration;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.CommittableRecordContexts;
import alpakkeer.core.stream.context.NoRecordContext;
import alpakkeer.core.stream.context.RecordContext;
import alpakkeer.core.util.StringConverters;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class PlainKafkaStreamMessagingAdapter implements StreamMessagingAdapter {

   private static final Logger LOG = LoggerFactory.getLogger(PlainKafkaStreamMessagingAdapter.class);

   private final ActorSystem system;

   private final ObjectMapper om;

   private final KafkaMessagingAdapterConfiguration configuration;

   @Override
   public <R, C extends RecordContext> CompletionStage<Done> putRecord(String topic, Record<R, C> record) {
      return Source
         .single(record)
         .toMat(recordsSink(topic), Keep.right())
         .run(system);
   }

   @Override
   public <R, C extends RecordContext> Sink<Record<R, C>, CompletionStage<Done>> recordsSink(String topic) {
      var topicSC = StringConverters.Converter(topic).toSnakeCase();
      var settings = ProducerSettings
         .create(configuration.getProducer(), new StringSerializer(), new StringSerializer())
         .withBootstrapServers(configuration.getBootstrapServer());

      return Flow
         .<Record<R, C>>create()
         .map(record -> {
            // TODO: Do after actual insertion
            if (record.getContext() instanceof CommittableRecordContext) {
               ((CommittableRecordContext) record.getContext()).commit();
            }

            return ProducerMessage.single(new ProducerRecord<>(topicSC, record.getKey(), om.writeValueAsString(record)));
         })
         .via(Producer.flexiFlow(settings))
         .toMat(Sink.ignore(), Keep.right());
   }

   @Override
   public <T> CompletionStage<Optional<Record<T, CommittableRecordContext>>> getNextRecord(String topic, Class<T> recordType, String consumerGroup) {
      LOG.warn("putNextRecord is not implemented for PlainKafkaStreamMessagingAdapter");
      return CompletableFuture.completedFuture(Optional.empty());
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> Source<Record<T, CommittableRecordContext>, NotUsed> recordsSource(String topic, Class<T> recordType, String consumerGroup) {
      var topicsSC = StringConverters.Converter(topic).toSnakeCase();
      var settings = ConsumerSettings.create(configuration.getConsumer(), new StringDeserializer(), new StringDeserializer())
            .withBootstrapServers(configuration.getBootstrapServer())
            .withGroupId(consumerGroup)
            .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

      return Consumer
         .plainSource(settings, Subscriptions.topics(topicsSC))
         .mapMaterializedValue(c -> NotUsed.getInstance())
         .map(record -> (Record<T, NoRecordContext>) om.readValue(record.value(), Record.class))
         .map(record -> record.withContext(CommittableRecordContexts.createFromRunnable(() -> {})));
   }
}

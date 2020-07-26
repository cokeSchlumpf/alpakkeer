package alpakkeer.core.stream.messaging;

import akka.actor.ActorSystem;
import alpakkeer.config.MessagingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StreamMessagingAdapters {

   private StreamMessagingAdapters() {

   }

   public static StreamMessagingAdapter createFromConfiguration(ActorSystem system, ObjectMapper om, MessagingConfiguration configuration) {
      var type = configuration.getType().toLowerCase();

      if (type.equals("fs") || type.equals("filesystem")) {
         return FileSystemStreamMessagingAdapter.apply(om, configuration.getFs());
      } else if (type.equals("in-memory")) {
         return InMemoryStreamMessagingAdapter.apply(om);
      }  else if (type.equals("kafka")) {
         return PlainKafkaStreamMessagingAdapter.apply(system, om, configuration.getKafka());
      }  else {
         throw new RuntimeException(String.format(
            "'%s' is an invalid messaging type; valid types are: 'in-memory', 'fs', 'kafka'", type));
      }
   }

}

package alpakkeer.core.stream.messaging;

import akka.actor.ActorSystem;
import alpakkeer.config.MessagingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StreamMessagingAdapters {

   private StreamMessagingAdapters() {

   }

   public static StreamMessagingAdapter createFromConfiguration(ActorSystem system, ObjectMapper om, MessagingConfiguration configuration) {
      var type = configuration.getType().toLowerCase();

      switch (type) {
         case "fs":
         case "filesystem":
            return FileSystemStreamMessagingAdapter.apply(om, configuration.getFs());
         case "in-memory":
            return InMemoryStreamMessagingAdapter.apply(om);
         case "kafka":
            return PlainKafkaStreamMessagingAdapter.apply(system, om, configuration.getKafka());
         default:
            throw new RuntimeException(String.format(
               "'%s' is an invalid messaging type; valid types are: 'in-memory', 'fs', 'kafka'", type));
      }
   }

}

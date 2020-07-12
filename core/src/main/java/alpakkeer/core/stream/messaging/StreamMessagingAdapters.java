package alpakkeer.core.stream.messaging;

import alpakkeer.config.MessagingConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class StreamMessagingAdapters {

   private StreamMessagingAdapters() {

   }

   public static StreamMessagingAdapter createFromConfiguration(ObjectMapper om, MessagingConfiguration configuration) {
      var type = configuration.getType().toLowerCase();

      if (type.equals("fs") || type.equals("filesystem")) {
         return FileSystemStreamMessagingAdapter.apply(om, configuration.getFs());
      } else if (type.equals("in-memory")) {
         return InMemoryStreamMessagingAdapter.apply(om);
      }  else {
         throw new RuntimeException(String.format(
            "'%s' is an invalid messaging type; valid types are: 'in-memory', 'fs', 'kafka'", type));
      }
   }

}

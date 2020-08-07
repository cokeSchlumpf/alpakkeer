package alpakkeer.core.stream.messaging;

import alpakkeer.javadsl.AlpakkeerBaseRuntime;

public final class StreamMessagingAdapters {

   private StreamMessagingAdapters() {

   }

   public static StreamMessagingAdapter createFromConfiguration(AlpakkeerBaseRuntime runtime) {
      var configuration = runtime.getConfiguration().getMessaging();
      var om = runtime.getObjectMapper();
      var system = runtime.getSystem();
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

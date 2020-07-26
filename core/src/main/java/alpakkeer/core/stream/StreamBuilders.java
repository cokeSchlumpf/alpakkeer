package alpakkeer.core.stream;

import alpakkeer.javadsl.AlpakkeerRuntime;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;

public final class StreamBuilders {

   private StreamBuilders() {

   }

   @Getter
   @AllArgsConstructor(staticName = "apply")
   private static class CommonStreamBuilder implements StreamBuilder {

      private final StreamMonitoringAdapter monitoring;

      private final StreamMessagingAdapter messaging;

      private final AlpakkeerRuntime runtime;

      private final Logger logger;

      public StreamMonitoringAdapter monitoring() {
         return monitoring;
      }

      public StreamMessagingAdapter messaging() {
         return messaging;
      }

      public AlpakkeerRuntime runtime() {
         return runtime;
      }

      @Override
      public Logger logger() {
         return logger;
      }

   }

   public static StreamBuilder common(
      StreamMonitoringAdapter monitoring,
      AlpakkeerRuntime runtime,
      Logger logger) {

      return CommonStreamBuilder.apply(monitoring, runtime.getMessaging(), runtime, logger);
   }

}

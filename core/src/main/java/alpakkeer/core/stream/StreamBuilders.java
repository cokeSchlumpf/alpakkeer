package alpakkeer.core.stream;

import alpakkeer.AlpakkeerRuntime;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class StreamBuilders {

   private StreamBuilders() {

   }

   @Getter
   @AllArgsConstructor(staticName = "apply")
   private static class CommonStreamBuilder implements StreamBuilder {

      private final StreamMonitoringAdapter monitoring;

      private final StreamMessagingAdapter messaging;

      private final AlpakkeerRuntime runtime;

      public StreamMonitoringAdapter monitoring() {
         return monitoring;
      }

      public StreamMessagingAdapter messaging() {
         return messaging;
      }

      public AlpakkeerRuntime runtime() {
         return runtime;
      }

   }

   public static StreamBuilder common(
      StreamMonitoringAdapter monitoring,
      AlpakkeerRuntime runtime) {

      return CommonStreamBuilder.apply(monitoring, runtime.getMessaging(), runtime);
   }

}

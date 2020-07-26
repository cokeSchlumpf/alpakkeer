package alpakkeer.core.processes;

import alpakkeer.javadsl.AlpakkeerRuntime;
import alpakkeer.core.processes.monitor.ProcessMonitor;
import alpakkeer.core.stream.StreamBuilder;
import alpakkeer.core.stream.StreamBuilders;
import alpakkeer.core.stream.StreamMonitoringAdapter;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;

@AllArgsConstructor(staticName = "apply")
public class ProcessStreamBuilder implements StreamBuilder {

   private final StreamBuilder sb;

   private final String executionId;

   public static ProcessStreamBuilder apply(
      AlpakkeerRuntime runtime,
      ProcessMonitor monitor,
      String executionId,
      Logger logger) {

      var monitoring = StreamMonitoringAdapter.apply(monitor, executionId);
      var sb = StreamBuilders.common(monitoring, runtime, logger);
      return apply(sb, executionId);
   }


   /**
    * Returns the execution id of the current execution.
    *
    * @return The current execution id
    */
   public String getExecutionId() {
      return executionId;
   }

   @Override
   public Logger getLogger() {
      return sb.getLogger();
   }

   @Override
   public StreamMonitoringAdapter getMonitoring() {
      return sb.getMonitoring();
   }

   @Override
   public StreamMessagingAdapter getMessaging() {
      return sb.getMessaging();
   }

   /**
    * Access the initialized Alpakkeer runtime.
    *
    * @return The runtime
    */
   public AlpakkeerRuntime getRuntime() {
      return sb.getRuntime();
   }

   @Override
   public StreamMonitoringAdapter monitoring() {
      return sb.monitoring();
   }

   @Override
   public StreamMessagingAdapter messaging() {
      return sb.messaging();
   }

   @Override
   public AlpakkeerRuntime runtime() {
      return sb.runtime();
   }

   @Override
   public Logger logger() {
      return sb.logger();
   }
}

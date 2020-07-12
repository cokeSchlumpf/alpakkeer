package alpakkeer.core.jobs;

import alpakkeer.AlpakkeerRuntime;
import alpakkeer.core.jobs.monitor.JobMonitor;
import alpakkeer.core.stream.StreamBuilder;
import alpakkeer.core.stream.StreamBuilders;
import alpakkeer.core.stream.StreamMonitoringAdapter;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "apply")
public final class JobStreamBuilder<P, C> implements StreamBuilder {

   private final StreamBuilder streamBuilder;

   private final String executionId;

   private final P properties;

   private final C context;

   public static <P, C> JobStreamBuilder<P, C> apply(
      AlpakkeerRuntime runtime,
      JobMonitor<P, C> monitor,
      String executionId,
      P properties,
      C context) {

      var monitoring = StreamMonitoringAdapter.apply(monitor, executionId);
      var sb = StreamBuilders.common(monitoring, runtime);
      return apply(sb, executionId, properties, context);
   }

   /**
    * Returns the input context for the current execution.
    *
    * @return The current context
    */
   public C getContext() {
      return context;
   }

   /**
    * Returns the execution id of the current execution.
    *
    * @return The current execution id
    */
   public String getExecutionId() {
      return executionId;
   }

   /**
    * Returns the properties of the execution.
    *
    * @return The execution's property
    */
   public P getProperties() {
      return properties;
   }

   @Override
   public StreamMonitoringAdapter getMonitoring() {
      return streamBuilder.getMonitoring();
   }

   @Override
   public StreamMessagingAdapter getMessaging() {
      return streamBuilder.getMessaging();
   }

   @Override
   public AlpakkeerRuntime getRuntime() {
      return streamBuilder.getRuntime();
   }

   @Override
   public StreamMonitoringAdapter monitoring() {
      return streamBuilder.monitoring();
   }

   @Override
   public StreamMessagingAdapter messaging() {
      return streamBuilder.messaging();
   }

   @Override
   public AlpakkeerRuntime runtime() {
      return streamBuilder.runtime();
   }
}

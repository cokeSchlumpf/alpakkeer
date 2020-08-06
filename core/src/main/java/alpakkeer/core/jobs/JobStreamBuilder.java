package alpakkeer.core.jobs;

import akka.Done;
import alpakkeer.javadsl.AlpakkeerRuntime;
import alpakkeer.core.jobs.monitor.JobMonitor;
import alpakkeer.core.stream.StreamBuilder;
import alpakkeer.core.stream.StreamBuilders;
import alpakkeer.core.stream.StreamMonitoringAdapter;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class JobStreamBuilder<P, C> implements StreamBuilder {

   private final StreamBuilder streamBuilder;

   private final String name;

   private final String executionId;

   private final P properties;

   private final C context;

   public static <P, C> JobStreamBuilder<P, C> apply(
      AlpakkeerRuntime runtime,
      JobMonitor<P, C> monitor,
      String name,
      String executionId,
      Logger logger,
      P properties,
      C context) {

      var monitoring = StreamMonitoringAdapter.apply(monitor, executionId);
      var sb = StreamBuilders.common(monitoring, runtime, logger);
      return apply(sb, name, executionId, properties, context);
   }

   /**
    * Returns the input context for the state of the context at the beginning of execution.
    * If the context us updated during execution, the return value of this method will not change.
    *
    * @return The current context
    */
   public C context() {
      return context;
   }

   /**
    * Returns the input context for the state of the context at the beginning of execution.
    * If the context us updated during execution, the return value of this method will not change.
    *
    * @return The current context
    */
   public C getContext() {
      return context;
   }

   public CompletionStage<Done> setContext(C context) {
      return streamBuilder
         .getRuntime()
         .getContextStore()
         .saveContext(name, context);
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
   public Logger getLogger() {
      return streamBuilder.getLogger();
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

   @Override
   public Logger logger() {
      return streamBuilder.getLogger();
   }
}

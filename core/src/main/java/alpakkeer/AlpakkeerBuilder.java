package alpakkeer;

import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.japi.function.Procedure;
import akka.japi.function.Procedure2;
import akka.japi.function.Procedure3;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.processes.ProcessDefinition;
import alpakkeer.core.processes.ProcessDefinitions;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.util.Operators;
import com.google.common.collect.Lists;
import io.javalin.Javalin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Defines the Alpakkeer builder DSL.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerBuilder {

   private List<Procedure3<Javalin, AlpakkeerRuntime, Resources>> apiExtensions;

   private List<Function<JobDefinitions, JobDefinition<?, ?>>> jobs;

   private List<Function<ProcessDefinitions, ProcessDefinition>> processes;

   private List<Function2<AlpakkeerRuntime, Resources, MetricStore<TimeSeries>>> tsMetrics;

   private AlpakkeerRuntimeBuilder runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), AlpakkeerRuntimeBuilder.apply());
   }

   /**
    * Use this method to override the defaults of the Alpakkeer runtime configuration.
    *
    * @param configure A function which accepts the configuration builder
    * @return The current builder instance
    */
   public AlpakkeerBuilder configure(Procedure<AlpakkeerRuntimeBuilder> configure) {
      Operators.suppressExceptions(() -> configure.apply(runtimeConfig));
      return this;
   }

   /**
    * Use this method to extend the API with custom endpoints.
    *
    * @param apiExtension A function which takes the {@link Javalin} instance
    * @return The current builder instance
    */
   public AlpakkeerBuilder withApiEndpoint(Procedure<Javalin> apiExtension) {
      apiExtensions.add((j, c, r) -> apiExtension.apply(j));
      return this;
   }

   /**
    * Use this method to extend the API with custom endpoints.
    *
    * @param apiExtension A function which takes the {@link Javalin} instance and the {@link AlpakkeerRuntime}
    *                     of Alpakkeer instance
    * @return The current builder instance
    */
   public AlpakkeerBuilder withApiEndpoint(Procedure2<Javalin, AlpakkeerRuntime> apiExtension) {
      apiExtensions.add((j, c, r) -> apiExtension.apply(j, c));
      return this;
   }

   /**
    * Use this method to extend the API with custom endpoints.
    *
    * @param apiExtension A function which takes the {@link Javalin} instance, the {@link AlpakkeerRuntime} and
    *                     the {@link Resources} of the Alpakkeer instance
    * @return The current builder instance
    */
   public AlpakkeerBuilder withApiEndpoint(Procedure3<Javalin, AlpakkeerRuntime, Resources> apiExtension) {
      apiExtensions.add(apiExtension);
      return this;
   }

   /**
    * Registers a new job to the application.
    *
    * @param builder A factory method which creates a {@link JobDefinition} from {@link JobDefinitions}-builder
    * @param <P>     The job's property type
    * @param <C>     The job's context type
    * @return The current builder instance
    */
   public <P, C> AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinitions.JobSettingsConfiguration<P, C>> builder) {
      jobs.add(j -> builder.apply(j).build());
      return this;
   }

   /**
    * Adds a custom time series metric to the application.
    *
    * @param metric The custom metric.
    * @return The current builder instance
    */
   public AlpakkeerBuilder withTimeSeriesMetric(MetricStore<TimeSeries> metric) {
      tsMetrics.add((c, r) -> metric);
      return this;
   }

   /**
    * Adds a custom time series metric to the application.
    *
    * @param metric A function which takes the {@link AlpakkeerRuntime} and returns the custom metric.
    * @return The current builder instance
    */
   public AlpakkeerBuilder withTimeSeriesMetric(Function<AlpakkeerRuntime, MetricStore<TimeSeries>> metric) {
      tsMetrics.add((c, r) -> metric.apply(c));
      return this;
   }

   /**
    * Adds a custom time series metric to the application.
    *
    * @param metric A function which takes the {@link AlpakkeerRuntime} and returns the custom metric.
    * @return The current builder instance
    */
   public AlpakkeerBuilder withTimeSeriesMetric(Function2<AlpakkeerRuntime, Resources, MetricStore<TimeSeries>> metric) {
      tsMetrics.add(metric);
      return this;
   }

   /**
    * Adds a new process to the application.
    *
    * @param builder A factory method which creates the ProcessDefinition
    * @return The current builder instance
    */
   public AlpakkeerBuilder withProcess(Function<ProcessDefinitions, ProcessDefinitions.ProcessDefinitionBuilder> builder) {
      processes.add(p -> builder.apply(p).build());
      return this;
   }

   /**
    * Builds and starts the Alpakkeer application.
    *
    * @return The running Alpakkeer instance.
    */
   public Alpakkeer start() {
      var alpakkeerConfiguration = AlpakkeerConfiguration.apply();
      var runtime = runtimeConfig.build(alpakkeerConfiguration);
      var resources = Resources.apply(runtime);

      // initialize components
      runtime.getMetricsCollectors().forEach(c -> c.run(runtime));

      jobs.stream().map(f ->
         Operators.suppressExceptions(() -> f.apply(JobDefinitions.apply(runtime)))).forEach(resources::addJob);

      processes.stream().map(f ->
         Operators.suppressExceptions(() -> f.apply(ProcessDefinitions.apply(runtime)))).forEach(resources::addProcess);

      tsMetrics.forEach(f ->
         Operators.suppressExceptions(() -> resources.addTimeSeriesMetric(f.apply(runtime, resources))));

      apiExtensions.forEach(ext ->
         Operators.suppressExceptions(() -> ext.apply(runtime.getApp(), runtime, resources)));

      return Alpakkeer.apply(alpakkeerConfiguration, runtime, resources);
   }

}

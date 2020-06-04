package alpakkeer;

import akka.japi.function.Procedure;
import akka.japi.function.Procedure2;
import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.config.RuntimeConfigurationBuilder;
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
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerBuilder {

   private List<Procedure2<Javalin, RuntimeConfiguration>> apiExtensions;

   private List<Function<JobDefinitions, JobDefinition<?, ?>>> jobs;

   private List<Function<ProcessDefinitions, ProcessDefinition>> processes;

   private List<Function<RuntimeConfiguration, MetricStore<TimeSeries>>> tsMetrics;

   private RuntimeConfigurationBuilder runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), RuntimeConfigurationBuilder.apply());
   }

   public AlpakkeerBuilder configure(Consumer<RuntimeConfigurationBuilder> configure) {
      configure.accept(runtimeConfig);
      return this;
   }

   public AlpakkeerBuilder withApiEndpoint(Procedure<Javalin> apiExtension) {
      apiExtensions.add((j, r) -> apiExtension.apply(j));
      return this;
   }

   public AlpakkeerBuilder withApiEndpoint(Procedure2<Javalin, RuntimeConfiguration> apiExtension) {
      apiExtensions.add(apiExtension);
      return this;
   }

   public <P, C> AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinition<P, C>> builder) {
      jobs.add(builder::apply);
      return this;
   }

   public AlpakkeerBuilder withTimeSeriesMetric(MetricStore<TimeSeries> metric) {
      tsMetrics.add(c -> metric);
      return this;
   }

   public AlpakkeerBuilder withTimeSeriesMetric(Function<RuntimeConfiguration, MetricStore<TimeSeries>> metric) {
      tsMetrics.add(metric);
      return this;
   }

   public AlpakkeerBuilder withProcess(Function<ProcessDefinitions, ProcessDefinition> builder) {
      processes.add(builder);
      return this;
   }

   public Alpakkeer start() {
      var alpakkeerConfiguration = AlpakkeerConfiguration.apply();
      var runtime = runtimeConfig.build(alpakkeerConfiguration);
      var resources = Resources.apply(runtime);

      // initialize components
      runtime.getMetricsCollectors().forEach(c -> c.run(runtime));
      jobs.stream().map(f -> f.apply(JobDefinitions.apply(runtime))).forEach(resources::addJob);
      processes.stream().map(f -> f.apply(ProcessDefinitions.apply(runtime))).forEach(resources::addProcess);
      tsMetrics.forEach(f -> resources.addTimeSeriesMetric(f.apply(runtime)));
      apiExtensions.forEach(ext -> Operators.suppressExceptions(() -> ext.apply(runtime.getApp(), runtime)));

      return Alpakkeer.apply(alpakkeerConfiguration, runtime, resources);
   }

}

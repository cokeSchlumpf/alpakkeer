package alpakkeer;

import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.config.RuntimeConfigurationBuilder;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.monitoring.MetricStore;
import alpakkeer.core.monitoring.values.TimeSeries;
import alpakkeer.core.resources.Resources;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerBuilder {

   private List<Function<JobDefinitions, JobDefinition<?, ?>>> jobs;

   private List<Function<RuntimeConfiguration, MetricStore<TimeSeries>>> tsMetrics;

   private RuntimeConfigurationBuilder runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(Lists.newArrayList(), Lists.newArrayList(), RuntimeConfigurationBuilder.apply());
   }

   public AlpakkeerBuilder configure(Consumer<RuntimeConfigurationBuilder> configure) {
      configure.accept(runtimeConfig);
      return this;
   }

   public AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinition<?, ?>> builder) {
      jobs.add(builder);
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

   public Alpakkeer start() {
      var config = runtimeConfig.build();
      var resources = Resources.apply(config.getSystem(), config.getScheduler(), config.getContextStore());

      // initialize components
      config.getMetricsCollectors().forEach(c -> c.run(config));
      jobs.stream().map(f -> f.apply(JobDefinitions.apply(config))).forEach(resources::addJob);
      tsMetrics.forEach(f -> resources.addTimeSeriesMetric(f.apply(config)));

      return Alpakkeer.apply(AlpakkeerConfiguration.apply(), config, resources);
   }

}

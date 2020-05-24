package alpakkeer;

import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.config.RuntimeConfigurationBuilder;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
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

   private RuntimeConfigurationBuilder runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(Lists.newArrayList(), RuntimeConfigurationBuilder.apply());
   }

   public AlpakkeerBuilder configure(Consumer<RuntimeConfigurationBuilder> configure) {
      configure.accept(runtimeConfig);
      return this;
   }

   public AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinition<?, ?>> builder) {
      jobs.add(builder);
      return this;
   }

   public Alpakkeer start() {
      var config = runtimeConfig.build();
      var resources = Resources.apply(config.getSystem(), config.getScheduler(), config.getContextStore());

      // initialize components
      config.getMetricsCollectors().forEach(c -> c.run(config));
      jobs.stream().map(f -> f.apply(JobDefinitions.apply(config))).forEach(resources::addJob);

      return Alpakkeer.apply(AlpakkeerConfiguration.apply(), config, resources);
   }

}

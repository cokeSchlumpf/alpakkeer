package alpakkeer;

import akka.actor.ActorSystem;
import alpakkeer.config.RuntimeConfigurationBuilder;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.scheduler.CronSchedulers;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerBuilder {

   private final CompletableFuture<ActorSystem> system;

   private List<Function<JobDefinitions, JobDefinition<?, ?>>> jobs;

   private RuntimeConfigurationBuilder runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(new CompletableFuture<>(), Lists.newArrayList(), RuntimeConfigurationBuilder.apply());
   }

   public AlpakkeerBuilder configure(Consumer<RuntimeConfigurationBuilder> configure) {
      configure.accept(runtimeConfig);
      return this;
   }

   public AlpakkeerBuilder withActorSystem(ActorSystem system) {
      this.system.complete(system);
      return this;
   }

   public AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinition<?, ?>> builder) {
      jobs.add(builder);
      return this;
   }

   public Alpakkeer start() {
      var config = runtimeConfig.build();
      var scheduler = CronSchedulers.apply();
      var resources = Resources.apply(config.getSystem(), scheduler, config.getContextStore());

      this.jobs.stream().map(f -> f.apply(JobDefinitions.apply(config))).forEach(resources::addJob);

      return Alpakkeer.apply(config, scheduler, resources);
   }

}

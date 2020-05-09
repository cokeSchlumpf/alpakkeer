package alpakkeer;

import akka.actor.ActorSystem;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.scheduler.CronSchedulers;
import alpakkeer.core.util.Operators;
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

   private List<JobDefinition<?, ?>> jobs;

   private RuntimeConfiguration runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(new CompletableFuture<>(), Lists.newArrayList(), RuntimeConfiguration.apply());
   }

   public AlpakkeerBuilder configure(Consumer<RuntimeConfiguration> configure) {
      configure.accept(runtimeConfig);
      return this;
   }

   public AlpakkeerBuilder withActorSystem(ActorSystem system) {
      this.system.complete(system);
      return this;
   }

   public AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinition<?, ?>> builder) {
      jobs.add(builder.apply(JobDefinitions.apply(this.system, runtimeConfig)));
      return this;
   }

   public Alpakkeer start() {
      if (!system.isDone()) {
         system.complete(ActorSystem.create("alpakkeer"));
      }

      var sys = Operators.suppressExceptions((Operators.ExceptionalSupplier<ActorSystem>) system::get);
      var scheduler = CronSchedulers.apply();
      var resources = Resources.apply(sys, scheduler, runtimeConfig.getContextStore());
      this.jobs.forEach(resources::addJob);

      return Alpakkeer.apply(sys, scheduler, resources, runtimeConfig.getObjectMapper());
   }

}

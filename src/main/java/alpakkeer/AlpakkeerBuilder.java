package alpakkeer;

import akka.actor.ActorSystem;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.scheduler.CronSchedulers;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerBuilder {

   private ActorSystem system;

   private List<JobDefinition<?>> jobs;

   private RuntimeConfiguration runtimeConfig;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(null, Lists.newArrayList(), RuntimeConfiguration.apply());
   }

   public AlpakkeerBuilder configure(Consumer<RuntimeConfiguration> configure) {
      configure.accept(runtimeConfig);
      return this;
   }

   public AlpakkeerBuilder withActorSystem(ActorSystem system) {
      this.system = system;
      return this;
   }

   public AlpakkeerBuilder withJob(Function<JobDefinitions, JobDefinition<?>> builder) {
      jobs.add(builder.apply(JobDefinitions.apply(runtimeConfig)));
      return this;
   }

   public Alpakkeer start() {
      if (system == null) {
         system = ActorSystem.create("alpakkeer");
      }

      var scheduler = CronSchedulers.apply();
      var resources = Resources.apply(system, scheduler);
      this.jobs.forEach(resources::addJob);

      return Alpakkeer.apply(system, scheduler, resources, runtimeConfig.getObjectMapper());
   }

}

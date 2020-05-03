package alpakkeer;

import akka.actor.ActorSystem;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.scheduler.CronSchedulers;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerBuilder {

   private ActorSystem system;

   private List<JobDefinition<?>> jobs;

   static AlpakkeerBuilder apply() {
      return new AlpakkeerBuilder(null, Lists.newArrayList());
   }

   public AlpakkeerBuilder withActorSystem(ActorSystem system) {
      this.system = system;
      return this;
   }

   public AlpakkeerBuilder withJob(JobDefinition<?> job) {
      jobs.add(job);
      return this;
   }

   public Alpakkeer start() {
      if (system == null) {
         system = ActorSystem.create("alpakkeer");
      }

      var scheduler = CronSchedulers.apply();
      var resources = Resources.apply(system, scheduler);
      this.jobs.forEach(resources::addJob);

      return Alpakkeer.apply(system, scheduler, resources);
   }

}

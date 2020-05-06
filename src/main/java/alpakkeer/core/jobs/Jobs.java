package alpakkeer.core.jobs;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import alpakkeer.core.jobs.actor.JobActor;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobStatus;
import alpakkeer.core.jobs.model.JobStatusDetails;
import alpakkeer.core.jobs.model.ScheduledExecution;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.ActorPatterns;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class Jobs {

   private static final Logger LOG = LoggerFactory.getLogger(Jobs.class);

   private Jobs() {

   }

   @AllArgsConstructor(staticName = "apply")
   private static class ActorJob<P> implements Job<P> {

      private final JobDefinition<P> definition;

      private final ActorRef<Message<P>> actor;

      private final ActorPatterns patterns;

      @Override
      public JobDefinition<P> getDefinition() {
         return definition;
      }

      @Override
      public CompletionStage<Done> start(P properties, Boolean queue) {
         return patterns.ask(actor, (replyTo, errorTo) -> Start.apply(queue, properties, replyTo, errorTo));
      }

      @Override
      public CompletionStage<Done> cancel(Boolean clearQueue) {
         return patterns.ask(actor, replyTo -> Stop.apply(clearQueue, replyTo));
      }

      @Override
      public CompletionStage<ScheduledExecution<P>> schedule(P properties, Boolean queue, CronExpression cron) {
         return patterns.ask(actor, replyTo -> Schedule.apply(cron, properties, queue, replyTo));
      }

      @Override
      public CompletionStage<JobStatus<P>> getStatus() {
         return patterns.ask(actor, Status::apply);
      }

      @Override
      public CompletionStage<JobStatusDetails<P>> getStatusDetails() {
         return patterns.ask(actor, StatusDetails::apply);
      }

   }

   public static <P> Job<P> apply(ActorSystem system, CronScheduler scheduler, JobDefinition<P> definition) {
      var behavior = JobActor.apply(definition, scheduler);
      var actor = Adapter.spawn(system, behavior, definition.getName().getValue());
      var actorJob = ActorJob.apply(definition, actor, ActorPatterns.apply(system));

      definition.getSchedule().forEach(s -> actorJob
         .schedule(s.getProperties(), s.isQueue(), s.getCron())
         .whenComplete((scheduled, exception) -> {
            if (exception != null) {
               LOG.warn(
                  String.format(
                     "Exception occurred while scheduling execution for job `%s` with cron `%s`",
                     definition.getName().getValue(), s.getCron().getValue()),
                  exception);
            } else {
               LOG.info(
                  "Scheduled execution for job `{}` for `{}`, next execution is `{}`",
                  definition.getName().getValue(), s.getCron().getValue(), scheduled.getNextExecution());
            }
         }));

      return actorJob;
   }

}

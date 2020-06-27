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
   private static class ActorJob<P, C> implements Job<P, C> {

      private final JobDefinition<P, C> definition;

      private final ActorRef<Message<P, C>> actor;

      private final ActorPatterns patterns;

      @Override
      public JobDefinition<P, C> getDefinition() {
         return definition;
      }

      @Override
      public CompletionStage<C> start(Boolean queue, P properties) {
         CompletionStage<CompletionStage<C>> started = patterns.ask(actor, (replyTo, errorTo) -> Start.apply(queue, properties, replyTo, errorTo));
         return started.thenCompose(c -> c);
      }

      @Override
      public CompletionStage<Done> cancel(Boolean clearQueue) {
         return patterns.ask(actor, replyTo -> Stop.apply(clearQueue, replyTo));
      }

      @Override
      public CompletionStage<ScheduledExecution<P>> schedule(CronExpression cron, Boolean queue, P properties) {
         return patterns.ask(actor, replyTo -> Schedule.apply(cron, properties, queue, replyTo));
      }

      @Override
      public CompletionStage<JobStatus> getStatus() {
         return patterns.ask(actor, Status::apply);
      }

      @Override
      public CompletionStage<JobStatusDetails<P, C>> getStatusDetails() {
         return patterns.ask(actor, StatusDetails::apply);
      }

   }

   public static <P, C> Job<P, C> apply(ActorSystem system, CronScheduler scheduler, ContextStore contextStore, JobDefinition<P, C> definition) {
      var behavior = JobActor.apply(definition, scheduler, contextStore);
      var actor = Adapter.spawn(system, behavior, definition.getName());
      var actorJob = ActorJob.apply(definition, actor, ActorPatterns.apply(system));

      definition.getSchedule().forEach(s -> actorJob
         .schedule(s.getCron(), s.isQueue(), s.getProperties())
         .whenComplete((scheduled, exception) -> {
            if (exception != null) {
               LOG.warn(
                  String.format(
                     "Exception occurred while scheduling execution for job `%s` with cron `%s`",
                     definition.getName(), s.getCron().getValue()),
                  exception);
            } else {
               LOG.info(
                  "Scheduled execution for job `{}` for `{}`, next execution is `{}`",
                  definition.getName(), s.getCron().getValue(), scheduled.getNextExecution());
            }
         }));

      return actorJob;
   }

}

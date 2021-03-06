package alpakkeer.core.jobs.actor.states;

import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecutionInternal;
import alpakkeer.core.jobs.actor.context.QueuedExecutionInternal;
import alpakkeer.core.jobs.actor.context.ScheduledExecutionReference;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.exceptions.AlreadyRunningException;
import alpakkeer.core.jobs.model.*;
import alpakkeer.core.util.Operators;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public abstract class State<P, C> {

   protected final Logger log;

   protected final JobState state;

   protected final ActorContext<Message<P, C>> actor;

   protected final Context<P, C> context;

   protected State(JobState state, ActorContext<Message<P, C>> actor, Context<P, C> context) {
      this.state = state;
      this.actor = actor;
      this.context = context;
      this.log = context.getJobDefinition().getLogger();
   }

   public abstract State<P, C> onCompleted(Completed<P, C> completed);

   public abstract State<P, C> onFinalized(Finalized<P, C> finalized);

   public abstract State<P, C> onFailed(Failed<P, C> failed);

   public abstract State<P, C> onStart(Start<P, C> start);

   public abstract State<P, C> onStarted(Started<P, C> started);

   public abstract State<P, C> onStop(Stop<P, C> stop);

   public void onSchedule(Schedule<P, C> schedule) {
      String id = UUID.randomUUID().toString();

      context.getScheduler()
         .schedule(id, schedule.getCron(), () ->
            actor.getSelf().tell(Start.apply(
               schedule.isQueue(), schedule.getProperties(),
               actor.getSystem().ignoreRef(), actor.getSystem().ignoreRef())))
         .whenComplete((done, exception) -> {
            if (exception != null) {
               log.warn("An exception occurred while scheduling a jb execution", exception);
            } else {
               actor.getSelf().tell(Scheduled.apply(
                  id, schedule.getCron(), schedule.getProperties(),
                  schedule.isQueue(), schedule.getReplyTo()));
            }
         });
   }

   public void onScheduled(Scheduled<P, C> scheduled) {
      context.addScheduledExecution(ScheduledExecutionReference.apply(
         scheduled.getName(), scheduled.getProperties(), scheduled.isQueue(),
         scheduled.getCron()));

      context.getScheduler().getJob(scheduled.getName()).whenComplete((details, exception) -> {
         if (exception != null) {
            log.warn("An exception occurred while getting job details from scheduler", exception);
         } else {
            details.ifPresentOrElse(
               d -> scheduled.getReplyTo().tell(ScheduledExecution.apply(
                  scheduled.getProperties(), scheduled.isQueue(),
                  scheduled.getCron(), d.getNextExecution())),
               () ->
                  log.warn("Received no job details from scheduler for scheduled execution `{}`", scheduled.getName()));
         }
      });
   }

   public void onStatus(Status<P, C> status) {
      context.getSchedule().thenAccept(schedule -> {
         var result = JobStatus.apply(
            context.getJobDefinition().getName(),
            state, context.getQueue().size());

         status.getReplyTo().tell(result);
      });
   }

   public void onStatusDetails(StatusDetails<P, C> status) {
      Operators
         .compose(
            context.getSchedule(),
            context.getJobDefinition().getMonitors().getStatus(),
            getCurrentContext(),
            (schedule, details, ctx) -> JobStatusDetails.apply(
               context.getJobDefinition().getName(),
               state,
               ctx,
               null,
               context.getQueue().stream().map(QueuedExecutionInternal::getQueuedExecution).collect(Collectors.toList()),
               schedule,
               details.orElse(null)))
         .whenComplete((details, ex) -> {
            if (ex != null) {
               log.warn(
                  String.format("An exception occurred while fetching status details of job `%s`", context.getJobDefinition().getName()),
                  ex);
            } else {
               status.getReplyTo().tell(details);
            }
         });
   }

   protected CompletionStage<C> getCurrentContext() {
      return context
         .getContextStore()
         .<C>readLatestContext(context.getJobDefinition().getName())
         .thenApply(opt -> opt.orElse(context.getJobDefinition().getInitialContext()))
         .exceptionally(ex -> {
            log.warn(String.format(
               "An exception occurred while reading current context of job `%s`", context.getJobDefinition().getName()),
               ex);

            return context.getJobDefinition().getInitialContext();
         });
   }

   protected void setCurrentContext(C ctx) {
      context.getContextStore()
         .saveContext(
            context.getJobDefinition().getName(),
            ctx)
         .whenComplete((done, ex) -> {
            if (ex != null) {
               log.warn(
                  String.format("An exception occurred while storing job context for job `%s`", context.getJobDefinition().getName()),
                  ex);
            }

            actor.getSelf().tell(Finalized.apply());
         });
   }

   protected void queue(Start<P, C> start) {
      if (!start.isQueue() && !context.getQueue().isEmpty()) {
         start.getErrorTo().tell(AlreadyRunningException.apply(context.getJobDefinition().getName()));
      } else {
         var queuedExecution = QueuedExecution.apply(start.getProperties());
         var queuedExecutionInternal = QueuedExecutionInternal.apply(queuedExecution, new CompletableFuture<C>());
         context.getQueue().add(queuedExecutionInternal);
         context.getJobDefinition().getMonitors().onQueued(context.getQueue().size());
         start.getReplyTo().tell(queuedExecutionInternal.getMaybeResult());
      }
   }

   protected State<P, C> processQueue() {
      if (context.getQueue().isEmpty()) {
         return Idle.apply(actor, context);
      } else {
         var execution = context.getQueue().remove(0);
         var eventualHandle = getCurrentContext().thenCompose(
            ctx -> context.getJobDefinition().run(
               execution.getQueuedExecution().getId(),
               execution.getQueuedExecution().getProperties(), ctx));

         eventualHandle.whenComplete((handle, ex) -> {
            if (ex != null) {
               actor.getSelf().tell(Failed.apply(ex));
            } else {
               actor.getSelf().tell(Started.apply(handle));
            }
         });

         context.getJobDefinition().getMonitors().onEnqueued(context.getQueue().size());
         context.getJobDefinition().getMonitors().onTriggered(
            execution.getQueuedExecution().getId(),
            execution.getQueuedExecution().getProperties());

         var current = CurrentExecution.apply(
            execution.getQueuedExecution().getId(),
            execution.getQueuedExecution().getProperties(),
            LocalDateTime.now());

         var currentInternal = CurrentExecutionInternal.apply(current, execution.getMaybeResult());

         return Starting.apply(actor, context, currentInternal);
      }
   }

}

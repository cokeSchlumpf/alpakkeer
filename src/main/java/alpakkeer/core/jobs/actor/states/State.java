package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecution;
import alpakkeer.core.jobs.actor.context.ScheduledExecutionReference;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.exceptions.AlreadyRunningException;
import alpakkeer.core.jobs.model.*;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public abstract class State<P, C> {

   protected final Logger LOG;

   protected final JobState state;

   protected final ActorContext<Message<P, C>> actor;

   protected final Context<P, C> context;

   protected State(JobState state, ActorContext<Message<P, C>> actor, Context<P, C> context) {
      this.state = state;
      this.actor = actor;
      this.context = context;
      this.LOG = context.getJobDefinition().getLogger();
   }

   public abstract State<P, C> onCompleted(Completed<P, C> completed);

   public abstract State<P, C> onFailed(Failed<P, C> failed);

   public abstract State<P, C> onStart(Start<P, C> start);

   public abstract State<P, C> onStarted(Started<P, C> started);

   public abstract State<P, C> onStop(Stop<P, C> stop);

   public void onSchedule(Schedule<P, C> schedule) {
      String id = UUID.randomUUID().toString();
      var name = Name.apply(id);

      context.getScheduler()
         .schedule(name, schedule.getCron(), () ->
            actor.getSelf().tell(Start.apply(
               schedule.isQueue(), schedule.getProperties(),
               actor.getSystem().ignoreRef(), actor.getSystem().ignoreRef())))
         .whenComplete((done, exception) -> {
            if (exception != null) {
               LOG.warn("An exception occurred while scheduling a jb execution", exception);
            } else {
               actor.getSelf().tell(Scheduled.apply(
                  name, schedule.getCron(), schedule.getProperties(),
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
            LOG.warn("An exception occurred while getting job details from scheduler", exception);
         } else {
            details.ifPresentOrElse(
               d -> scheduled.getReplyTo().tell(ScheduledExecution.apply(
                  scheduled.getProperties(), scheduled.isQueue(),
                  scheduled.getCron(), d.getNextExecution())),
               () ->
                  LOG.warn("Received no job details from scheduler for scheduled execution `{}`", scheduled.getName().getValue()));
         }
      });
   }

   public void onStatus(Status<P, C> status) {
      context.getSchedule().thenAccept(schedule -> {
         var result = JobStatus.apply(
            context.getJobDefinition().getName().getValue(),
            state, (C) null, ImmutableList.copyOf(context.getQueue()), schedule); // TODO: Set context

         status.getReplyTo().tell(result);
      });
   }

   public void onStatusDetails(StatusDetails<P, C> status) {
      Operators
         .compose(
            context.getSchedule(),
            context.getJobDefinition().getMonitors().getStatus(),
            (schedule, details) -> {
               var jobStatus = JobStatus.apply(
                  context.getJobDefinition().getName().getValue(),
                  state, (C) null, List.copyOf(context.getQueue()), schedule); // TODO: Set context

               return JobStatusDetails.apply(jobStatus, details.orElse(null));
            })
         .whenComplete((details, ex) -> {
            if (ex != null) {
               LOG.warn(
                  String.format("An exception occurred while fetching status details of job `%s`", context.getJobDefinition().getName().getValue()),
                  ex);
            } else {
               status.getReplyTo().tell(details);
            }
         });
   }

   protected void queue(Start<P, C> start) {
      if (!start.isQueue()) {
         start.getErrorTo().tell(AlreadyRunningException.apply(context.getJobDefinition().getName()));
      } else {
         context.getQueue().add(QueuedExecution.apply(start.getProperties()));
         context.getJobDefinition().getMonitors().onQueued(context.getQueue().size());
         start.getReplyTo().tell(Done.getInstance());
      }
   }

   protected State<P, C> processQueue() {
      if (context.getQueue().isEmpty()) {
         return Idle.apply(actor, context);
      } else {
         var execution = context.getQueue().remove(0);
         var eventualHandle = context.getJobDefinition().run(execution.getId(), execution.getProperties(), null); // TODO: Set context

         eventualHandle.whenComplete((handle, ex) -> {
            if (ex != null) {
               actor.getSelf().tell(Failed.apply(ex));
            } else {
               actor.getSelf().tell(Started.apply(handle));
            }
         });

         context.getJobDefinition().getMonitors().onEnqueued(context.getQueue().size());
         context.getJobDefinition().getMonitors().onTriggered(execution.getId(), execution.getProperties());

         var current = CurrentExecution.apply(execution.getId(), execution.getProperties(), LocalDateTime.now());
         return Starting.apply(actor, context, current);
      }
   }

}

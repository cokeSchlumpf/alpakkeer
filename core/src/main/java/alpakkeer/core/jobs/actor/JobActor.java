package alpakkeer.core.jobs.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import alpakkeer.core.jobs.ContextStore;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.actor.states.Idle;
import alpakkeer.core.jobs.actor.states.State;
import alpakkeer.core.scheduler.CronScheduler;

public final class JobActor<P, C> extends AbstractBehavior<Message<P, C>> {

   private State<P, C> state;

   private JobActor(ActorContext<Message<P, C>> context, JobDefinition<P, C> definition, CronScheduler scheduler, ContextStore contextStore) {
      super(context);
      this.state = Idle.apply(context, Context.apply(definition, scheduler, contextStore));
   }

   public static <P, C> Behavior<Message<P, C>> apply(JobDefinition<P, C> definition, CronScheduler scheduler, ContextStore contextStore) {
      return Behaviors.setup(ctx -> new JobActor<>(ctx, definition, scheduler, contextStore));
   }

   @Override
   @SuppressWarnings("unchecked")
   public Receive<Message<P, C>> createReceive() {
      return newReceiveBuilder()
         .onMessage(Completed.class, completed -> this.onCompleted((Completed<P, C>) completed))
         .onMessage(Finalized.class, finalized -> this.onFinalized((Finalized<P, C>) finalized))
         .onMessage(Schedule.class, schedule -> this.onSchedule((Schedule<P, C>) schedule))
         .onMessage(Scheduled.class, scheduled -> this.onScheduled((Scheduled<P, C>) scheduled))
         .onMessage(Start.class, start -> this.onStart((Start<P, C>) start))
         .onMessage(Started.class, started -> this.onStarted((Started<P, C>) started))
         .onMessage(Status.class, status -> this.onStatus((Status<P, C>) status))
         .onMessage(StatusDetails.class, status -> this.onStatusDetails((StatusDetails<P, C>) status))
         .onMessage(Stop.class, stop -> this.onStop((Stop<P, C>) stop))
         .onMessage(Failed.class, failed -> this.onFailed((Failed<P, C>) failed))
         .build();
   }

   private Behavior<Message<P, C>> onCompleted(Completed<P, C> completed) {
      state = state.onCompleted(completed);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onFinalized(Finalized<P, C> finalized) {
      state = state.onFinalized(finalized);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onFailed(Failed<P, C> failed) {
      state = state.onFailed(failed);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onSchedule(Schedule<P, C> schedule) {
      state.onSchedule(schedule);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onScheduled(Scheduled<P, C> scheduled) {
      state.onScheduled(scheduled);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onStart(Start<P, C> start) {
      state = state.onStart(start);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onStarted(Started<P, C> started) {
      state = state.onStarted(started);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onStatus(Status<P, C> status) {
      state.onStatus(status);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onStatusDetails(StatusDetails<P, C> status) {
      state.onStatusDetails(status);
      return Behaviors.same();
   }

   private Behavior<Message<P, C>> onStop(Stop<P, C> stop) {
      state = state.onStop(stop);
      return Behaviors.same();
   }

}

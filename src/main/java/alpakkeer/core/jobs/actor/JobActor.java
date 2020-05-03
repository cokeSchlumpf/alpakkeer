package alpakkeer.core.jobs.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.actor.states.Idle;
import alpakkeer.core.jobs.actor.states.State;
import alpakkeer.core.scheduler.CronScheduler;

public final class JobActor<P> extends AbstractBehavior<Message<P>> {

   private State<P> state;

   private JobActor(ActorContext<Message<P>> context, JobDefinition<P> definition, CronScheduler scheduler) {
      super(context);
      this.state = Idle.apply(context, Context.apply(definition, scheduler));
   }

   public static <P> Behavior<Message<P>> apply(JobDefinition<P> definition, CronScheduler scheduler) {
      return Behaviors.setup(ctx -> new JobActor<>(ctx, definition, scheduler));
   }

   @Override
   @SuppressWarnings("unchecked")
   public Receive<Message<P>> createReceive() {
      return newReceiveBuilder()
         .onMessage(Completed.class, completed -> this.onCompleted((Completed<P>) completed))
         .onMessage(Schedule.class, schedule -> this.onSchedule((Schedule<P>) schedule))
         .onMessage(Scheduled.class, scheduled -> this.onScheduled((Scheduled<P>) scheduled))
         .onMessage(Start.class, start -> this.onStart((Start<P>) start))
         .onMessage(Started.class, started -> this.onStarted((Started<P>) started))
         .onMessage(Status.class, status -> this.onStatus((Status<P>) status))
         .onMessage(Stop.class, stop -> this.onStop((Stop<P>) stop))
         .onMessage(Failed.class, failed -> this.onFailed((Failed<P>) failed))
         .build();
   }

   private Behavior<Message<P>> onCompleted(Completed<P> completed) {
      state = state.onCompleted(completed);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onFailed(Failed<P> failed) {
      state = state.onFailed(failed);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onSchedule(Schedule<P> schedule) {
      state.onSchedule(schedule);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onScheduled(Scheduled<P> scheduled) {
      state.onScheduled(scheduled);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onStart(Start<P> start) {
      state = state.onStart(start);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onStarted(Started<P> started) {
      state = state.onStarted(started);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onStatus(Status<P> status) {
      state.onStatus(status);
      return Behaviors.same();
   }

   private Behavior<Message<P>> onStop(Stop<P> stop) {
      state = state.onStop(stop);
      return Behaviors.same();
   }

}

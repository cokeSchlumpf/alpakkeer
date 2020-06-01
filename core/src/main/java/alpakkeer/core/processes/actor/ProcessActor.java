package alpakkeer.core.processes.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import alpakkeer.core.processes.ProcessDefinition;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.actor.states.State;
import alpakkeer.core.processes.actor.states.Stopped;

public final class ProcessActor extends AbstractBehavior<Message> {

   private State state;

   public ProcessActor(ActorContext<Message> actor, State initialState) {
      super(actor);
      state = initialState;
   }

   public static Behavior<Message> create(ProcessDefinition definition) {
      return Behaviors.setup(actor -> Behaviors.withTimers(scheduler -> {
         var context = ProcessContext.apply(actor, scheduler, definition, definition.getLogger());
         var initialState = Stopped.apply(context);
         return new ProcessActor(actor, initialState);
      }));
   }

   @Override
   public Receive<Message> createReceive() {
      return newReceiveBuilder()
         .onMessage(Completed.class, this::onCompleted)
         .onMessage(Failed.class, this::onFailed)
         .onMessage(Start.class, this::onStart)
         .onMessage(Started.class, this::onStarted)
         .onMessage(Status.class, this::onStatus)
         .onMessage(StatusDetails.class, this::onStatusDetails)
         .onMessage(Stop.class, this::onStop)
         .build();
   }


   private Behavior<Message> onCompleted(Completed completed) {
      state = state.onCompleted(completed);
      return Behaviors.same();
   }

   private Behavior<Message> onFailed(Failed failed) {
      state = state.onFailed(failed);
      return Behaviors.same();
   }

   private Behavior<Message> onStart(Start start) {
      state = state.onStart(start);
      return Behaviors.same();
   }

   private Behavior<Message> onStarted(Started started) {
      state = state.onStarted(started);
      return Behaviors.same();
   }

   private Behavior<Message> onStatus(Status status) {
      state.onStatus(status);
      return Behaviors.same();
   }

   private Behavior<Message> onStatusDetails(StatusDetails status) {
      state.onStatusDetails(status);
      return Behaviors.same();
   }

   private Behavior<Message> onStop(Stop stop) {
      state = state.onStop(stop);
      return Behaviors.same();
   }

}

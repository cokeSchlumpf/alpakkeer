package alpakkeer.core.processes.actor.states;

import akka.Done;
import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;
import alpakkeer.core.processes.model.ProcessStatus;

import java.time.Instant;

public final class Idle extends State {

   private final Instant nextRestart;

   private Idle(ProcessContext context, Instant nextRestart) {
      super(ProcessState.IDLE, context);
      this.nextRestart = nextRestart;
   }

   public static Idle apply(ProcessContext context, Instant nextRestart) {
      return new Idle(context, nextRestart);
   }

   @Override
   public State onCompleted(Completed completed) {
      context.getLog().warn("Received unexpected `Completed` message in state `idle`.");
      return this;
   }

   @Override
   public State onFailed(alpakkeer.core.processes.actor.protocol.Failed failed) {
      context.getLog().warn("Received unexpected `Failed` message in state `idle`.");
      return this;
   }

   @Override
   public State onStart(Start start) {
      context.getScheduler().cancelAll();
      this.start();
      return Starting.apply(context, start, context.getDefinition().getInitialRetryBackoff());
   }

   @Override
   public State onStarted(Started started) {
      context.getLog().warn("Received unexpected `Started` message in state `idle`.");
      return this;
   }

   @Override
   public State onStop(Stop stop) {
      context.getScheduler().cancelAll();
      stop.getReplyTo().tell(Done.getInstance());
      return Stopped.apply(context);
   }

   @Override
   public void onStatus(Status status) {
      var s = ProcessStatus.apply(
         context.getDefinition().getName().toString(),
         state,
         nextRestart);

      status.getReplyTo().tell(s);
   }
}

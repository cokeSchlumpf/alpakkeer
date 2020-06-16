package alpakkeer.core.processes.actor.states;

import akka.Done;
import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;
import alpakkeer.core.processes.model.ProcessStatus;

import java.time.Duration;
import java.time.Instant;

public final class Failed extends State {

   private final Duration currentBackoff;

   private final Instant nextRestart;

   private Failed(ProcessContext context, Duration currentBackoff, Instant nextRestart) {
      super(ProcessState.FAILED, context);
      this.currentBackoff = currentBackoff;
      this.nextRestart = nextRestart;
   }

   public static Failed apply(ProcessContext context, Duration currentBackoff, Instant nextRestart) {
      return new Failed(context, currentBackoff, nextRestart);
   }

   @Override
   public State onCompleted(Completed completed) {
      context.getLog().warn("Received unexpected `Completed` message in state `failed`.");
      return this;
   }

   @Override
   public State onFailed(alpakkeer.core.processes.actor.protocol.Failed failed) {
      context.getLog().warn("Received unexpected `Failed` message in state `failed`.");
      return this;
   }

   @Override
   public State onStart(Start start) {
      var executionId = start();
      this.context.getScheduler().cancelAll();
      return Starting.apply(context, start, currentBackoff.multipliedBy(2), executionId);
   }

   @Override
   public State onStarted(Started started) {
      context.getLog().warn("Received unexpected `Started` message in state `failed`.");
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
      var s = ProcessStatus.apply(context.getDefinition().getName().toString(), this.state, nextRestart);
      status.getReplyTo().tell(s);
   }
}

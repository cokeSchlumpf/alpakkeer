package alpakkeer.core.processes.actor.states;

import akka.Done;
import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;
import alpakkeer.core.processes.model.ProcessStatus;

public final class Stopped extends State {

   private Stopped(ProcessContext context) {
      super(ProcessState.STOPPED, context);
   }

   public static Stopped apply(ProcessContext context) {
      return new Stopped(context);
   }

   @Override
   public State onCompleted(Completed completed) {
      context.getLog().warn("Received unexpected message `Completed` in state `stopped`");
      return this;
   }

   @Override
   public State onFailed(alpakkeer.core.processes.actor.protocol.Failed failed) {
      context.getLog().warn("Received unexpected message `Failed` in state `stopped`");
      return this;
   }

   @Override
   public State onStart(Start start) {
      this.start();
      return Starting.apply(context, start, context.getDefinition().getInitialRetryBackoff());
   }

   @Override
   public State onStarted(Started started) {
      context.getLog().warn("Received unexpected message `Started` in state `stopped`");
      return this;
   }

   @Override
   public State onStop(Stop stop) {
      stop.getReplyTo().tell(Done.getInstance());
      return this;
   }

   @Override
   public void onStatus(Status status) {
      var s = ProcessStatus.apply(context.getDefinition().getName().toString(), state);
      status.getReplyTo().tell(s);
   }
}

package alpakkeer.core.processes.actor.states;

import akka.Done;
import alpakkeer.core.processes.ProcessHandle;
import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;
import alpakkeer.core.processes.model.ProcessStatus;

import java.time.Duration;
import java.time.Instant;

public final class Running extends State {

   private final Duration nextRetryBackoff;

   private final ProcessHandle handle;

   private final Instant started;

   private Running(ProcessContext context, Duration nextRetryBackoff, ProcessHandle handle, Instant started) {
      super(ProcessState.RUNNING, context);
      this.nextRetryBackoff = nextRetryBackoff;
      this.handle = handle;
      this.started = started;
   }

   public static Running apply(ProcessContext context, Duration nextRetryBackoff, ProcessHandle handle) {
      return new Running(context, nextRetryBackoff, handle, Instant.now());
   }

   @Override
   public State onCompleted(Completed completed) {
      var start = Start.apply(context.getActor().getSystem().ignoreRef(), context.getActor().getSystem().ignoreRef());
      var nextRestart = Instant.now().plusMillis(context.getDefinition().getCompletionRestartBackoff().toMillis());
      context.getScheduler().startSingleTimer(start, context.getDefinition().getCompletionRestartBackoff());
      return Idle.apply(context, nextRestart);
   }

   @Override
   public State onFailed(alpakkeer.core.processes.actor.protocol.Failed failed) {
      var start = Start.apply(context.getActor().getSystem().ignoreRef(), context.getActor().getSystem().ignoreRef());

      var timout = started.plusMillis(context.getDefinition().getRetryBackoffResetTimeout().toMillis()).toEpochMilli();
      var now = Instant.now().toEpochMilli();

      if (now < timout) {
         context.getScheduler().startSingleTimer(start, nextRetryBackoff);
         var nextRetry = Instant.now().plusMillis(nextRetryBackoff.toMillis());
         return Failed.apply(context, nextRetryBackoff, nextRetry);
      } else {
         context.getScheduler().startSingleTimer(start, context.getDefinition().getInitialRetryBackoff());
         var nextRetry = Instant.now().plusMillis(context.getDefinition().getInitialRetryBackoff().toMillis());
         return Failed.apply(context, nextRetryBackoff, nextRetry);
      }
   }

   @Override
   public State onStart(Start start) {
      start.getReplyTo().tell(Done.getInstance());
      return this;
   }

   @Override
   public State onStarted(Started started) {
      context.getLog().warn("Received unexpected message `Started` in state `running`");
      return this;
   }

   @Override
   public State onStop(Stop stop) {
      handle.stop();
      return Stopping.apply(context, stop);
   }

   @Override
   public void onStatus(Status status) {
      var s = ProcessStatus.apply(context.getDefinition().getName().toString(), state);
      status.getReplyTo().tell(s);
   }
}

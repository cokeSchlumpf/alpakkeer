package alpakkeer.core.processes.actor.states;

import akka.Done;
import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;
import alpakkeer.core.processes.model.ProcessStatus;
import com.google.common.collect.Lists;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class Starting extends State {

   private final List<Start> starts;

   private final List<Stop> stops;

   private final Duration nextRetryBackOff;

   private final String executionId;

   private Starting(
      ProcessContext context, List<Start> starts, List<Stop> stops, Duration nextRetryBackoff, String executionId) {

      super(ProcessState.STARTING, context);
      this.starts = starts;
      this.stops = stops;
      this.nextRetryBackOff = nextRetryBackoff;
      this.executionId = executionId;
   }

   public static Starting apply(ProcessContext context, Start cmd, Duration nextRetryBackOff, String executionId) {
      var starts = Lists.newArrayList(cmd);
      var stops = Lists.<Stop>newArrayList();

      return new Starting(context, starts, stops, nextRetryBackOff, executionId);
   }

   @Override
   public State onCompleted(Completed completed) {
      context.getLog().warn("Received unexpected message `Completed` in state `starting`");
      return this;
   }

   @Override
   public State onFailed(alpakkeer.core.processes.actor.protocol.Failed failed) {
      var nextRestart = Instant.now().plusMillis(nextRetryBackOff.toMillis());
      var start = Start.apply(
         context.getActor().getSystem().ignoreRef(),
         context.getActor().getSystem().ignoreRef());

      context.getScheduler().startSingleTimer(start, nextRetryBackOff);
      context.getDefinition().getMonitors().onFailed(executionId, failed.getException(), nextRestart);
      return Failed.apply(context, nextRetryBackOff, nextRestart);
   }

   @Override
   public State onStart(Start start) {
      this.starts.add(start);
      return this;
   }

   @Override
   public State onStarted(Started started) {
      context.getDefinition().getMonitors().onStarted(executionId);

      started.getHandle().getCompletion().whenComplete((done, ex) -> {
         if (ex != null) {
            context.getActor().getSelf().tell(alpakkeer.core.processes.actor.protocol.Failed.apply(ex));
         } else {
            context.getActor().getSelf().tell(Completed.apply());
         }
      });

      starts.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      stops.forEach(s -> context.getActor().getSelf().tell(s));
      return Running.apply(context, executionId, nextRetryBackOff, started.getHandle());
   }

   @Override
   public State onStop(Stop stop) {
      this.stops.add(stop);
      return this;
   }

   @Override
   public void onStatus(Status status) {
      var s = ProcessStatus.apply(context.getDefinition().getName().toString(), state);
      status.getReplyTo().tell(s);
   }
}

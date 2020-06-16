package alpakkeer.core.processes.actor.states;

import akka.Done;
import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.Failed;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;
import alpakkeer.core.processes.model.ProcessStatus;
import com.google.common.collect.Lists;

import java.util.List;

public final class Stopping extends State {

   private final List<Start> starts;

   private final List<Stop> stops;

   private final String executionId;

   private Stopping(ProcessContext context, List<Start> starts, List<Stop> stops, String executionId) {
      super(ProcessState.STOPPING, context);
      this.starts = starts;
      this.stops = stops;
      this.executionId = executionId;
   }

   public static Stopping apply(ProcessContext context, Stop cmd, String executionId) {
      return new Stopping(context, Lists.newArrayList(), Lists.newArrayList(cmd), executionId);
   }

   @Override
   public State onCompleted(Completed completed) {
      context.getDefinition().getMonitors().onStopped(executionId);
      stops.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      starts.forEach(s -> context.getActor().getSelf().tell(s));
      return Stopped.apply(context);
   }

   @Override
   public State onFailed(Failed failed) {
      context.getLog().warn(String.format(
         "Process execution `%s` stopped with Exception",
         executionId), failed.getException());
      context.getDefinition().getMonitors().onStopped(executionId);
      stops.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      starts.forEach(s -> context.getActor().getSelf().tell(s));
      return Stopped.apply(context);
   }

   @Override
   public State onStart(Start start) {
      this.starts.add(start);
      return this;
   }

   @Override
   public State onStarted(Started started) {
      context.getLog().warn("Received unexpected message `Started` in state `stopping`");
      return this;
   }

   @Override
   public State onStop(Stop stop) {
      stops.add(stop);
      return this;
   }

   @Override
   public void onStatus(Status status) {
      var s = ProcessStatus.apply(context.getDefinition().getName().toString(), state);
      status.getReplyTo().tell(s);
   }

}

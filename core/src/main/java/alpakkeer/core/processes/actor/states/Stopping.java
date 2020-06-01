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

   private Stopping(ProcessContext context, List<Start> starts, List<Stop> stops) {
      super(ProcessState.STOPPING, context);
      this.starts = starts;
      this.stops = stops;
   }

   public static Stopping apply(ProcessContext context, Stop cmd) {
      return new Stopping(context, Lists.newArrayList(), Lists.newArrayList(cmd));
   }

   @Override
   public State onCompleted(Completed completed) {
      stops.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      starts.forEach(s -> context.getActor().getSelf().tell(s));
      return Stopped.apply(context);
   }

   @Override
   public State onFailed(Failed failed) {
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

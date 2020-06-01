package alpakkeer.core.processes.actor.states;

import alpakkeer.core.processes.actor.ProcessContext;
import alpakkeer.core.processes.actor.protocol.*;
import alpakkeer.core.processes.model.ProcessState;

import java.util.UUID;

public abstract class State {

   protected final ProcessState state;

   protected final ProcessContext context;

   protected State(ProcessState state, ProcessContext context) {
      this.context = context;
      this.state = state;
   }

   public abstract State onCompleted(Completed completed);

   public abstract State onFailed(alpakkeer.core.processes.actor.protocol.Failed failed);

   public abstract State onStart(Start start);

   public abstract State onStarted(Started started);

   public abstract State onStop(Stop stop);

   public abstract void onStatus(Status status);

   public void onStatusDetails(StatusDetails statusDetails) {

   }

   protected void start() {
      context
         .getDefinition()
         .run(UUID.randomUUID().toString())
         .whenComplete((handle, ex) -> {
            if (ex != null) {
               context.getActor().getSelf().tell(alpakkeer.core.processes.actor.protocol.Failed.apply(ex));
            } else {
               context.getActor().getSelf().tell(Started.apply(handle));
            }
         });
   }

}

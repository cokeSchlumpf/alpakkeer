package alpakkeer.core.jobs.actor.states;

import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecution;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import com.google.common.collect.Lists;

import java.util.List;

public final class Starting<P> extends State<P> {

   private final CurrentExecution<P> currentExecution;

   private final List<Stop<P>> stopRequests;

   private Starting(
      ActorContext<Message<P>> actor,
      Context<P> context,
      CurrentExecution<P> currentExecution,
      List<Stop<P>> stopRequests) {

      super(JobState.RUNNING, actor, context);
      this.currentExecution = currentExecution;
      this.stopRequests = stopRequests;
   }

   public static <P> Starting<P> apply(
      ActorContext<Message<P>> actor,
      Context<P> context,
      CurrentExecution<P> currentExecution) {

      return new Starting<>(actor, context, currentExecution, Lists.newArrayList());
   }

   @Override
   public State<P> onCompleted(Completed<P> completed) {
      LOG.warn("Received unexpected message `Completed` in state `starting`");
      return this;
   }

   @Override
   public State<P> onFailed(Failed<P> failed) {
      LOG.warn("An exception occurred while starting job", failed.getException());
      return processQueue();
   }

   @Override
   public State<P> onStart(Start<P> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P> onStarted(Started<P> started) {
      started.getHandle().getCompletion().whenComplete((done, exception) -> {
         if (exception != null) {
            actor.getSelf().tell(Failed.apply(exception));
         } else {
            actor.getSelf().tell(Completed.apply());
         }
      });

      stopRequests.forEach(actor.getSelf()::tell);
      return Running.apply(actor, context, currentExecution, started.getHandle());
   }

   @Override
   public State<P> onStop(Stop<P> stop) {
      stopRequests.add(stop);
      if (stop.isClearQueue()) context.getQueue().clear();
      return this;
   }

}

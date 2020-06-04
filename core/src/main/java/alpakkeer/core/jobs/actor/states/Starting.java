package alpakkeer.core.jobs.actor.states;

import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecutionInternal;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import com.google.common.collect.Lists;

import java.util.List;

public final class Starting<P, C> extends State<P, C> {

   private final CurrentExecutionInternal<P, C> currentExecution;

   private final List<Stop<P, C>> stopRequests;

   private Starting(
      ActorContext<Message<P, C>> actor,
      Context<P, C> context,
      CurrentExecutionInternal<P, C> currentExecution,
      List<Stop<P, C>> stopRequests) {

      super(JobState.RUNNING, actor, context);
      this.currentExecution = currentExecution;
      this.stopRequests = stopRequests;
   }

   public static <P, C> Starting<P, C> apply(
      ActorContext<Message<P, C>> actor,
      Context<P, C> context,
      CurrentExecutionInternal<P, C> currentExecution) {

      return new Starting<>(actor, context, currentExecution, Lists.newArrayList());
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      log.warn("Received unexpected message `Completed` in state `starting`");
      return this;
   }

   @Override
   public State<P, C> onFinalized(Finalized<P, C> finalized) {
      log.warn("Received unexpected message `Finalized` in state `starting`");
      return this;
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      log.warn("An exception occurred while starting job", failed.getException());

      context.getJobDefinition().getMonitors().onFailed(
         currentExecution.getCurrentExecution().getId(),
         failed.getException());

      currentExecution.getCompletableFuture().completeExceptionally(failed.getException());

      return processQueue();
   }

   @Override
   public State<P, C> onStart(Start<P, C> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P, C> onStarted(Started<P, C> started) {
      context.getJobDefinition().getMonitors().onStarted(
         currentExecution.getCurrentExecution().getId());

      started.getHandle().getCompletion().whenComplete((done, exception) -> {
         if (exception != null) {
            actor.getSelf().tell(Failed.apply(exception));
         } else {
            actor.getSelf().tell(done.map(Completed::<P, C>apply).orElse(Completed.apply()));
         }
      });

      stopRequests.forEach(actor.getSelf()::tell);
      return Running.apply(
         actor, context, currentExecution, started.getHandle());
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      stopRequests.add(stop);
      if (stop.isClearQueue()) context.getQueue().clear();
      return this;
   }

}

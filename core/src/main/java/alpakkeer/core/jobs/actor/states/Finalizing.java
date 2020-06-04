package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecutionInternal;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;

public final class Finalizing<P, C> extends State<P, C> {

   private final CurrentExecutionInternal<P, C> currentExecution;

   private final C result;

   private Finalizing(
      JobState state, ActorContext<Message<P, C>> actor, Context<P, C> context,
      CurrentExecutionInternal<P, C> currentExecution, C result) {

      super(state, actor, context);
      this.currentExecution = currentExecution;
      this.result = result;
   }

   public static <P, C> Finalizing<P, C> apply(
      JobState state, ActorContext<Message<P, C>> actor, Context<P, C> context,
      CurrentExecutionInternal<P, C> currentExecution, C result) {

      return new Finalizing<>(state, actor, context, currentExecution, result);
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      log.warn("Received unexpected message `Completed` in state `finalizing`");
      return this;
   }

   @Override
   public State<P, C> onFinalized(Finalized<P, C> finalized) {
      currentExecution.getCompletableFuture().complete(result);
      return processQueue();
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      log.warn("Received unexpected message `Failed` in state `finalizing`");
      return this;
   }

   @Override
   public State<P, C> onStart(Start<P, C> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P, C> onStarted(Started<P, C> started) {
      log.warn("Received unexpected message `Started` in state `finalizing`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      if (stop.isClearQueue()) context.getQueue().clear();
      stop.getReplyTo().tell(Done.getInstance());
      return this;
   }

}

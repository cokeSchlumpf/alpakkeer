package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;

public final class Finalizing<P, C> extends State<P, C> {

   private Finalizing(JobState state, ActorContext<Message<P, C>> actor, Context<P, C> context) {
      super(state, actor, context);
   }

   public static <P, C> Finalizing<P, C> apply(JobState state, ActorContext<Message<P, C>> actor, Context<P, C> context) {
      return new Finalizing<>(state, actor, context);
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      LOG.warn("Received unexpected message `Completed` in state `finalizing`");
      return this;
   }

   @Override
   public State<P, C> onFinalized(Finalized<P, C> finalized) {
      return processQueue();
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      LOG.warn("Received unexpected message `Failed` in state `finalizing`");
      return this;
   }

   @Override
   public State<P, C> onStart(Start<P, C> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P, C> onStarted(Started<P, C> started) {
      LOG.warn("Received unexpected message `Started` in state `finalizing`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      if (stop.isClearQueue()) context.getQueue().clear();
      stop.getReplyTo().tell(Done.getInstance());
      return this;
   }

}

package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import alpakkeer.core.jobs.model.QueuedExecution;

public final class Idle<P, C> extends State<P, C> {

   private Idle(ActorContext<Message<P, C>> actor, Context<P, C> context) {
      super(JobState.IDLE, actor, context);
   }

   public static <P, C> Idle<P, C> apply(ActorContext<Message<P, C>> actor, Context<P, C> context) {
      assert context.getQueue().isEmpty();

      return new Idle<>(actor, context);
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      LOG.warn("Received unexpected message `Completed` in state `idle`");
      return this;
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      LOG.warn("Received unexpected message `Failed` in state `idle`");
      return this;
   }

   @Override
   public State<P, C> onStart(Start<P, C> start) {
      start.getReplyTo().tell(Done.getInstance());
      context.getQueue().add(QueuedExecution.apply(start.getProperties()));
      return processQueue();
   }

   @Override
   public State<P, C> onStarted(Started<P, C> started) {
      LOG.warn("Received unexpected message `Started` in state `idle`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      stop.getReplyTo().tell(Done.getInstance());
      return this;
   }

}

package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import alpakkeer.core.jobs.model.QueuedExecution;

public final class Idle<P> extends State<P> {

   private Idle(ActorContext<Message<P>> actor, Context<P> context) {
      super(JobState.IDLE, actor, context);
   }

   public static <P> Idle<P> apply(ActorContext<Message<P>> actor, Context<P> context) {
      assert context.getQueue().isEmpty();

      return new Idle<>(actor, context);
   }

   @Override
   public State<P> onCompleted(Completed<P> completed) {
      LOG.warn("Received unexpected message `Completed` in state `idle`");
      return this;
   }

   @Override
   public State<P> onFailed(Failed<P> failed) {
      LOG.warn("Received unexpected message `Failed` in state `idle`");
      return this;
   }

   @Override
   public State<P> onStart(Start<P> start) {
      start.getReplyTo().tell(Done.getInstance());
      context.getQueue().add(QueuedExecution.apply(start.getProperties()));
      return processQueue();
   }

   @Override
   public State<P> onStarted(Started<P> started) {
      LOG.warn("Received unexpected message `Started` in state `idle`");
      return this;
   }

   @Override
   public State<P> onStop(Stop<P> stop) {
      stop.getReplyTo().tell(Done.getInstance());
      return this;
   }

}

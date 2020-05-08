package alpakkeer.core.jobs.actor.states;

import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.JobHandle;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecution;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;

public final class Running<P, C> extends State<P, C> {

   private final CurrentExecution<P> currentExecution;

   private final JobHandle<C> handle;

   private Running(ActorContext<Message<P, C>> actor, Context<P, C> context, CurrentExecution<P> currentExecution, JobHandle<C> handle) {
      super(JobState.RUNNING, actor, context);
      this.currentExecution = currentExecution;
      this.handle = handle;
   }

   public static <P, C> Running<P, C> apply(ActorContext<Message<P, C>> actor, Context<P, C> context, CurrentExecution<P> currentExecution, JobHandle<C> handle) {
      return new Running<>(actor, context, currentExecution, handle);
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      if (completed.getResult().isPresent()) {
         context.getJobDefinition().getMonitors().onCompleted(currentExecution.getId(), completed.getResult().get());
         setCurrentContext(completed.getResult().get());
         return Finalizing.apply(state, actor, context);
      } else {
         context.getJobDefinition().getMonitors().onCompleted(currentExecution.getId());
         return processQueue();
      }
   }

   @Override
   public State<P, C> onFinalized(Finalized<P, C> finalized) {
      LOG.warn("Received unexpected message `Finalized` in state `running`");
      return this;
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      context.getJobDefinition().getMonitors().onFailed(currentExecution.getId(), failed.getException());
      return processQueue();
   }

   @Override
   public State<P, C> onStart(Start<P, C> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P, C> onStarted(Started<P, C> started) {
      LOG.warn("Received unexpected message `Started` in state `running`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      LOG.info("Received request to stop job execution `{}`", currentExecution.getId());

      handle.stop();
      if (stop.isClearQueue()) context.getQueue().clear();
      return Stopping.apply(actor, context, currentExecution, stop);
   }

}

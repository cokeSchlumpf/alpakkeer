package alpakkeer.core.jobs.actor.states;

import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.JobHandle;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecutionInternal;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;

import java.util.NoSuchElementException;

public final class Running<P, C> extends State<P, C> {

   private final CurrentExecutionInternal<P, C> currentExecution;

   private final JobHandle<C> handle;

   private Running(ActorContext<Message<P, C>> actor, Context<P, C> context, CurrentExecutionInternal<P, C> currentExecution, JobHandle<C> handle) {
      super(JobState.RUNNING, actor, context);
      this.currentExecution = currentExecution;
      this.handle = handle;
   }

   public static <P, C> Running<P, C> apply(ActorContext<Message<P, C>> actor, Context<P, C> context, CurrentExecutionInternal<P, C> currentExecution, JobHandle<C> handle) {
      return new Running<>(actor, context, currentExecution, handle);
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      if (completed.getResult().isPresent()) {
         context.getJobDefinition().getMonitors().onCompleted(currentExecution.getCurrentExecution().getId(), completed.getResult().get());
         setCurrentContext(completed.getResult().get());
         return Finalizing.apply(state, actor, context, currentExecution, completed.getResult().get());
      } else {
         var ex = new NoSuchElementException(String.format(
            "Job `%s` did not return a result in execution `%s`",
            context.getJobDefinition().getName(),
            currentExecution.getCurrentExecution().getId()));

         context.getJobDefinition().getMonitors().onCompleted(currentExecution.getCurrentExecution().getId());
         currentExecution.getCompletableFuture().completeExceptionally(ex);

         return processQueue();
      }
   }

   @Override
   public State<P, C> onFinalized(Finalized<P, C> finalized) {
      log.warn("Received unexpected message `Finalized` in state `running`");
      return this;
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      context.getJobDefinition().getMonitors().onFailed(currentExecution.getCurrentExecution().getId(), failed.getException());
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
      log.warn("Received unexpected message `Started` in state `running`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      log.info("Received request to stop job execution `{}`", currentExecution.getCurrentExecution().getId());

      handle.stop();
      if (stop.isClearQueue()) context.getQueue().clear();
      return Stopping.apply(actor, context, currentExecution, stop);
   }

}

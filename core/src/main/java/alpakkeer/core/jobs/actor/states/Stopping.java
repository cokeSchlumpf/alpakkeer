package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecutionInternal;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.NoSuchElementException;

public final class Stopping<P, C> extends State<P, C> {

   private final CurrentExecutionInternal<P, C> currentExecution;

   private final List<Stop<P, C>> stopRequests;

   private Stopping(
      ActorContext<Message<P, C>> actor,
      Context<P, C> context,
      CurrentExecutionInternal<P, C> currentExecution,
      List<Stop<P, C>> stopRequests) {

      super(JobState.STOPPING, actor, context);

      this.currentExecution = currentExecution;
      this.stopRequests = stopRequests;
   }

   public static <P, C> Stopping<P, C> apply(
      ActorContext<Message<P, C>> actor,
      Context<P, C> context,
      CurrentExecutionInternal<P, C> currentExecution,
      Stop<P, C> stopRequest) {

      return new Stopping<>(actor, context, currentExecution, Lists.newArrayList(stopRequest));
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      stopRequests.forEach(s -> s.getReplyTo().tell(Done.getInstance()));

      if (completed.getResult().isPresent()) {
         context.getJobDefinition().getMonitors().onStopped(
            currentExecution.getCurrentExecution().getId(), completed.getResult().get());
         setCurrentContext(completed.getResult().get());
         return Finalizing.apply(state, actor, context, currentExecution, completed.getResult().get());
      } else {
         var ex = new NoSuchElementException(String.format(
            "Job `%s` did not return a result after stopping execution `%s`",
            context.getJobDefinition().getName(),
            currentExecution.getCurrentExecution().getId()));

         context.getJobDefinition().getMonitors().onStopped(
            currentExecution.getCurrentExecution().getId());

         currentExecution.getCompletableFuture().completeExceptionally(ex);

         return processQueue();
      }
   }

   @Override
   public State<P, C> onFinalized(Finalized<P, C> finalized) {
      log.warn("Received unexpected message `Finalized` in state `stopping`");
      return this;
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      log.warn(String.format(
         "Stopped job execution `%s` with failure.",
         currentExecution.getCurrentExecution().getId()), failed.getException());

      context.getJobDefinition().getMonitors().onFailed(
         currentExecution.getCurrentExecution().getId(), failed.getException());

      currentExecution.getCompletableFuture().completeExceptionally(failed.getException());

      stopRequests.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      return processQueue();
   }

   @Override
   public State<P, C> onStart(Start<P, C> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P, C> onStarted(Started<P, C> started) {
      log.warn("Received unexpected message `Started` in state `stopping`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      stopRequests.add(stop);
      if (stop.isClearQueue()) context.getQueue().clear();
      return this;
   }

}

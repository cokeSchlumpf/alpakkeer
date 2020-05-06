package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecution;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import com.google.common.collect.Lists;

import java.util.List;

public final class Stopping<P, C> extends State<P, C> {

   private final CurrentExecution<P> currentExecution;

   private final List<Stop<P, C>> stopRequests;

   private Stopping(
      ActorContext<Message<P, C>> actor,
      Context<P, C> context,
      CurrentExecution<P> currentExecution,
      List<Stop<P, C>> stopRequests) {

      super(JobState.STOPPING, actor, context);

      this.currentExecution = currentExecution;
      this.stopRequests = stopRequests;
   }

   public static <P, C> Stopping<P, C> apply(
      ActorContext<Message<P, C>> actor,
      Context<P, C> context,
      CurrentExecution<P> currentExecution,
      Stop<P, C> stopRequest) {

      return new Stopping<>(actor, context, currentExecution, Lists.newArrayList(stopRequest));
   }

   @Override
   public State<P, C> onCompleted(Completed<P, C> completed) {
      LOG.info("Successfully stopped job execution `{}`", currentExecution.getId());
      context.getJobDefinition().getMonitors().onStopped(currentExecution.getId());
      stopRequests.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      return processQueue();
   }

   @Override
   public State<P, C> onFailed(Failed<P, C> failed) {
      LOG.warn(String.format("Stopped job execution `%s` with failure.", currentExecution.getId()), failed.getException());
      context.getJobDefinition().getMonitors().onFailed(currentExecution.getId(), failed.getException());
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
      LOG.warn("Received unexpected message `Started` in state `stopping`");
      return this;
   }

   @Override
   public State<P, C> onStop(Stop<P, C> stop) {
      stopRequests.add(stop);
      if (stop.isClearQueue()) context.getQueue().clear();
      return this;
   }

}

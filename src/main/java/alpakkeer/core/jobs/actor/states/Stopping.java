package alpakkeer.core.jobs.actor.states;

import akka.Done;
import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.JobHandle;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecution;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;
import com.google.common.collect.Lists;

import java.util.List;

public final class Stopping<P> extends State<P> {

   private final CurrentExecution<P> currentExecution;

   private final JobHandle handle;

   private final List<Stop<P>> stopRequests;

   private Stopping(
      ActorContext<Message<P>> actor,
      Context<P> context,
      CurrentExecution<P> currentExecution,
      JobHandle handle,
      List<Stop<P>> stopRequests) {

      super(JobState.STOPPING, actor, context);

      this.currentExecution = currentExecution;
      this.handle = handle;
      this.stopRequests = stopRequests;
   }

   public static <P> Stopping<P> apply(
      ActorContext<Message<P>> actor,
      Context<P> context,
      CurrentExecution<P> currentExecution,
      JobHandle handle,
      Stop<P> stopRequest) {

      return new Stopping<>(actor, context, currentExecution, handle, Lists.newArrayList(stopRequest));
   }

   @Override
   public State<P> onCompleted(Completed<P> completed) {
      LOG.info("Successfully stopped job execution `{}`", currentExecution.getId());
      stopRequests.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      return processQueue();
   }

   @Override
   public State<P> onFailed(Failed<P> failed) {
      LOG.warn(String.format("Stopped job execution `%s` with failure.", currentExecution.getId()), failed.getException());
      stopRequests.forEach(s -> s.getReplyTo().tell(Done.getInstance()));
      return processQueue();
   }

   @Override
   public State<P> onStart(Start<P> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P> onStarted(Started<P> started) {
      LOG.warn("Received unexpected message `Started` in state `stopping`");
      return this;
   }

   @Override
   public State<P> onStop(Stop<P> stop) {
      stopRequests.add(stop);
      if (stop.isClearQueue()) context.getQueue().clear();
      return this;
   }

}

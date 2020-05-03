package alpakkeer.core.jobs.actor.states;

import akka.actor.typed.javadsl.ActorContext;
import alpakkeer.core.jobs.JobHandle;
import alpakkeer.core.jobs.actor.context.Context;
import alpakkeer.core.jobs.actor.context.CurrentExecution;
import alpakkeer.core.jobs.actor.protocol.*;
import alpakkeer.core.jobs.model.JobState;

public final class Running<P> extends State<P> {

   private final CurrentExecution<P> currentExecution;

   private final JobHandle handle;

   private Running(ActorContext<Message<P>> actor, Context<P> context, CurrentExecution<P> currentExecution, JobHandle handle) {
      super(JobState.RUNNING, actor, context);
      this.currentExecution = currentExecution;
      this.handle = handle;
   }

   public static <P> Running<P> apply(ActorContext<Message<P>> actor, Context<P> context, CurrentExecution<P> currentExecution, JobHandle handle) {
      return new Running<>(actor, context, currentExecution, handle);
   }

   @Override
   public State<P> onCompleted(Completed<P> completed) {
      LOG.info("Successfully finished job execution `{}`", currentExecution.getId());
      return processQueue();
   }

   @Override
   public State<P> onFailed(Failed<P> failed) {
      LOG.warn(String.format("Job execution `%s` finished with exception.", currentExecution.getId()), failed.getException());
      return processQueue();
   }

   @Override
   public State<P> onStart(Start<P> start) {
      queue(start);
      return this;
   }

   @Override
   public State<P> onStarted(Started<P> started) {
      LOG.warn("Received unexpected message `Started` in state `running`");
      return this;
   }

   @Override
   public State<P> onStop(Stop<P> stop) {
      LOG.info("Received request to stop job execution `{}`", currentExecution.getId());

      handle.stop();
      if (stop.isClearQueue()) context.getQueue().clear();
      return Stopping.apply(actor, context, currentExecution, handle, stop);
   }

}

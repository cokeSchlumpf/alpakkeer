package alpakkeer.core.jobs;

import akka.Done;
import alpakkeer.core.jobs.model.JobStatus;
import alpakkeer.core.jobs.model.JobStatusDetails;
import alpakkeer.core.jobs.model.ScheduledExecution;
import alpakkeer.core.scheduler.model.CronExpression;

import java.util.concurrent.CompletionStage;

public interface Job<P, C> {

   /**
    * Returns the job's definition.
    *
    * @return The definition.
    */
   JobDefinition<P, C> getDefinition();

   /**
    * Request to start a job execution.
    *
    * @param queue      Whether the request should be queued if the job is already running.
    * @param properties The properties for the execution.
    * @return The completion stage for the result of the execution.
    */
   CompletionStage<CompletionStage<C>> start(Boolean queue, P properties);

   /**
    * Request to start a job execution with default properties.
    *
    * @param queue Whether the request should be queued if the job is already running.
    * @return The completion stage for the result of the execution.
    */
   default CompletionStage<CompletionStage<C>> start(Boolean queue) {
      return start(queue, getDefinition().getDefaultProperties());
   }

   /**
    * Request to start a job execution with default properties and with queuing.
    *
    * @return The completion stage for the result of the execution.
    */
   default CompletionStage<CompletionStage<C>> start() {
      return start(true);
   }

   /**
    * Cancel job execution(s).
    *
    * @param clearQueue If true, not only the current execution will be cancelled, but also all queued executions.
    * @return Completion Stage which completes when cancellation is done.
    */
   CompletionStage<Done> cancel(Boolean clearQueue);

   /**
    * Schedule job executions.
    *
    * @param cron       The cron expression to defined the schedule.
    * @param queue      Whether the scheduled execution should be queued if the job is already running.
    * @param properties The properties for the scheduled execution.
    * @return The details of the scheduled execution
    */
   CompletionStage<ScheduledExecution<P>> schedule(CronExpression cron, Boolean queue, P properties);

   /**
    * Schedule a job execution with default properties.
    *
    * @param cron The cron expression to defined the schedule.
    * @param queue Whether the scheduled execution should be queued if the job is already running.
    * @return The details of the scheduled execution
    */
   default CompletionStage<ScheduledExecution<P>> schedule(CronExpression cron, Boolean queue) {
      return schedule(cron, queue, getDefinition().getDefaultProperties());
   }

   /**
    * Schedule a job execution with default properties and w/o queuing.
    *
    * @param cron The cron expression to defined the schedule.
    * @return The details of the scheduled execution
    */
   default CompletionStage<ScheduledExecution<P>> schedule(CronExpression cron) {
      return schedule(cron, false, getDefinition().getDefaultProperties());
   }

   /**
    * Returns the overall job status.
    *
    * @return The status
    */
   CompletionStage<JobStatus> getStatus();

   /**
    * Returns the detailed job status.
    *
    * @return Status details
    */
   CompletionStage<JobStatusDetails<P, C>> getStatusDetails();

}

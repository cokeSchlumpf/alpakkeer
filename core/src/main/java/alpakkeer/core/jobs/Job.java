package alpakkeer.core.jobs;

import akka.Done;
import alpakkeer.core.jobs.model.JobStatus;
import alpakkeer.core.jobs.model.JobStatusDetails;
import alpakkeer.core.jobs.model.ScheduledExecution;
import alpakkeer.core.scheduler.model.CronExpression;

import java.util.concurrent.CompletionStage;

public interface Job<P, C> {

   JobDefinition<P, C> getDefinition();

   CompletionStage<CompletionStage<C>> start(P properties, Boolean queue);

   CompletionStage<Done> cancel(Boolean clearQueue);

   CompletionStage<ScheduledExecution<P>> schedule(P properties, Boolean queue, CronExpression cron);

   CompletionStage<JobStatus> getStatus();

   CompletionStage<JobStatusDetails<P, C>> getStatusDetails();

}

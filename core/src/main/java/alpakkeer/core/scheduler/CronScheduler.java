package alpakkeer.core.scheduler;

import akka.Done;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.scheduler.model.JobDetails;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface CronScheduler {

   CompletionStage<Done> schedule(String name, CronExpression cron, Runnable job);

   CompletionStage<Optional<JobDetails>> getJob(String name);

   CompletionStage<List<JobDetails>> getJobs();

   CompletionStage<Done> remove(String name);

   CompletionStage<Done> terminate();

}

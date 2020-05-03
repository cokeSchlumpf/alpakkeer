package alpakkeer.core.scheduler;

import akka.Done;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.scheduler.model.JobDetails;
import alpakkeer.core.values.Name;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface CronScheduler {

   CompletionStage<Done> schedule(Name name, CronExpression cron, Runnable job);

   CompletionStage<Optional<JobDetails>> getJob(Name name);

   CompletionStage<List<JobDetails>> getJobs();

   CompletionStage<Done> remove(Name name);

   CompletionStage<Done> terminate();

}

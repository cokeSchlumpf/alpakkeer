package alpakkeer.core.jobs;

import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.values.Name;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface JobDefinition<P> {

   P getDefaultProperties();

   Name getName();

   List<ScheduleExecution<P>> getSchedule();

   CompletionStage<JobHandle> run(String runId, P properties);

}

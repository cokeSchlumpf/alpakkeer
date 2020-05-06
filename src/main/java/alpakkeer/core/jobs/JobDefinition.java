package alpakkeer.core.jobs;

import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.jobs.monitor.CombinedJobMonitor;
import alpakkeer.core.values.Name;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface JobDefinition<P> {

   P getDefaultProperties();

   Name getName();

   Logger getLogger();

   List<ScheduleExecution<P>> getSchedule();

   CombinedJobMonitor<P> getMonitors();

   CompletionStage<JobHandle> run(String runId, P properties);

}

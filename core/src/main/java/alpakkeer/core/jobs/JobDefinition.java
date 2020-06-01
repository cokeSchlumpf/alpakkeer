package alpakkeer.core.jobs;

import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.jobs.monitor.JobMonitorGroup;
import alpakkeer.core.values.Name;
import org.slf4j.Logger;

import java.util.List;

public interface JobDefinition<P, C> extends JobRunner<P, C> {

   P getDefaultProperties();

   C getInitialContext();

   Name getName();

   Logger getLogger();

   List<ScheduleExecution<P>> getSchedule();

   JobMonitorGroup<P, C> getMonitors();

}

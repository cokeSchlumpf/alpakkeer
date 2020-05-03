package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor(staticName = "apply")
public class JobStatus<P> {

   String name;

   JobState state;

   List<QueuedExecution<P>> queued;

   List<ScheduledExecution<P>> schedule;

}

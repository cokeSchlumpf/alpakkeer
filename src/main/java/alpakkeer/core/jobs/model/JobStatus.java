package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor(staticName = "apply")
public class JobStatus<P, C> {

   String name;

   JobState state;

   C context;

   List<QueuedExecution<P>> queued;

   List<ScheduledExecution<P>> schedule;

}

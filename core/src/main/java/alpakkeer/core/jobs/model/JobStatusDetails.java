package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

import java.util.List;

@Value
@AllArgsConstructor(staticName = "apply")
public class JobStatusDetails<P, C> {

   String name;

   JobState state;

   C context;

   @With
   CurrentExecution<P> current;

   List<QueuedExecution<P>> queued;

   List<ScheduledExecution<P>> schedule;

   Object details;

}

package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class JobStatusDetails<P> {

   JobStatus<P> status;

   Object details;

}

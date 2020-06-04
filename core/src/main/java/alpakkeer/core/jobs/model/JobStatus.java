package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@AllArgsConstructor(staticName = "apply")
public class JobStatus {

   String name;

   JobState state;

   int queued;

}

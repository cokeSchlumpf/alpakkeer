package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@AllArgsConstructor(staticName = "apply")
public class CurrentExecution<P> {

   String id;

   P properties;

   LocalDateTime started;

}

package alpakkeer.core.jobs.actor.context;

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

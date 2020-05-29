package alpakkeer.core.jobs.actor.protocol;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Failed<P, C> implements Message<P, C> {

   Throwable exception;

}

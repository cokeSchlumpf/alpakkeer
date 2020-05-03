package alpakkeer.core.jobs.actor.protocol;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Failed<P> implements Message<P> {

   Throwable exception;

}

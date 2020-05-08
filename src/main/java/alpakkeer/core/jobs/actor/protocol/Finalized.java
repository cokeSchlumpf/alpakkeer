package alpakkeer.core.jobs.actor.protocol;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Finalized<P, C> implements Message<P, C> {

}

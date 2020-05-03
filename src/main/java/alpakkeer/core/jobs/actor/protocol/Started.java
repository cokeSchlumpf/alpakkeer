package alpakkeer.core.jobs.actor.protocol;

import alpakkeer.core.jobs.JobHandle;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Started<P> implements Message<P> {

   JobHandle handle;

}

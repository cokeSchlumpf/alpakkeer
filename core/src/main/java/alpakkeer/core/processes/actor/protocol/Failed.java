package alpakkeer.core.processes.actor.protocol;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class Failed implements Message {

   Throwable exception;

}

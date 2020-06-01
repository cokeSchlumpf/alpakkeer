package alpakkeer.core.processes.actor.protocol;

import alpakkeer.core.processes.ProcessHandle;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor(staticName = "apply")
public class Started implements Message {

   ProcessHandle handle;

}

package alpakkeer.core.processes.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor(staticName = "apply")
public class ProcessStatus {

   String name;

   ProcessState state;

   Instant nextRestart;

   public static ProcessStatus apply(String name, ProcessState state) {
      return apply(name, state, null);
   }

}

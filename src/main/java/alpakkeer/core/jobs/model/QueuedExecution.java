package alpakkeer.core.jobs.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@AllArgsConstructor(staticName = "apply")
public class QueuedExecution<P> {

   String id;

   P properties;

   public static <P> QueuedExecution<P> apply(P properties) {
      String id = UUID.randomUUID().toString();
      return apply(id, properties);
   }

}

package alpakkeer.core.jobs.actor.protocol;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Value
@AllArgsConstructor(staticName = "apply")
public class Completed<P, C> implements Message<P, C> {

   C result;

   public static <P, C> Completed<P, C> apply() {
      return apply(null);
   }

   public Optional<C> getResult() {
      return Optional.ofNullable(result);
   }

}

package alpakkeer.core.processes.actor.protocol;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class Completed implements Message {

   private static final Completed INSTANCE = new Completed();

   private Completed() {

   }

   public static Completed apply() {
      return INSTANCE;
   }

}

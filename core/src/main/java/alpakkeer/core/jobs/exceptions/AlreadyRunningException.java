package alpakkeer.core.jobs.exceptions;

import alpakkeer.core.values.Name;

public final class AlreadyRunningException extends RuntimeException {

   private AlreadyRunningException(Name name) {
      super(String.format("Job `%s` is already running.", name.getValue()));
   }

   public static AlreadyRunningException apply(Name name) {
      return new AlreadyRunningException(name);
   }

}

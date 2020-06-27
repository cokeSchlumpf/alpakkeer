package alpakkeer.core.jobs.exceptions;

public final class AlreadyRunningException extends RuntimeException {

   private AlreadyRunningException(String name) {
      super(String.format("Job `%s` is already running.", name));
   }

   public static AlreadyRunningException apply(String name) {
      return new AlreadyRunningException(name);
   }

}

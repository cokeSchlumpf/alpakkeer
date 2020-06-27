package alpakkeer.core.resources;

public final class JobAlreadyExistsException extends RuntimeException {

   private JobAlreadyExistsException(String message) {
      super(message);
   }

   public static JobAlreadyExistsException apply(String job) {
      String message = String.format("Job with name %s", job);
      return new JobAlreadyExistsException(message);
   }

}

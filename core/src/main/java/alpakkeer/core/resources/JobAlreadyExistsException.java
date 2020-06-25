package alpakkeer.core.resources;

import alpakkeer.core.values.Name;

public final class JobAlreadyExistsException extends RuntimeException {

   private JobAlreadyExistsException(String message) {
      super(message);
   }

   public static JobAlreadyExistsException apply(Name job) {
      String message = String.format("Job with name %s", job.getValue());
      return new JobAlreadyExistsException(message);
   }

}

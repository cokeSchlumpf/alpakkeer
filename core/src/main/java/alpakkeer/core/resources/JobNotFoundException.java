package alpakkeer.core.resources;

public final class JobNotFoundException extends RuntimeException {

   private JobNotFoundException(String message) {
      super(message);
   }

   public static JobNotFoundException apply(String job) {
      String message = String.format("No job with name `%s` found", job);
      return new JobNotFoundException(message);
   }

}

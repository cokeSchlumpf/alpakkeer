package alpakkeer.core.resources;

public final class ProcessAlreadyExistsException extends RuntimeException {

   private ProcessAlreadyExistsException(String message) {
      super(message);
   }

   public static ProcessAlreadyExistsException apply(String process) {
      String message = String.format("Process with name %s", process);
      return new ProcessAlreadyExistsException(message);
   }

}

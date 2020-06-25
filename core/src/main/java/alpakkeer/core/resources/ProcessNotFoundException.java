package alpakkeer.core.resources;

public class ProcessNotFoundException extends RuntimeException {

   private ProcessNotFoundException(String message) {
      super(message);
   }

   public static ProcessNotFoundException apply(String name) {
      var message = String.format("No process with name `%s` found", name);
      return new ProcessNotFoundException(message);
   }

}

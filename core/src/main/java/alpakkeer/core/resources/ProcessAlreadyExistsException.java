package alpakkeer.core.resources;

import alpakkeer.core.values.Name;

public class ProcessAlreadyExistsException extends RuntimeException {

   private ProcessAlreadyExistsException(String message) {
      super(message);
   }

   public static ProcessAlreadyExistsException apply(Name process) {
      String message = String.format("Process with name %s", process.getValue());
      return new ProcessAlreadyExistsException(message);
   }

}

package alpakkeer.core.processes.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProcessState {

   IDLE("idle"), FAILED("failed"), RUNNING("running"), STARTING("starting"), STOPPED("stopped"), STOPPING("stopping");

   @JsonValue
   private String name;

   ProcessState(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }

}

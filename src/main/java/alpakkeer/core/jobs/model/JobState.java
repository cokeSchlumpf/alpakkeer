package alpakkeer.core.jobs.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JobState {
   IDLE("idle"), STARTING("starting"), RUNNING("running"), STOPPING("stopping");

   @JsonValue
   private String name;

   JobState(String name) {
      this.name = name;
   }

   public String getName() {
      return name;
   }
}

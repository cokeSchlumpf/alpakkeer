package alpakkeer.core.processes;

import akka.Done;

import java.util.concurrent.CompletionStage;

public interface ProcessHandle {

   CompletionStage<Done> getCompletion();

   void stop();

}

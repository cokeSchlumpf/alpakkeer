package alpakkeer.core.processes;

import akka.Done;
import alpakkeer.core.processes.model.ProcessStatus;
import alpakkeer.core.processes.model.ProcessStatusDetails;

import java.util.concurrent.CompletionStage;

public interface Process {

   ProcessDefinition getDefinition();

   CompletionStage<Done> resume();

   CompletionStage<Done> pause();

   CompletionStage<ProcessStatus> getStatus();

   CompletionStage<ProcessStatusDetails> getStatusDetails();

}

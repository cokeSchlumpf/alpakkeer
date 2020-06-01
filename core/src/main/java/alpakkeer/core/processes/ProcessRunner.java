package alpakkeer.core.processes;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface ProcessRunner {

   CompletionStage<ProcessHandle> run(String executionId);

}

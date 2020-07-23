package alpakkeer.core.processes;

import alpakkeer.core.processes.monitor.ProcessMonitorGroup;
import io.javalin.Javalin;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public interface ProcessDefinition {

   void extendApi(Javalin api, Process processInstance);

   boolean isEnabled();

   boolean isInitiallyStarted();

   String getName();

   Duration getCompletionRestartBackoff();

   Duration getInitialRetryBackoff();

   Logger getLogger();

   ProcessMonitorGroup getMonitors();

   Duration getRetryBackoffResetTimeout();

   CompletionStage<ProcessHandle> run(String executionId);

}

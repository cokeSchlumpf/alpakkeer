package alpakkeer.core.processes;

import alpakkeer.core.processes.monitor.ProcessMonitorGroup;
import io.javalin.Javalin;
import org.slf4j.Logger;

import java.time.Duration;

public interface ProcessDefinition extends ProcessRunner {

   void extendApi(Javalin api, Process processInstance);

   boolean isInitiallyStarted();

   String getName();

   Duration getCompletionRestartBackoff();

   Duration getInitialRetryBackoff();

   Logger getLogger();

   ProcessMonitorGroup getMonitors();

   Duration getRetryBackoffResetTimeout();

}

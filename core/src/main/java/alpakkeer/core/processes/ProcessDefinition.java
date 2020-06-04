package alpakkeer.core.processes;

import alpakkeer.core.jobs.Job;
import alpakkeer.core.processes.monitor.ProcessMonitor;
import alpakkeer.core.processes.monitor.ProcessMonitorGroup;
import alpakkeer.core.values.Name;
import io.javalin.Javalin;
import org.slf4j.Logger;

import java.time.Duration;

public interface ProcessDefinition extends ProcessRunner {

   void extendApi(Javalin api, Process processInstance);

   boolean isInitiallyStarted();

   Name getName();

   Duration getCompletionRestartBackoff();

   Duration getInitialRetryBackoff();

   Logger getLogger();

   ProcessMonitorGroup getMonitors();

   Duration getRetryBackoffResetTimeout();

}

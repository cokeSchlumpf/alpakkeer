package alpakkeer.core.processes;

import alpakkeer.core.values.Name;
import org.slf4j.Logger;

import java.time.Duration;

public interface ProcessDefinition extends ProcessRunner {

   boolean isInitiallyStarted();

   Name getName();

   Logger getLogger();

   Duration getInitialRetryBackoff();

   Duration getCompletionRestartBackoff();

   Duration getRetryBackoffResetTimeout();

}

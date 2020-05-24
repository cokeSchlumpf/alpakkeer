package alpakkeer.core.monitoring;

import alpakkeer.config.RuntimeConfiguration;

public interface MetricsCollector extends MetricsMonitor {

   void run(RuntimeConfiguration runtime);

}

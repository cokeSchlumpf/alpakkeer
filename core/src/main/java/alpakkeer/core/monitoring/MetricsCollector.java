package alpakkeer.core.monitoring;

import alpakkeer.AlpakkeerRuntime;

public interface MetricsCollector extends MetricsMonitor {

   void run(AlpakkeerRuntime runtime);

}

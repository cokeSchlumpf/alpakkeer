package alpakkeer.core.monitoring;

import alpakkeer.javadsl.AlpakkeerRuntime;

public interface MetricsCollector extends MetricsMonitor {

   void run(AlpakkeerRuntime runtime);

}

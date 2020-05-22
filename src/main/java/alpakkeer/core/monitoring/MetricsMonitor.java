package alpakkeer.core.monitoring;

import java.util.List;

public interface MetricsMonitor {

   List<Metric<List<Marker>>> getMarkerMetrics();

   List<Metric<TimeSeries>> getTimeSeriesMetrics();

}

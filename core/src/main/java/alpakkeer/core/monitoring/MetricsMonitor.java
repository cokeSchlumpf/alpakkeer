package alpakkeer.core.monitoring;

import alpakkeer.core.monitoring.values.Marker;
import alpakkeer.core.monitoring.values.TimeSeries;

import java.util.List;

public interface MetricsMonitor {

   List<MetricStore<List<Marker>>> getMarkerMetrics();

   List<MetricStore<TimeSeries>> getTimeSeriesMetrics();

}

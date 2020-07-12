package alpakkeer.core.stream;

public interface StreamMonitor {

   void onStats(String executionId, String name, CheckpointMonitor.Stats statistics);

   void onStats(String executionId, String name, LatencyMonitor.Stats statistics);

}

package alpakkeer.core.monitoring;

import alpakkeer.core.monitoring.collectors.InMemoryMetricsCollector;
import alpakkeer.core.monitoring.collectors.LoggingMetricsCollector;
import alpakkeer.core.scheduler.model.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetricCollectors {

   private MetricCollectors() {

   }

   public static MetricsCollector createInMemory(CronExpression interval, int maxSize) {
      return InMemoryMetricsCollector.apply(interval, maxSize);
   }

   public static MetricsCollector createInMemory(CronExpression interval) {
      return createInMemory(interval, 500_000);
   }

   public static MetricsCollector createInMemory() {
      return createInMemory(CronExpression.everySeconds(30), 500_000);
   }

   public static MetricsCollector createLogging(CronExpression interval, Logger log) {
      return LoggingMetricsCollector.apply(log, interval);
   }

   public static MetricsCollector createLogging(CronExpression interval) {
      return LoggingMetricsCollector.apply(LoggerFactory.getLogger("alpakkeer.metrics"), interval);
   }

   public static MetricsCollector createLogging(Logger log) {
      return LoggingMetricsCollector.apply(log, CronExpression.everySeconds(30));
   }

   public static MetricsCollector createLogging() {
      return LoggingMetricsCollector.apply(LoggerFactory.getLogger("alpakkeer.metrics"), CronExpression.everySeconds(30));
   }

}

package alpakkeer.core.stream;

import alpakkeer.javadsl.AlpakkeerRuntime;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;
import org.slf4j.Logger;

public interface StreamBuilder {

   Logger getLogger();

   StreamMonitoringAdapter getMonitoring();

   StreamMessagingAdapter getMessaging();

   AlpakkeerRuntime getRuntime();

   StreamMonitoringAdapter monitoring();

   StreamMessagingAdapter messaging();

   AlpakkeerRuntime runtime();

   Logger logger();

}

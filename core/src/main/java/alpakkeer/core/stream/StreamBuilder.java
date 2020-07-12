package alpakkeer.core.stream;

import alpakkeer.AlpakkeerRuntime;
import alpakkeer.core.stream.messaging.StreamMessagingAdapter;

public interface StreamBuilder {

   StreamMonitoringAdapter getMonitoring();

   StreamMessagingAdapter getMessaging();

   AlpakkeerRuntime getRuntime();

   StreamMonitoringAdapter monitoring();

   StreamMessagingAdapter messaging();

   AlpakkeerRuntime runtime();

}

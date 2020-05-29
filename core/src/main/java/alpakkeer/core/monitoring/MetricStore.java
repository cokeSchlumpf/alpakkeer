package alpakkeer.core.monitoring;

import java.time.Instant;
import java.util.concurrent.CompletionStage;

public interface MetricStore<T> {

   String getName();

   String getDescription();

   CompletionStage<T> query(Instant from, Instant to);

}

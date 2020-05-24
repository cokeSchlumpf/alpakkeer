package alpakkeer.core.monitoring.values;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor(staticName = "apply")
public class DataPoint {

   Instant moment;

   double value;

}

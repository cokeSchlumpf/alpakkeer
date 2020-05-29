package alpakkeer.core.values.grafana;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public class Range {

   ZonedDateTime from;

   ZonedDateTime to;

   RangeRaw raw;

}

package alpakkeer.samples;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor(staticName = "apply")
public class IncrementalContext {

   LocalDateTime lastCrawled;

}

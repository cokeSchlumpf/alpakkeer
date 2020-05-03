package alpakkeer;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class About {

   String environment;

   String version;

}

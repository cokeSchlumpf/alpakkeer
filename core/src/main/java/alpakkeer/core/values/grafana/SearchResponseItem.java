package alpakkeer.core.values.grafana;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public class SearchResponseItem {

   String text;

   int value;

}

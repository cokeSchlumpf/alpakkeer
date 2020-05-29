package alpakkeer.core.values.grafana;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public class AnnotationQuery {

   String query;

   String name;

   String datasource;

   boolean enable;

   String iconColor;

}

package alpakkeer.core.values.grafana;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public class AnnotationRequest {

   Range range;

   RangeRaw rangeRaw;

   JsonNode annotation;

   JsonNode variables;

}

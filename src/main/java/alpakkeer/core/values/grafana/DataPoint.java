package alpakkeer.core.values.grafana;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Value
@AllArgsConstructor(staticName = "apply")
@JsonSerialize(using = DataPoint.DataPointSerializer.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public class DataPoint {

   Long timestamp;

   Double value;

   public static DataPoint apply(LocalDateTime dateTime, Double value) {
      return apply(dateTime.atZone(ZoneId.systemDefault()).toEpochSecond(), value);
   }

   public static class DataPointSerializer extends StdSerializer<DataPoint> {

      protected DataPointSerializer() {
         super(DataPoint.class);
      }

      @Override
      public void serialize(DataPoint value, JsonGenerator gen, SerializerProvider provider) throws IOException {
         gen.writeStartArray(2);
         gen.writeNumber(value.getValue());
         gen.writeNumber(value.getTimestamp());
         gen.writeEndArray();
      }
   }

}

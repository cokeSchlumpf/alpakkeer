package alpakkeer.core.scheduler.model;

import alpakkeer.core.util.Operators;
import alpakkeer.core.values.ValueClass;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.IOException;

@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(staticName = "apply")
@JsonSerialize(using = CronExpression.CronExpressionSerializer.class)
@JsonDeserialize(using = CronExpression.CronExpressionDeserializer.class)
public class CronExpression extends ValueClass<String> {

   String value;

   public static CronExpression everyMinute() {
      return apply("0 * * * * ?");
   }

   public static CronExpression everyDayAt(int hour) {
      return everyDayAt(hour, 0);
   }

   public static CronExpression everyDayAt(int hour, int minute) {
      Operators.require(hour >= 0 && hour <= 23, "hour must be between 0 and 23 (inclusive)");
      Operators.require(minute >= 0 && minute <= 59, "minute must be between 0 and 59 (inclusive)");
      return apply(String.format("0 %d %d ? * *", minute, hour));
   }

   public static CronExpression everyHours(int interval) {
      Operators.require(interval > 0 && interval < 24, "interval must be between 0 and 24 (exclusive)");
      return apply(String.format("0 0 0/%d * * ?", interval));
   }

   public static CronExpression everyMinutes(int interval) {
      Operators.require(interval > 0 && interval < 60, "interval must be between 0 and 60 (exclusive)");
      return apply(String.format("0 0/%d * * * ?", interval));
   }

   public static CronExpression everySeconds(int interval) {
      Operators.require(interval > 0 && interval < 60, "interval must be between 0 and 60 (exclusive)");
      return apply(String.format("0/%d * * * * ?", interval));
   }

   static class CronExpressionSerializer extends StdSerializer<CronExpression> {

      public CronExpressionSerializer() {
         super(CronExpression.class);
      }

      @Override
      public void serialize(CronExpression value, JsonGenerator gen, SerializerProvider provider) throws IOException {
         gen.writeString(value.getValue());
      }

   }

   static class CronExpressionDeserializer extends StdDeserializer<CronExpression> {

      public CronExpressionDeserializer() {
         super(CronExpression.class);
      }

      @Override
      public CronExpression deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
         return CronExpression.apply(p.readValueAs(String.class));
      }

   }

}

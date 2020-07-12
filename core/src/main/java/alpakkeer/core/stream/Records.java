package alpakkeer.core.stream;

import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.io.IOException;
import java.util.UUID;

public final class Records {

   @Value
   @AllArgsConstructor(staticName = "apply")
   @JsonSerialize(using = SimpleRecordSerializer.class)
   @JsonDeserialize(using = SimpleRecordDeserializer.class)
   public static class SimpleRecord<T> implements Record {

      String key;

      T value;

      Class<T> type;

   }

   public static class SimpleRecordSerializer extends StdSerializer<SimpleRecord> {

      protected SimpleRecordSerializer() {
         super(SimpleRecord.class);
      }

      @Override
      public void serialize(SimpleRecord value, JsonGenerator gen, SerializerProvider provider) throws IOException {
         gen.writeStartObject();
         gen.writeStringField("key", value.key);
         gen.writeStringField("type", value.type.getName());
         gen.writeObjectField("value", value.getValue());
         gen.writeEndObject();
      }

   }

   public static class SimpleRecordDeserializer extends StdDeserializer<SimpleRecord> {

      protected SimpleRecordDeserializer() {
         super(SimpleRecord.class);
      }

      @Override
      @SuppressWarnings("unchecked")
      public SimpleRecord deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
         JsonNode node = p.getCodec().readTree(p);
         var key = node.get("key").asText();
         var type = Operators.suppressExceptions(() -> Class.forName(node.get("type").asText()));
         var value = node.get("value").traverse(p.getCodec()).readValueAs(type);

         return SimpleRecord.apply(key, value, (Class<Object>) type);
      }

   }

   /**
    * Creates a new
    * @param value
    * @param <T>
    * @return
    */
   public static <T> SimpleRecord<T> apply(T value) {
      return apply(value, UUID.randomUUID().toString());
   }

   @SuppressWarnings("unchecked")
   public static <T> SimpleRecord<T> apply(T value, String key) {
      return SimpleRecord.apply(key, value, (Class<T>) value.getClass());
   }

}

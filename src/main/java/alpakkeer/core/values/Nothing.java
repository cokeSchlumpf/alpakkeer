package alpakkeer.core.values;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JsonSerialize(using = Nothing.NothingSerializer.class)
@JsonDeserialize(using = Nothing.NothingDeserializer.class)
public final class Nothing {

   private static final Nothing INSTANCE = new Nothing();

   private Nothing() {

   }

   public static Nothing getInstance() {
      return INSTANCE;
   }

   static class NothingSerializer extends StdSerializer<Nothing> {

      public NothingSerializer() {
         super(Nothing.class);
      }

      @Override
      public void serialize(Nothing value, JsonGenerator gen, SerializerProvider provider) throws IOException {
         gen.writeString("<no properties>");
      }

   }

   static class NothingDeserializer extends StdDeserializer<Nothing> {

      public NothingDeserializer() {
         super(Nothing.class);
      }

      @Override
      public Nothing deserialize(JsonParser p, DeserializationContext ctxt) {
         return Nothing.getInstance();
      }

   }

}

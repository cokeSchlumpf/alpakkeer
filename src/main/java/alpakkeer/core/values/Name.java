package alpakkeer.core.values;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.IOException;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonSerialize(using = Name.NameSerializer.class)
@JsonDeserialize(using = Name.NameDeserializer.class)
public class Name extends ValueClass<String> {

   private static final Pattern pattern = Pattern.compile("[-a-z0-9]+");

   String value;

   public static Name apply(String name) {
      if (!pattern.matcher(name).matches()) {
         throw new IllegalArgumentException(String.format("`%s` is not a valid name", name));
      }

      return new Name(name);
   }

   static class NameSerializer extends StdSerializer<Name> {

      public NameSerializer() {
         super(Name.class);
      }

      @Override
      public void serialize(Name value, JsonGenerator gen, SerializerProvider provider) throws IOException {
         gen.writeString(value.getValue());
      }

   }

   static class NameDeserializer extends StdDeserializer<Name> {

      public NameDeserializer() {
         super(Name.class);
      }

      @Override
      public Name deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
         return Name.apply(p.readValueAs(String.class));
      }

   }

}


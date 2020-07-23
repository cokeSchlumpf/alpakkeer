package alpakkeer.core.stream;

import alpakkeer.core.stream.context.NoRecordContext;
import alpakkeer.core.stream.context.RecordContext;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.util.UUID;

@Value
@JsonSerialize(using = Record.RecordSerializer.class)
@JsonDeserialize(using = Record.RecordDeserializer.class)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Record<V, C extends RecordContext> {

   @JsonIgnore
   @XmlTransient
   transient C context;

   String key;

   V value;

   Class<C> contextType;

   Class<V> valueType;

   public static <V, C extends RecordContext> Record<V, C> apply(
      V value, String key, C context, Class<V> valueVype, Class<C> contextVype) {

      return new Record<>(context, key, value, contextVype, valueVype);
   }

   @SuppressWarnings("unchecked")
   public static <V, C extends RecordContext> Record<V, C> apply(
      V value, String key, C context) {

      return apply(value, key, context, (Class<V>) value.getClass(), (Class<C>) context.getClass());
   }

   @SuppressWarnings("unchecked")
   public static <V> Record<V, NoRecordContext> apply(V value, String key) {
      return apply(value, key, NoRecordContext.INSTANCE, (Class<V>) value.getClass(), NoRecordContext.class);
   }

   public static <V> Record<V, NoRecordContext> apply(V value) {
      return apply(value, UUID.randomUUID().toString());
   }

   public <T extends RecordContext> Record<V, T> withContext(T context, Class<T> contextType) {
      return Record.apply(value, key, context, valueType, contextType);
   }

   @SuppressWarnings("unchecked")
   public <T extends RecordContext> Record<V, T> withContext(T context) {
      return withContext(context, (Class<T>) context.getClass());
   }

   public <T> Record<T, C> withValue(T value, Class<T> recordType) {
      return Record.apply(value, key, context, recordType, contextType);
   }

   @SuppressWarnings("unchecked")
   public <T> Record<T, C> withValue(T record) {
      return withValue(record, (Class<T>) record.getClass());
   }

   public static class RecordSerializer extends StdSerializer<Record> {

      protected RecordSerializer() {
         super(Record.class);
      }

      @Override
      public void serialize(Record value, JsonGenerator gen, SerializerProvider provider) throws IOException {
         gen.writeStartObject();
         gen.writeStringField("key", value.key);
         gen.writeStringField("type", value.valueType.getName());
         gen.writeObjectField("value", value.value);
         gen.writeEndObject();
      }

   }

   public static class RecordDeserializer extends StdDeserializer<Record> {

      protected RecordDeserializer() {
         super(Record.class);
      }

      @Override
      @SuppressWarnings("unchecked")
      public Record deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
         JsonNode node = p.getCodec().readTree(p);
         var key = node.get("key").asText();
         var type = Operators.suppressExceptions(() -> Class.forName(node.get("type").asText()));
         var value = node.get("value").traverse(p.getCodec()).readValueAs(type);

         return Record.apply(value, key, NoRecordContext.INSTANCE, (Class<Object>) type, NoRecordContext.class);
      }

   }

}

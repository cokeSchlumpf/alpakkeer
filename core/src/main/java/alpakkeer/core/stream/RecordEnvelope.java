package alpakkeer.core.stream;

import alpakkeer.core.stream.context.NoRecordContext;
import alpakkeer.core.stream.context.RecordContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.xml.bind.annotation.XmlTransient;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecordEnvelope<R extends Record, C extends RecordContext> {

   @JsonIgnore
   @XmlTransient
   transient C context;

   R record;

   Class<C> contextType;

   Class<R> recordType;

   public static <R extends Record, C extends RecordContext> RecordEnvelope<R, C> apply(R record, C context, Class<C> contextType, Class<R> recordType) {
      return new RecordEnvelope<>(context, record, contextType, recordType);
   }

   @SuppressWarnings("unchecked")
   public static <R extends Record, C extends RecordContext> RecordEnvelope<R, C> apply(R record, C context) {
      return apply(record, context, (Class<C>) context.getClass(), (Class<R>) record.getClass());
   }

   @SuppressWarnings("unchecked")
   public static <R extends Record> RecordEnvelope<R, NoRecordContext> apply(R record) {
      return apply(record, NoRecordContext.INSTANCE, NoRecordContext.class, (Class<R>) record.getClass());
   }

   public <T extends Record> RecordEnvelope<T, C> withRecord(T record, Class<T> recordType) {
      return RecordEnvelope.apply(record, context, contextType, recordType);
   }

   @SuppressWarnings("unchecked")
   public <T extends Record> RecordEnvelope<T, C> withRecord(T record) {
      return withRecord(record, (Class<T>) record.getClass());
   }

}

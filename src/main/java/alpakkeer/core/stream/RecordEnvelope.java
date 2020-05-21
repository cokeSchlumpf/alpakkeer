package alpakkeer.core.stream;

import alpakkeer.core.stream.context.RecordContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.xml.bind.annotation.XmlTransient;

@Getter
@AllArgsConstructor(staticName = "apply")
public class RecordEnvelope<R, C extends RecordContext> {

   @JsonIgnore
   @XmlTransient
   private transient final C context;

   private final R record;

   private final Class<R> recordType;

   public <T> RecordEnvelope<T, C> withRecord(T record, Class<T> recordType) {
      return RecordEnvelope.apply(context, record, recordType);
   }

   @SuppressWarnings("unchecked")
   public <T> RecordEnvelope<T, C> withRecord(T record) {
      return withRecord(record, (Class<T>) record.getClass());
   }

}

package alpakkeer.core.jobs.context;

import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.NoRecordContext;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "apply")
public final class ContextMapper {

   private final ObjectMapper om;

   public <C> String toString(C context) {
      return Operators.suppressExceptions(() -> om.writeValueAsString(Record.apply(context)));
   }

   @SuppressWarnings("unchecked")
   public <C> C fromString(String context) {
      return Operators
         .suppressExceptions(() -> (Record<C, NoRecordContext>) om.readValue(context, Record.class))
         .getValue();
   }

}

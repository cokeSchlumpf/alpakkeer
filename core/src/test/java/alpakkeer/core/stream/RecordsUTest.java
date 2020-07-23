package alpakkeer.core.stream;

import alpakkeer.core.util.ObjectMapperFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.junit.Test;

public class RecordsUTest {

   @Value
   @NoArgsConstructor(force = true)
   @AllArgsConstructor(staticName = "apply")
   private static class Bla {

      String foo;

      int bar;

   }

   @Test
   @SuppressWarnings("unchecked")
   public void testSerialization() throws JsonProcessingException {
      var record = Record.apply(Bla.apply("foo", 123), "123");
      var om = ObjectMapperFactory.apply().create(true);
      var json = om.writeValueAsString(record);

      System.out.println(json);

      var recordRead = om.readValue(json, Record.class);
      assert(record.equals(recordRead));
   }

}

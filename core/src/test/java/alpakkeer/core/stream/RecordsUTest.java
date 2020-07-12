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
      var record = Records.apply(Bla.apply("foo", 123), "123");
      var om = ObjectMapperFactory.apply().create(true);
      var json = om.writeValueAsString(record);

      System.out.println(json);

      var recordRead = (Records.SimpleRecord<Bla>) om.readValue(json, Records.SimpleRecord.class);
      assert(record.equals(recordRead));
   }

   @Test
   public void testRecordEnvelopeSerialization() throws JsonProcessingException {
      var record = Records.apply(Bla.apply("foo", 123), "123");
      var env = RecordEnvelope.apply(record);
      var om = ObjectMapperFactory.apply().create(true);
      var json = om.writeValueAsString(env);

      System.out.println(json);

      var envRead = om.readValue(json, RecordEnvelope.class);
      assert(env.equals(envRead));
   }

}

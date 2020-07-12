package alpakkeer.core.stream.context;

public final class RecordContexts {

   private RecordContexts() {

   }

   public static NoRecordContext none() {
      return NoRecordContext.INSTANCE;
   }

}

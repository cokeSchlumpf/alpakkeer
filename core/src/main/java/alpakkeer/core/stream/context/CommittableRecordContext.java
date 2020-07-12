package alpakkeer.core.stream.context;

import akka.Done;

import java.util.concurrent.CompletionStage;

public interface CommittableRecordContext extends RecordContext {

   CompletionStage<Done> commit();

}

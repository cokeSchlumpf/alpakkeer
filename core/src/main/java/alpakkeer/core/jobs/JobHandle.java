package alpakkeer.core.jobs;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface JobHandle<C> {

   CompletionStage<Optional<C>> getCompletion();

   void stop();

}

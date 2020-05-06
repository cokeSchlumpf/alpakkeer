package alpakkeer.core.jobs;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface JobHandle<C> {

   CompletionStage<C> getCompletion();

   CompletionStage<Optional<C>> stop();

}

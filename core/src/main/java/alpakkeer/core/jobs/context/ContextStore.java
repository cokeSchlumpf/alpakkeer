package alpakkeer.core.jobs.context;

import akka.Done;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ContextStore {

   <C> CompletionStage<Done> saveContext(String name, C context);

   <C> CompletionStage<Optional<C>> readLatestContext(String name);

}

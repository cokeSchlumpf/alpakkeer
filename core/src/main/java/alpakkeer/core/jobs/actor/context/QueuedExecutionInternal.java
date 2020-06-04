package alpakkeer.core.jobs.actor.context;

import alpakkeer.core.jobs.model.QueuedExecution;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.concurrent.CompletableFuture;

@Value
@AllArgsConstructor(staticName = "apply")
public class QueuedExecutionInternal<P, C> {

   QueuedExecution<P> queuedExecution;

   CompletableFuture<C> maybeResult;

}

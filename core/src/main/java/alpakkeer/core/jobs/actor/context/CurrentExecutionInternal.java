package alpakkeer.core.jobs.actor.context;

import alpakkeer.core.jobs.model.CurrentExecution;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.concurrent.CompletableFuture;

@Value
@AllArgsConstructor(staticName = "apply")
public class CurrentExecutionInternal<P, C> {

   CurrentExecution<P> currentExecution;

   CompletableFuture<C> completableFuture;

}

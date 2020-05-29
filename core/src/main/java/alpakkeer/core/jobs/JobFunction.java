package alpakkeer.core.jobs;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface JobFunction<P, C> {

   CompletionStage<JobHandle<C>> run(String executionId, P properties, C context);

}

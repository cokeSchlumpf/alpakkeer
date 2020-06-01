package alpakkeer.core.jobs;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface JobRunner<P, C> {

   CompletionStage<JobHandle<C>> run(String executionId, P properties, C context);

}

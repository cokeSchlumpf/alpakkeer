package alpakkeer.core.jobs;

import akka.Done;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public final class JobHandles {

   private static Logger LOG = LoggerFactory.getLogger(JobHandles.class);

   private JobHandles() {

   }

   @AllArgsConstructor(staticName = "apply")
   private static class SimpleJobHandle implements JobHandle {

      CompletionStage<?> done;

      @Override
      public CompletionStage<Done> getCompletion() {
         return done.thenApply(i -> Done.getInstance());
      }

      @Override
      public CompletionStage<Done> stop() {
         LOG.warn("Attempt to stop simple job - Simple jobs cannot be requested to stop; will continue processing.");
         return done.thenApply(i -> Done.getInstance());
      }

   }

   public static JobHandle create(CompletionStage<?> run) {
      return SimpleJobHandle.apply(run);
   }

   public static JobHandle runAsync(Runnable run, Executor executor) {
      return SimpleJobHandle.apply(CompletableFuture.runAsync(run, executor).thenApply(ignore -> Done.getInstance()));
   }

   public static JobHandle runAsync(Runnable run) {
      return runAsync(run, ForkJoinPool.commonPool());
   }

}

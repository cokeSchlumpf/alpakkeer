package alpakkeer.core.jobs;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public final class JobHandles {

   private static Logger LOG = LoggerFactory.getLogger(JobHandles.class);

   private JobHandles() {

   }

   @AllArgsConstructor(staticName = "apply")
   private static class SimpleJobHandle<C> implements JobHandle<C> {

      CompletionStage<C> done;

      @Override
      public CompletionStage<C> getCompletion() {
         return done;
      }

      @Override
      public CompletionStage<Optional<C>> stop() {
         LOG.warn("Attempt to stop simple job - Simple jobs cannot be requested to stop; will continue processing.");
         return done.thenApply(Optional::of);
      }

   }

   public static <C> JobHandle<C> create(CompletionStage<C> run) {
      return SimpleJobHandle.apply(run);
   }

}

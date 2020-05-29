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

      CompletionStage<Optional<C>> done;

      @Override
      public CompletionStage<Optional<C>> getCompletion() {
         return done;
      }

      @Override
      public void stop() {
         LOG.warn("Attempt to stop simple job - Simple jobs cannot be requested to stop; will continue processing.");
      }

   }

   public static <C> JobHandle<C> create(CompletionStage<C> run) {
      return SimpleJobHandle.apply(run.thenApply(Optional::of));
   }

   public static <C> JobHandle<C> createFromOptional(CompletionStage<Optional<C>> run) {
      return SimpleJobHandle.apply(run);
   }



}

package alpakkeer.core.processes;

import akka.Done;
import akka.japi.Pair;
import akka.stream.UniqueKillSwitch;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public final class ProcessHandles {

   private static final Logger LOG = LoggerFactory.getLogger(ProcessHandles.class);

   private ProcessHandles() {

   }

   @AllArgsConstructor(staticName = "apply")
   private static class SimpleHandle implements ProcessHandle {

      private final CompletionStage<?> done;

      @Override
      public CompletionStage<Done> getCompletion() {
         return done.thenApply(i -> Done.getInstance());
      }

      @Override
      public void stop() {
         LOG.warn("Attempt to stop simple process - Simple processes cannot be requested to stop; will continue processing.");
      }

   }

   @AllArgsConstructor(staticName = "apply")
   private static class KillSwitchHandle implements ProcessHandle {

      private final CompletionStage<?> done;

      private final UniqueKillSwitch killSwitch;

      @Override
      public CompletionStage<Done> getCompletion() {
         return done.thenApply(i -> Done.getInstance());
      }

      @Override
      public void stop() {
         killSwitch.shutdown();
      }

   }

   public static ProcessHandle createFromCS(CompletionStage<?> process) {
      return SimpleHandle.apply(process);
   }

   public static ProcessHandle createFromCancellableGraph(Pair<UniqueKillSwitch, CompletionStage<Done>> mat) {
      return KillSwitchHandle.apply(mat.second(), mat.first());
   }

}

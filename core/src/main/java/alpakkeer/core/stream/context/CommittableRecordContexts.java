package alpakkeer.core.stream.context;

import akka.Done;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class CommittableRecordContexts {

   private CommittableRecordContexts() {

   }

   @AllArgsConstructor(staticName = "apply")
   private static class SimpleCommittableRecordContext implements CommittableRecordContext {

      private final Supplier<CompletionStage<Done>> commit;

      @Override
      public CompletionStage<Done> commit() {
         return commit.get();
      }

   }

   public static CommittableRecordContext create(Supplier<CompletionStage<Done>> commit) {
      return SimpleCommittableRecordContext.apply(commit);
   }

   public static CommittableRecordContext createFromRunnable(Runnable run) {
      return SimpleCommittableRecordContext.apply(() -> {
         run.run();
         return CompletableFuture.completedFuture(Done.getInstance());
      });
   }

}

package alpakkeer.core.jobs;

import akka.Done;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class ContextStores {

   public ContextStore create() {
      return new ContextStore() {
         private HashMap<String, Object> store = Maps.newHashMap();

         @Override
         public <C> CompletionStage<Done> saveContext(String name, C context) {
            store.put(name, context);
            return CompletableFuture.completedFuture(Done.getInstance());
         }

         @Override
         @SuppressWarnings("unchecked")
         public <C> CompletionStage<Optional<C>> readLatestContext(String name) {
            if (store.containsKey(name)) {
               return CompletableFuture.completedFuture(Optional.of((C) store.get(name)));
            } else {
               return CompletableFuture.completedFuture(Optional.empty());
            }
         }
      };
   }

}

package alpakkeer.core.jobs.context;

import akka.Done;
import alpakkeer.javadsl.AlpakkeerBaseRuntime;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class  ContextStores {

   private ContextStores() {

   }

   public static ContextStore createFromConfiguration(AlpakkeerBaseRuntime runtime) {
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

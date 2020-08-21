package alpakkeer.core.jobs.context;

import akka.Done;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InMemoryContextStore implements ContextStore {

   private final ObjectMapper om;

   private final HashMap<String, String> store;

   public static InMemoryContextStore apply(ObjectMapper om) {
      return new InMemoryContextStore(om, Maps.newHashMap());
   }

   @Override
   public <C> CompletionStage<Done> saveContext(String name, C context) {
      store.put(name, Operators.suppressExceptions(() -> om.writeValueAsString(context)));
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

}

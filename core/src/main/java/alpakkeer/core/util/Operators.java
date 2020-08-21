package alpakkeer.core.util;

import akka.japi.function.Function2;
import akka.japi.function.Function3;
import com.google.common.hash.Hashing;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public final class Operators {

   private Operators() {

   }

   public static <T1, T2, R> CompletionStage<R> compose(
      CompletionStage<T1> cs1, CompletionStage<T2> cs2, Function2<T1, T2, R> combineWith) {
      CompletableFuture<T1> f1 = cs1.toCompletableFuture();
      CompletableFuture<T2> f2 = cs2.toCompletableFuture();

      return CompletableFuture
         .allOf(f1, f2)
         .thenApply(v -> Operators.suppressExceptions(() -> combineWith.apply(f1.join(), f2.join())));
   }

   public static <T1, T2, T3, R> CompletionStage<R> compose(
      CompletionStage<T1> cs1, CompletionStage<T2> cs2, CompletionStage<T3> cs3, Function3<T1, T2, T3, R> combineWith) {
      CompletableFuture<T1> f1 = cs1.toCompletableFuture();
      CompletableFuture<T2> f2 = cs2.toCompletableFuture();
      CompletableFuture<T3> f3 = cs3.toCompletableFuture();

      return CompletableFuture
         .allOf(f1, f2, f3)
         .thenApply(v -> Operators.suppressExceptions(() -> combineWith.apply(f1.join(), f2.join(), f3.join())));
   }

   public static <T> CompletionStage<List<T>> allOf(List<CompletionStage<T>> futures) {
      if (futures.isEmpty()) {
         return CompletableFuture.completedFuture(List.of());
      }

      AtomicReference<List<T>> results = new AtomicReference<>(new ArrayList<>());
      CompletableFuture<List<T>> result = new CompletableFuture<>();

      futures.forEach(f -> {
         f.thenAccept(r -> results.getAndUpdate(currentResults -> {
             currentResults.add(r);

             if (currentResults.size() == futures.size()) {
                 result.complete(currentResults);
             }

             return currentResults;
         }));

         f.exceptionally(ex -> {
             result.completeExceptionally(ex);
             return null;
         });
      });

      return result;
   }

   public static <T, E extends Exception> CompletionStage<T> completeExceptionally(E with) {
      CompletableFuture<T> result = new CompletableFuture<>();
      result.completeExceptionally(with);
      return result;
   }

   public static <T> CompletionStage<T> completeExceptionally() {
      CompletableFuture<T> result = new CompletableFuture<>();
      result.completeExceptionally(new RuntimeException());
      return result;
   }

   public static <T> Optional<T> exceptionToNone(ExceptionalSupplier<T> supplier) {
      try {
         return Optional.of(supplier.get());
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   public static boolean isCause(Class<? extends Throwable> expected, Throwable exc) {
      return expected.isInstance(exc) || (
         exc != null && isCause(expected, exc.getCause())
      );
   }

   public static String hash() {
      return Hashing
         .goodFastHash(8)
         .newHasher()
         .putLong(System.currentTimeMillis())
         .putString(UUID.randomUUID().toString(), StandardCharsets.UTF_8)
         .hash()
         .toString();
   }

   @SuppressWarnings("unchecked")
   public static <T> Optional<T> hasCause(Throwable t, Class<T> exType) {
      if (exType.isInstance(t)) {
         return Optional.of((T) t);
      } else if (t.getCause() != null) {
         return hasCause(t.getCause(), exType);
      } else {
         return Optional.empty();
      }
   }

   public static String extractMessage(Throwable ex) {
      return Optional
         .ofNullable(ExceptionUtils.getRootCause(ex))
         .map(t -> String.format("%s: %s", t.getClass().getSimpleName(), t.getMessage()))
         .orElse(Optional
            .ofNullable(ex.getMessage())
            .map(str -> String.format("%s: %s", ex.getClass().getSimpleName(), ex.getMessage()))
            .orElse(String.format("%s: No details provided.", ex.getClass().getSimpleName())));
   }

   public static void ignoreExceptions(ExceptionalRunnable runnable, Logger log) {
      try {
         runnable.run();
      } catch (Exception e) {
         if (log != null) {
            log.warn("An exception occurred but will be ignored", e);
         }
      }
   }

   public static void ignoreExceptions(ExceptionalRunnable runnable) {
      ignoreExceptions(runnable, null);
   }

   public static <T> T ignoreExceptionsWithDefault(ExceptionalSupplier<T> supplier, T defaultValue, Logger log) {
      try {
         return supplier.get();
      } catch (Exception e) {
         if (log != null) {
            log.warn("An exception occurred but will be ignored", e);
         }

         return defaultValue;
      }
   }

   public static <T> T ignoreExceptionsWithDefault(ExceptionalSupplier<T> supplier, T defaultValue) {
      return ignoreExceptionsWithDefault(supplier, defaultValue, null);
   }

   public static void require(boolean condition, String message, Object... args) {
      if (!condition) {
         throw new IllegalArgumentException(String.format(message, args));
      }
   }

   public static void require(boolean condition) {
      require(condition, "pre-conditions not met");
   }

   public static void suppressExceptions(ExceptionalRunnable runnable) {
      try {
         runnable.run();
      } catch (Exception e) {
         ExceptionUtils.wrapAndThrow(e);
      }
   }

   public static void suppressExceptions(ExceptionalRunnable runnable, String message) {
      try {
         runnable.run();
      } catch (Exception e) {
         throw new RuntimeException(message, e);
      }
   }

   public static <T> T suppressExceptions(ExceptionalSupplier<T> supplier) {
      try {
         return supplier.get();
      } catch (Exception e) {
         return ExceptionUtils.wrapAndThrow(e);
      }
   }

   public static <T> T suppressExceptions(ExceptionalSupplier<T> supplier, String message) {
      try {
         return supplier.get();
      } catch (Exception e) {
         throw new RuntimeException(message, e);
      }
   }

   @FunctionalInterface
   public interface ExceptionalRunnable {

      void run() throws Exception;

   }

   @FunctionalInterface
   public interface ExceptionalConsumer<T> {

      void accept(T param) throws Exception;

   }

   @FunctionalInterface
   public interface ExceptionalSupplier<T> {

      T get() throws Exception;

   }

   @FunctionalInterface
   public interface ExceptionalFunction<I, R> {

      R apply(I in) throws Exception;

   }

}

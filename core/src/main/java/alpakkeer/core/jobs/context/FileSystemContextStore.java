package alpakkeer.core.jobs.context;

import akka.Done;
import alpakkeer.config.FileSystemContextStoreConfiguration;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.NoRecordContext;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileSystemContextStore implements ContextStore {

   private final Path directory;

   private final ObjectMapper om;

   public static FileSystemContextStore apply(Path directory, ObjectMapper om) {
      Operators.suppressExceptions(() -> Files.createDirectories(directory));
      return new FileSystemContextStore(directory, om);
   }

   public static FileSystemContextStore apply(FileSystemContextStoreConfiguration config, ObjectMapper om) {
      return apply(new File(config.getDirectory()).toPath(), om);
   }

   @Override
   public <C> CompletionStage<Done> saveContext(String name, C context) {
      Operators.suppressExceptions(() -> {
         var filename = name + ".json";

         try (OutputStream os = Files.newOutputStream(directory.resolve(filename))) {
            var record = Record.apply(context);
            om.writeValue(os, record);
         }
      });

      return CompletableFuture.completedFuture(Done.getInstance());
   }

   @Override
   @SuppressWarnings("unchecked")
   public <C> CompletionStage<Optional<C>> readLatestContext(String name) {
      return CompletableFuture.completedFuture(
         Operators.suppressExceptions(() -> {
            var file = directory.resolve(name + ".json");

            if (Files.exists(file)) {
               try (InputStream is = Files.newInputStream(file)) {
                  var record = (Record<C, NoRecordContext>) om.readValue(is, Record.class);
                  return Optional.of(record.getValue());
               }
            } else {
               return Optional.empty();
            }
         }));
   }

}

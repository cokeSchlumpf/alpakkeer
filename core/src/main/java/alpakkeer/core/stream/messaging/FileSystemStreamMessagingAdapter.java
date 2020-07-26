package alpakkeer.core.stream.messaging;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.alpakka.file.DirectoryChange;
import akka.stream.alpakka.file.javadsl.Directory;
import akka.stream.alpakka.file.javadsl.DirectoryChangesSource;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.config.FileSystemStreamMessagingConfiguration;
import alpakkeer.core.stream.Record;
import alpakkeer.core.stream.context.CommittableRecordContext;
import alpakkeer.core.stream.context.CommittableRecordContexts;
import alpakkeer.core.stream.context.NoRecordContext;
import alpakkeer.core.stream.context.RecordContext;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class FileSystemStreamMessagingAdapter implements StreamMessagingAdapter {

   private static Logger LOG = LoggerFactory.getLogger(FileSystemStreamMessagingAdapter.class);

   private final Path workingDirectory;

   private final ObjectMapper om;

   public static FileSystemStreamMessagingAdapter apply(ObjectMapper om, FileSystemStreamMessagingConfiguration config) {
      return apply(Path.of(config.getDirectory()), om);
   }

   private Path getDirectory(String topic) {
      var dir = workingDirectory.resolve(topic);
      Operators.suppressExceptions(() -> Files.createDirectories(dir));
      return dir;
   }

   private CompletionStage<Done> putDocument$internal(String topic, Record<?, ?> record) {
      return Operators.suppressExceptions(() -> {
         var filename = record.getKey() + ".json";

         try (OutputStream os = Files.newOutputStream(getDirectory(topic).resolve(filename))) {
            om.writeValue(os, record);
         }

         if (record.getContext() instanceof CommittableRecordContext) {
            return ((CommittableRecordContext) record.getContext()).commit();
         } else {
            return CompletableFuture.completedFuture(Done.getInstance());
         }
      });
   }

   @SuppressWarnings("unchecked")
   private <R> Record<R, CommittableRecordContext> getDocument$internal(Path path) {
      return Operators.suppressExceptions(() -> {
         try (InputStream is = Files.newInputStream(path)) {
            var context = CommittableRecordContexts.createFromRunnable(() -> Operators.ignoreExceptions(() -> Files.delete(path), LOG));
            var record = (Record<R, NoRecordContext>) om.readValue(is, Record.class);

            return record.withContext(context);
         }
      });
   }

   @Override
   public <R, C extends RecordContext> CompletionStage<Done> putRecord(String topic, Record<R, C> record) {
      return putDocument$internal(topic, record);
   }

   @Override
   public <R, C extends RecordContext> Sink<Record<R, C>, CompletionStage<Done>> recordsSink(String topic) {
      return Flow
         .<Record<R, C>>create()
         .toMat(Sink.foreach(record -> putDocument$internal(topic, record)), Keep.right());
   }

   @Override
   public <T> CompletionStage<Optional<Record<T, CommittableRecordContext>>> getNextRecord(String topic, Class<T> recordType, String consumerGroup) {
      // TODO: Use consumer id!
      return CompletableFuture.completedFuture(Operators.suppressExceptions(() -> Files
         .list(getDirectory(topic))
         .map(this::<T>getDocument$internal)
         .findFirst()));
   }

   @Override
   public <T> Source<Record<T, CommittableRecordContext>, NotUsed> recordsSource(String topic, Class<T> recordType, String consumerGroup) {
      // TODO: Use consumer id!
      var dir = getDirectory(topic);

      return DirectoryChangesSource
         .create(dir, Duration.ofSeconds(30), 1024)
         .filter(pair -> pair.second().equals(DirectoryChange.Creation) || pair.second().equals(DirectoryChange.Modification))
         .map(Pair::first)
         .prepend(Directory.ls(dir))
         .map(this::getDocument$internal);
   }

}

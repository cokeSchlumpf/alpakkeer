package alpakkeer.samples;

import akka.Done;
import akka.stream.alpakka.csv.javadsl.CsvParsing;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import alpakkeer.core.stream.CheckpointMonitors;
import alpakkeer.core.stream.StreamBuilder;
import alpakkeer.core.util.DateTimes;
import com.google.common.collect.Maps;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SampleStreams {

   private SampleStreams() {

   }

   public static RunnableGraph<CompletionStage<Done>> checkpointMonitorSample() {
      return Source
         .range(0, Integer.MAX_VALUE)
         .takeWithin(Duration.ofMinutes(1))
         .throttle(42, Duration.ofMillis(320))
         .via(CheckpointMonitors.create(Duration.ofSeconds(10), System.out::println))
         .toMat(Sink.ignore(), Keep.right());
   }

   public static RunnableGraph<CompletionStage<Done>> twitter(StreamBuilder sb) {
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
      Pattern pattern = Pattern.compile("(?:\\s|\\A|^)[#]+([A-Za-z0-9-_]+)");

      var processTweets = Flow.<Collection<ByteString>>create()
         .map(c -> c.stream().map(bs -> bs.decodeString(StandardCharsets.UTF_8)).collect(Collectors.toList()))
         .map(cols -> {
            var id = cols.get(1);
            var date = ZonedDateTime.parse(cols.get(2), fmt);
            var user = cols.get(4);
            var text = cols.get(5).toLowerCase();

            return Tweet.apply(id, DateTimes.ofZonedDateTime(date).toLocalDateTime(), user, text);
         })
         .map(tweet -> pattern
            .matcher(tweet.getText())
            .results()
            .map(MatchResult::group)
            .collect(Collectors.toList()))
         .filter(list -> !list.isEmpty());

      return FileIO
         .fromPath(new File("./src/main/resources/tweets.csv").toPath())
         .via(CsvParsing.lineScanner())
         .via(sb.createCheckpointMonitor("tweet-count", Duration.ofMillis(500)))
         .via(sb.createLatencyMonitor("tweet-processing", processTweets, Duration.ofSeconds(1)))
         .fold(Maps.<String, Integer>newHashMap(), (collected, tags) -> {
            tags.forEach(tag -> {
               if (collected.containsKey(tag)) {
                  collected.put(tag, collected.get(tag) + 1);
               } else {
                  collected.put(tag, 1);
               }
            });

            return collected;
         })
         .map(map -> map.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(10).collect(Collectors.toList()))
         .toMat(Sink.foreach(System.out::println), Keep.right());
   }

}

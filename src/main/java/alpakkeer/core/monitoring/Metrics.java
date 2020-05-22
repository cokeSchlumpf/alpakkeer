package alpakkeer.core.monitoring;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.function.Function2;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.util.Operators;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public final class Metrics {

   private Metrics() {

   }

   public static Metric<List<Marker>> createMarkerMetric(String name, String description, Function2<Instant, Instant, List<Marker>> get) {
      return new Metric<>() {
         @Override
         public String getName() {
            return name;
         }

         @Override
         public String getDescription() {
            return description;
         }

         @Override
         public CompletionStage<List<Marker>> query(Instant from, Instant to) {
            return CompletableFuture.completedFuture(Operators.suppressExceptions(() -> get.apply(from, to)));
         }
      };
   }

   public static Metric<TimeSeries> createTimeSeriesMetricCS(
      String name, String description, Function2<Instant, Instant, CompletionStage<TimeSeries>> get) {

      return new Metric<>() {
         @Override
         public String getName() {
            return name;
         }

         @Override
         public String getDescription() {
            return description;
         }

         @Override
         public CompletionStage<TimeSeries> query(Instant from, Instant to) {
            return Operators.suppressExceptions(() -> get.apply(from, to));
         }
      };
   }

   public static Metric<TimeSeries> createTimeSeriesMetric(String name, String description, Function2<Instant, Instant, TimeSeries> get) {
      return createTimeSeriesMetricCS(name, description, (f, t) -> CompletableFuture.completedFuture(get.apply(f, t)));
   }

   public static Metric<TimeSeries> createTimeSeriesMetricFromDataPoints(
      String name, String description, Source<DataPoint, NotUsed> datapoints, ActorSystem system) {

      return createTimeSeriesMetricCS(name, description, (from, to) -> datapoints
         .statefulMapConcat(() -> {
            EvictingQueue<Double> firstValue = EvictingQueue.create(1);
            List<Double> lastValue = Lists.newArrayList();

            return dp -> {
               if (dp.getMoment().toEpochMilli() < from.toEpochMilli()) {
                  firstValue.add(dp.getValue());
                  return List.of();
               } else if (dp.getMoment().toEpochMilli() > to.toEpochMilli() && lastValue.isEmpty()) {
                  lastValue.add(dp.getValue());
                  return List.of(DataPoint.apply(to, dp.getValue()));
               } else if (dp.getMoment().toEpochMilli() > to.toEpochMilli()) {
                  return List.of();
               } else if (!firstValue.isEmpty()) {
                  return List.of(DataPoint.apply(from, firstValue.poll()), dp);
               } else {
                  return List.of(dp);
               }
            };
         })
         .runWith(Sink.seq(), system)
         .thenApply(TimeSeries::apply));
   }

   public static Metric<TimeSeries> createTimeSeriesMetricFromDataPoints(
      String name, String description, Supplier<List<DataPoint>> datapoints, ActorSystem system) {

      return createTimeSeriesMetricFromDataPoints(name, description, Source.from(() -> datapoints.get().iterator()), system);
   }

}

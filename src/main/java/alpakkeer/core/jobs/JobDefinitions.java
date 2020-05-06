package alpakkeer.core.jobs;

import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.jobs.monitor.*;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
import alpakkeer.core.values.Nothing;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor(staticName = "apply")
public final class JobDefinitions {

   private RuntimeConfiguration runtimeConfiguration;

   public interface WithRunnableBuilderFactory<P> {

      RuntimeConfiguration getRuntimeConfiguration();

      Name getName();
      
      P getDefaultProperties();

      default WithRunnableBuilder<P> withCancelableRunnable(BiFunction<String, P, CompletionStage<JobHandle>> handleFactory) {
         return WithRunnableBuilder.apply(getRuntimeConfiguration(), getName(), getDefaultProperties(), handleFactory);
      }

      default WithRunnableBuilder<P> withCancelableRunnableFromId(String name, Function<String, CompletionStage<JobHandle>> handleFactory) {
         return withCancelableRunnable((id, p) -> handleFactory.apply(id));
      }

      default WithRunnableBuilder<P> withCancelableRunnableFromProperties(String name, Function<P, CompletionStage<JobHandle>> handleFactory) {
         return withCancelableRunnable((id, p) -> handleFactory.apply(p));
      }

      default WithRunnableBuilder<P> withRunnableCS(BiFunction<String, P, CompletionStage<?>> run) {
         return WithRunnableBuilder.apply(
            getRuntimeConfiguration(), getName(), getDefaultProperties(),
            (id, p) -> CompletableFuture.completedFuture(JobHandles.create(run.apply(id, p))));
      }

      default WithRunnableBuilder<P> withRunnableCSFromId(Function<String, CompletionStage<?>> run) {
         return withRunnableCS((id, p) -> run.apply(id));
      }

      default WithRunnableBuilder<P> withRunnableCSFromProperties(Function<P, CompletionStage<?>> run) {
         return withRunnableCS((id, p) -> run.apply(p));
      }

      default WithRunnableBuilder<P> withRunnable(BiConsumer<String, P> run, Executor executor) {
         return withRunnableCS((id, p) -> CompletableFuture.runAsync(() -> run.accept(id, p), executor));
      }

      default WithRunnableBuilder<P> withRunnable(BiConsumer<String, P> run) {
         return withRunnable(run, ForkJoinPool.commonPool());
      }

      default WithRunnableBuilder<P> withRunnable(Operators.ExceptionalRunnable run, Executor executor) {
         return withRunnable((s, p) -> Operators.suppressExceptions(run), executor);
      }

      default WithRunnableBuilder<P> withRunnable(Operators.ExceptionalRunnable run) {
         return withRunnable(run, ForkJoinPool.commonPool());
      }

      default WithRunnableBuilder<P> withRunnableFromId(Consumer<String> run, Executor executor) {
         return withRunnable((id, p) -> run.accept(id), executor);
      }

      default WithRunnableBuilder<P> withRunnableFromId(Consumer<String> run) {
         return withRunnableFromId(run, ForkJoinPool.commonPool());
      }

      default WithRunnableBuilder<P> withRunnableFromProperties(Operators.ExceptionalConsumer<P> run, Executor executor) {
         return withRunnable((id, p) -> Operators.suppressExceptions(() -> run.accept(p)), executor);
      }

      default WithRunnableBuilder<P> withRunnableFromProperties(Operators.ExceptionalConsumer<P> run) {
         return withRunnableFromProperties(run, ForkJoinPool.commonPool());
      }
      
   }

   @Getter
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class InitialBuilder implements WithRunnableBuilderFactory<Nothing> {

      private RuntimeConfiguration runtimeConfiguration;

      private Name name;

      public <P> WithPropertiesBuilder<P> withProperties(P defaultProperties) {
         return WithPropertiesBuilder.apply(runtimeConfiguration, name, defaultProperties);
      }

      @Override
      public Nothing getDefaultProperties() {
         return Nothing.getInstance();
      }

   }

   @Getter
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class WithPropertiesBuilder<P> implements WithRunnableBuilderFactory<P> {

      private RuntimeConfiguration runtimeConfiguration;

      private Name name;

      private P defaultProperties;

   }

   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class WithRunnableBuilder<P> {

      private RuntimeConfiguration runtimeConfiguration;

      private Name name;

      private P defaultProperties;

      private BiFunction<String, P, CompletionStage<JobHandle>> run;

      private List<ScheduleExecution<P>> scheduleExecutions;

      private CombinedJobMonitor<P> monitors;

      private Logger logger;

      public static <P> WithRunnableBuilder<P> apply(RuntimeConfiguration runtimeConfiguration, Name name, P defaultProperties, BiFunction<String, P, CompletionStage<JobHandle>> run) {
         var logger = LoggerFactory.getLogger(String.format("alpakkeer.jobs.%s", name.getValue())); // TODO: Name to snake case
         return apply(runtimeConfiguration, name, defaultProperties, run, Lists.newArrayList(), CombinedJobMonitor.apply(), logger);
      }

      public JobDefinition<P> build() {
         return SimpleJobDefinition.apply(run, name, defaultProperties, logger, scheduleExecutions, monitors);
      }

      /*
       * Monitors
       */
      public WithRunnableBuilder<P> withHistoryMonitor() {
         return withHistoryMonitor(10);
      }

      public WithRunnableBuilder<P> withHistoryMonitor(int limit) {
         return withMonitor(InMemoryHistoryJobMonitor.apply(limit, runtimeConfiguration.getObjectMapper()));
      }

      public WithRunnableBuilder<P> withLoggingMonitor() {
         return withMonitor(LoggingJobMonitor.apply(name.getValue(), logger, runtimeConfiguration.getObjectMapper()));
      }

      public WithRunnableBuilder<P> withPrometheusMetricsMonitor() {
         return withMonitor(PrometheusJobMonitor.apply(name.getValue(), runtimeConfiguration.getCollectorRegistry()));
      }

      public WithRunnableBuilder<P> withMonitor(JobMonitor<P> monitor) {
         monitors = monitors.withMonitor(monitor);
         return this;
      }

      /*
       * Schedule
       */
      public WithRunnableBuilder<P> withScheduledExecution(ScheduleExecution<P> execution) {
         this.scheduleExecutions.add(execution);
         return this;
      }

      public WithRunnableBuilder<P> withScheduledExecution(CronExpression cron, P properties, boolean queue) {
         return withScheduledExecution(ScheduleExecution.apply(properties, queue, cron));
      }

      public WithRunnableBuilder<P> withScheduledExecution(CronExpression cron, P properties) {
         return withScheduledExecution(cron, properties, true);
      }

      public WithRunnableBuilder<P> withScheduledExecution(CronExpression cron) {
         return withScheduledExecution(cron, defaultProperties, true);
      }

   }

   @AllArgsConstructor(staticName = "apply")
   static class SimpleJobDefinition<P> implements JobDefinition<P> {

      private final BiFunction<String, P, CompletionStage<JobHandle>> run;

      private final Name name;

      private final P defaultProperties;

      private final Logger logger;

      private final List<ScheduleExecution<P>> schedule;

      private final CombinedJobMonitor<P> monitors;

      @Override
      public P getDefaultProperties() {
         return defaultProperties;
      }

      @Override
      public Name getName() {
         return name;
      }

      @Override
      public Logger getLogger() {
         return logger;
      }

      @Override
      public List<ScheduleExecution<P>> getSchedule() {
         return ImmutableList.copyOf(schedule);
      }

      @Override
      public CombinedJobMonitor<P> getMonitors() {
         return monitors;
      }

      @Override
      public CompletionStage<JobHandle> run(String runId, P properties) {
         return run.apply(runId, properties);
      }

   }

   public InitialBuilder create(Name name) {
      return InitialBuilder.apply(runtimeConfiguration, name);
   }

   public InitialBuilder create(String name) {
      return create(Name.apply(name));
   }

}

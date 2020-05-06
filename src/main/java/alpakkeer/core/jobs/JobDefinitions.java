package alpakkeer.core.jobs;

import akka.japi.function.Function2;
import akka.japi.function.Function3;
import akka.japi.function.Procedure2;
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
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@AllArgsConstructor(staticName = "apply")
public final class JobDefinitions {

   private RuntimeConfiguration runtimeConfiguration;

   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobTypeConfiguration<P, C> {

      private final P defaultProperties;

      private final C initialContext;

      public static JobTypeConfiguration<Nothing, Nothing> apply() {
         return apply(Nothing.getInstance(), Nothing.getInstance());
      }

      public <T> JobTypeConfiguration<T, C> withDefaultProperties(T defaultProperties) {
         return apply(defaultProperties, initialContext);
      }

      public <T> JobTypeConfiguration<P, T> withInitialContext(T initialContext) {
         return apply(defaultProperties, initialContext);
      }

   }

   @Value
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobRunnableConfiguration<P, C> {

      Name name;

      RuntimeConfiguration runtimeConfiguration;

      JobTypeConfiguration<P, C> jobTypes;

      public JobSettingsConfiguration<P, C> runCancelableCS(Function3<String, P, C, CompletionStage<JobHandle<C>>> run) {
         return JobSettingsConfiguration.apply(name, runtimeConfiguration, jobTypes, run);
      }

      public JobSettingsConfiguration<P, C> fromCancelable(Function3<String, P, C, JobHandle<C>> run) {
         return runCancelableCS((s, p, c) -> CompletableFuture.completedFuture(run.apply(s, p, c)));
      }

      public JobSettingsConfiguration<P, C> runCS(Function3<String, P, C, CompletionStage<C>> run) {
         return fromCancelable((s, p, c) -> JobHandles.create(run.apply(s, p, c)));
      }

      public JobSettingsConfiguration<P, C> run(Function3<String, P, C, C> run) {
         return runCS((s, p, c) -> CompletableFuture.supplyAsync(() -> Operators.suppressExceptions(() -> run.apply(s, p, c))));
      }

      public JobSettingsConfiguration<P, C> runCS(Function2<String, P, CompletionStage<?>> run) {
         return runCS((s, p, c) -> run.apply(s, p).thenApply(i -> c));
      }

      public JobSettingsConfiguration<P, C> run(Procedure2<String, P> run) {
         return runCS((s, p) -> {
            run.apply(s, p);
            return CompletableFuture.completedFuture(Nothing.getInstance());
         });
      }


   }

   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobSettingsConfiguration<P, C> {

      private final Name name;

      private final RuntimeConfiguration runtimeConfiguration;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final Function3<String, P, C, CompletionStage<JobHandle<C>>> run;

      private List<ScheduleExecution<P>> scheduleExecutions;

      private CombinedJobMonitor<P> monitors;

      private Logger logger;

      public static <P, C> JobSettingsConfiguration<P, C> apply(
         Name name, RuntimeConfiguration runtimeConfiguration, JobTypeConfiguration<P, C> jobTypes, Function3<String, P, C, CompletionStage<JobHandle<C>>> run) {

         var logger = LoggerFactory.getLogger(String.format("alpakkeer.jobs.%s", name.getValue())); // TODO: Name to snake case
         return apply(name, runtimeConfiguration, jobTypes, run, Lists.newArrayList(), CombinedJobMonitor.apply(), logger);
      }

      public JobDefinition<P, C> build() {
         return SimpleJobDefinition.apply(name, jobTypes, run, logger, scheduleExecutions, monitors);
      }

      /*
       * Monitors
       */
      public JobSettingsConfiguration<P, C> withHistoryMonitor() {
         return withHistoryMonitor(10);
      }

      public JobSettingsConfiguration<P, C> withHistoryMonitor(int limit) {
         return withMonitor(InMemoryHistoryJobMonitor.apply(limit, runtimeConfiguration.getObjectMapper()));
      }

      public JobSettingsConfiguration<P, C> withLoggingMonitor() {
         return withMonitor(LoggingJobMonitor.apply(name.getValue(), logger, runtimeConfiguration.getObjectMapper()));
      }

      public JobSettingsConfiguration<P, C> withPrometheusMetricsMonitor() {
         return withMonitor(PrometheusJobMonitor.apply(name.getValue(), runtimeConfiguration.getCollectorRegistry()));
      }

      public JobSettingsConfiguration<P, C> withMonitor(JobMonitor<P> monitor) {
         monitors.withMonitor(monitor);
         return this;
      }

      /*
       * Schedule
       */
      public JobSettingsConfiguration<P, C> withScheduledExecution(ScheduleExecution<P> execution) {
         this.scheduleExecutions.add(execution);
         return this;
      }

      public JobSettingsConfiguration<P, C> withScheduledExecution(CronExpression cron, P properties, boolean queue) {
         return withScheduledExecution(ScheduleExecution.apply(properties, queue, cron));
      }

      public JobSettingsConfiguration<P, C> withScheduledExecution(CronExpression cron, P properties) {
         return withScheduledExecution(cron, properties, true);
      }

      public JobSettingsConfiguration<P, C> withScheduledExecution(CronExpression cron) {
         return withScheduledExecution(cron, jobTypes.defaultProperties, true);
      }

   }

   @AllArgsConstructor(staticName = "apply")
   static class SimpleJobDefinition<P, C> implements JobDefinition<P, C> {

      private final Name name;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final Function3<String, P, C, CompletionStage<JobHandle<C>>> run;

      private final Logger logger;

      private final List<ScheduleExecution<P>> schedule;

      private final CombinedJobMonitor<P> monitors;

      @Override
      public P getDefaultProperties() {
         return jobTypes.defaultProperties;
      }

      @Override
      public C getInitialContext() {
         return null;
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
      public CompletionStage<JobHandle<C>> run(String executionId, P properties, C context) {
         return Operators.suppressExceptions(() -> run.apply(executionId, properties, context));
      }

   }

   public <P, C> JobRunnableConfiguration<P, C> create(String name, Function<JobTypeConfiguration<Nothing, Nothing>, JobTypeConfiguration<P, C>> cfg) {
      return JobRunnableConfiguration.apply(Name.apply(name), runtimeConfiguration, cfg.apply(JobTypeConfiguration.apply()));
   }

   public <P, C> JobRunnableConfiguration<P, C> create(String name, P defaultProperties, C initialContext) {
      return create(name, cfg -> JobTypeConfiguration.apply(defaultProperties, initialContext));
   }

   public <P> JobRunnableConfiguration<P, Nothing> create(String name, P defaultProperties) {
      return create(name, defaultProperties, Nothing.getInstance());
   }

   public JobRunnableConfiguration<Nothing, Nothing> create(String name) {
      return create(name, Nothing.getInstance());
   }

}

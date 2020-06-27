package alpakkeer.core.jobs;

import akka.Done;
import akka.japi.Procedure;
import akka.japi.function.Procedure2;
import akka.stream.javadsl.RunnableGraph;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.jobs.monitor.*;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import alpakkeer.core.util.Strings;
import alpakkeer.core.values.Nothing;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.javalin.Javalin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@AllArgsConstructor(staticName = "apply")
public final class JobDefinitions {

   private RuntimeConfiguration runtimeConfiguration;

   /**
    * A simple builder to configure property and context of a job definition.
    *
    * @param <P> The job's properties type
    * @param <C> The job's context type
    */
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobTypeConfiguration<P, C> {

      private final P defaultProperties;

      private final C initialContext;

      /**
       * Creates a new instance.
       *
       * @return The new instance.
       */
      public static JobTypeConfiguration<Nothing, Done> apply() {
         return apply(Nothing.getInstance(), Done.getInstance());
      }

      /**
       * Configures the default properties for a job. These properties will be used to run the job if no other properties
       * are passed when starting a job execution.
       *
       * @param defaultProperties The default properties
       * @param <T>               The type of the properties
       * @return A new builder instance
       */
      public <T> JobTypeConfiguration<T, C> withDefaultProperties(T defaultProperties) {
         return apply(defaultProperties, initialContext);
      }

      /**
       * Configures the initial context of a job. This context will be used to initialize the job for the first time. After
       * each run, the job can update its context.
       *
       * @param initialContext The job's initial context
       * @param <T>            The type of the context
       * @return A new builder instance
       */
      public <T> JobTypeConfiguration<P, T> withInitialContext(T initialContext) {
         return apply(defaultProperties, initialContext);
      }

   }

   /**
    * An intermediate builder to create job definitions.
    *
    * @param <P> The property type of the job
    * @param <C> The context type of the job
    */
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobRunnableConfiguration<P, C> {

      private final String name;

      private final RuntimeConfiguration runtimeConfiguration;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final JobMonitorGroup<P, C> monitors;

      /**
       * Creates a new instance.
       *
       * @param name                 The name of the job
       * @param runtimeConfiguration The Alpakkeer runtime configuration
       * @param jobTypes             The type configurations of the job
       * @param <P>                  The job's property type
       * @param <C>                  The job's context type
       * @return A new instance
       */
      public static <P, C> JobRunnableConfiguration<P, C> apply(
         String name, RuntimeConfiguration runtimeConfiguration,
         JobTypeConfiguration<P, C> jobTypes) {

         return apply(name, runtimeConfiguration, jobTypes, JobMonitorGroup.apply());
      }

      /**
       * Define the job which returns a {@link JobHandle}.
       *
       * @param run The job execution factory.
       * @return The {@link JobSettingsConfiguration} for the job.
       */
      public JobSettingsConfiguration<P, C> runCancelableCS(Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run) {
         return JobSettingsConfiguration.apply(name, runtimeConfiguration, jobTypes, run, monitors);
      }

      public JobSettingsConfiguration<P, C> runCancelable(Function<JobStreamBuilder<P, C>, JobHandle<C>> run) {
         return runCancelableCS(sb -> CompletableFuture.completedFuture(run.apply(sb)));
      }

      public JobSettingsConfiguration<P, C> runCS(Function<JobStreamBuilder<P, C>, CompletionStage<C>> run) {
         return runCancelable(sb -> JobHandles.create(run.apply(sb)));
      }

      public JobSettingsConfiguration<P, C> runGraph(Function<JobStreamBuilder<P, C>, RunnableGraph<CompletionStage<C>>> run) {
         return runCS(sb -> Operators.suppressExceptions(() -> run.apply(sb)).run(runtimeConfiguration.getSystem()));
      }

      public JobSettingsConfiguration<P, C> runScalaGraph(Function<JobStreamBuilder<P, C>, akka.stream.scaladsl.RunnableGraph<Future<C>>> run) {
         return runGraph(sb -> run.apply(sb).mapMaterializedValue(FutureConverters::toJava).asJava());
      }

      public JobSettingsConfiguration<P, C> run(Function<JobStreamBuilder<P, C>, C> run) {
         return runCS(sb -> CompletableFuture.supplyAsync(() -> Operators.suppressExceptions(() -> run.apply(sb))));
      }

      public JobSettingsConfiguration<P, C> run(Procedure<JobStreamBuilder<P, C>> run) {
         return runCancelable(sb -> JobHandles
            .createFromOptional(CompletableFuture
               .supplyAsync(() -> {
                  Operators.suppressExceptions(() -> run.apply(sb));
                  return Optional.empty();
               })));
      }

   }

   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobSettingsConfiguration<P, C> {

      private final String name;

      private final RuntimeConfiguration runtimeConfiguration;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run;

      private final JobMonitorGroup<P, C> monitors;

      private List<ScheduleExecution<P>> scheduleExecutions;

      private List<Procedure2<Javalin, Job<P, C>>> apiExtensions;

      private Logger logger;

      public static <P, C> JobSettingsConfiguration<P, C> apply(
         String name, RuntimeConfiguration runtimeConfiguration, JobTypeConfiguration<P, C> jobTypes,
         Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run, JobMonitorGroup<P, C> monitors) {

         var logger = LoggerFactory.getLogger(String.format(
            "alpakkeer.jobs.%s",
            Strings.convert(name).toSnakeCase()));

         return apply(
            name, runtimeConfiguration, jobTypes, run, monitors,
            Lists.newArrayList(), Lists.newArrayList(), logger);
      }

      public JobDefinition<P, C> build() {
         return SimpleJobDefinition.apply(name, jobTypes, run, logger, scheduleExecutions, monitors, apiExtensions);
      }

      public JobSettingsConfiguration<P, C> withApiEndpoint(Procedure2<Javalin, Job<P, C>> apiExtension) {
         apiExtensions.add(apiExtension);
         return this;
      }

      public JobSettingsConfiguration<P, C> withHistoryMonitor() {
         return withHistoryMonitor(10);
      }

      public JobSettingsConfiguration<P, C> withHistoryMonitor(int limit) {
         return withMonitor(InMemoryHistoryJobMonitor.apply(
            limit,
            runtimeConfiguration.getObjectMapper(),
            runtimeConfiguration.getSystem()));
      }

      public JobSettingsConfiguration<P, C> withLoggingMonitor() {
         return withMonitor(LoggingJobMonitor.apply(name, logger, runtimeConfiguration.getObjectMapper()));
      }

      public JobSettingsConfiguration<P, C> withPrometheusMonitor() {
         return withMonitor(PrometheusJobMonitor.apply(name, runtimeConfiguration.getCollectorRegistry()));
      }

      public JobSettingsConfiguration<P, C> withMonitor(JobMonitor<P, C> monitor) {
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

      private final String name;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run;

      private final Logger logger;

      private final List<ScheduleExecution<P>> schedule;

      private final JobMonitorGroup<P, C> monitors;

      private final List<Procedure2<Javalin, Job<P, C>>> apiExtensions;

      @Override
      public void extendApi(Javalin api, Job<P, C> jobInstance) {
         apiExtensions.forEach(ext -> Operators.suppressExceptions(() -> ext.apply(api, jobInstance)));
      }

      @Override
      public P getDefaultProperties() {
         return jobTypes.defaultProperties;
      }

      @Override
      public C getInitialContext() {
         return jobTypes.initialContext;
      }

      @Override
      public String getName() {
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
      public JobMonitorGroup<P, C> getMonitors() {
         return monitors;
      }

      @Override
      public CompletionStage<JobHandle<C>> run(String executionId, P properties, C context) {
         return Operators.suppressExceptions(() -> run.apply(JobStreamBuilder.apply(monitors, executionId, properties, context)));
      }

   }

   public <P, C> JobRunnableConfiguration<P, C> create(String name, Function<JobTypeConfiguration<Nothing, Done>, JobTypeConfiguration<P, C>> cfg) {
      return JobRunnableConfiguration.apply(name, runtimeConfiguration, cfg.apply(JobTypeConfiguration.apply()));
   }

   public <P, C> JobRunnableConfiguration<P, C> create(String name, P defaultProperties, C initialContext) {
      return create(name, cfg -> JobTypeConfiguration.apply(defaultProperties, initialContext));
   }

   public <P> JobRunnableConfiguration<P, Done> create(String name, P defaultProperties) {
      return create(name, defaultProperties, Done.getInstance());
   }

   public JobRunnableConfiguration<Nothing, Done> create(String name) {
      return create(name, Nothing.getInstance());
   }

   public RuntimeConfiguration getRuntime() {
      return runtimeConfiguration;
   }

}

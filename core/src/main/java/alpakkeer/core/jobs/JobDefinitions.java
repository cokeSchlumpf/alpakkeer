package alpakkeer.core.jobs;

import akka.Done;
import akka.japi.Procedure;
import akka.japi.function.Procedure2;
import akka.stream.javadsl.RunnableGraph;
import alpakkeer.Alpakkeer;
import alpakkeer.AlpakkeerRuntime;
import alpakkeer.config.JobConfiguration;
import alpakkeer.config.ScheduledExecutionConfiguration;
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

/**
 * Factory for {@link JobDefinition}.
 */
@AllArgsConstructor(staticName = "apply")
public final class JobDefinitions {

   private static final Logger LOG = LoggerFactory.getLogger(Alpakkeer.class);

   private AlpakkeerRuntime runtimeConfiguration;

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

      private final AlpakkeerRuntime runtimeConfiguration;

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
         String name, AlpakkeerRuntime runtimeConfiguration,
         JobTypeConfiguration<P, C> jobTypes) {

         return apply(name, runtimeConfiguration, jobTypes, JobMonitorGroup.apply());
      }

      /**
       * Define a job execution factory which returns a {@link CompletionStage} of a {@link JobHandle}. A {@link JobHandle} can be used
       * to cancel the job during execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> runCancelableCS(Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run) {
         return JobSettingsConfiguration.apply(name, runtimeConfiguration, jobTypes, run, monitors);
      }

      /**
       * Define a job execution factory which returns a {@link JobHandle}. A {@link JobHandle} can be used to cancel
       * the job during execution. The result of the Job Handle will be the next value of the context and the
       * result value of the job execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> runCancelable(Function<JobStreamBuilder<P, C>, JobHandle<C>> run) {
         return runCancelableCS(sb -> CompletableFuture.completedFuture(run.apply(sb)));
      }

      /**
       * Define a job execution factory which returns a {@link CompletionStage}. The result of the completion stage will
       * be the next context of the job and the result value of the job execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> runCS(Function<JobStreamBuilder<P, C>, CompletionStage<C>> run) {
         return runCancelable(sb -> JobHandles.create(run.apply(sb)));
      }

      /**
       * Define a job execution factory which returns a Scala {@link Future}. The result of the future will be the next
       * context of the job and the result of the job execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> runCSScala(Function<JobStreamBuilder<P, C>, Future<C>> run) {
         return runCancelable(sb -> JobHandles.create(FutureConverters.toJava(run.apply(sb))));
      }

      /**
       * Define a job execution factory which returns an Akka Streams {@link RunnableGraph} which materializes to a
       * {@link CompletionStage}. The result will be the next job context and the result of the
       * job execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> runGraph(Function<JobStreamBuilder<P, C>, RunnableGraph<CompletionStage<C>>> run) {
         return runCS(sb -> Operators.suppressExceptions(() -> run.apply(sb)).run(runtimeConfiguration.getSystem()));
      }

      /**
       * Define a job execution factory which returns an Akka Streams {@link RunnableGraph} which materializes to a
       * Scala {@link Future}. The result will be the next job context and the result of the job execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> runGraphScala(Function<JobStreamBuilder<P, C>, akka.stream.scaladsl.RunnableGraph<Future<C>>> run) {
         return runGraph(sb -> run.apply(sb).mapMaterializedValue(FutureConverters::toJava).asJava());
      }

      /**
       * Define a job execution factory by defining a simple function. The function will be executed asynchronously on each
       * job execution. The result of the function will be the next job context and the result of the job execution.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> run(Function<JobStreamBuilder<P, C>, C> run) {
         return runCS(sb -> CompletableFuture.supplyAsync(() -> Operators.suppressExceptions(() -> run.apply(sb))));
      }

      /**
       * Define a job execution factory by defining a simple procedure. The procedure will be executed asynchronously on
       * each job execution. The context of the job will always be the initial context and thus can be ignored.
       *
       * @param run The job execution factory
       * @return The {@link JobSettingsConfiguration} for the job
       */
      public JobSettingsConfiguration<P, C> run(Procedure<JobStreamBuilder<P, C>> run) {
         return runCancelable(sb -> JobHandles
            .createFromOptional(CompletableFuture
               .supplyAsync(() -> {
                  Operators.suppressExceptions(() -> run.apply(sb));
                  return Optional.empty();
               })));
      }

   }

   /**
    * The final builder for creating {@link JobDefinition} instances.
    *
    * @param <P> The property type of the job
    * @param <C> The context type of the job
    */
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class JobSettingsConfiguration<P, C> {

      private final String name;

      private final AlpakkeerRuntime runtime;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run;

      private final JobMonitorGroup<P, C> monitors;

      private boolean enabled;

      private List<ScheduleExecution<P>> scheduleExecutions;

      private List<Procedure2<Javalin, Job<P, C>>> apiExtensions;

      private Logger logger;

      /**
       * Creates a new instance.
       *
       * @param name                 The name of the job
       * @param runtimeConfiguration The Alpakkeer runtime configuration
       * @param jobTypes             The type configurations of the job
       * @param run                  The job execution factory
       * @param monitors             The monitors for the job
       * @param <P>                  The job's property type
       * @param <C>                  The job's context type
       * @return A new instance
       */
      public static <P, C> JobSettingsConfiguration<P, C> apply(
         String name, AlpakkeerRuntime runtimeConfiguration, JobTypeConfiguration<P, C> jobTypes,
         Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run, JobMonitorGroup<P, C> monitors) {

         var logger = LoggerFactory.getLogger(String.format(
            "alpakkeer.jobs.%s",
            Strings.convert(name).toSnakeCase()));

         return apply(
            name, runtimeConfiguration, jobTypes, run, monitors,
            true, Lists.newArrayList(), Lists.newArrayList(), logger);
      }

      /**
       * Creates the final job definition.
       *
       * @return The jib definition.
       */
      public JobDefinition<P, C> build() {
         return SimpleJobDefinition.apply(name, enabled, jobTypes, run, logger, scheduleExecutions, monitors, apiExtensions, runtime);
      }

      /**
       * The job definition can be disabled. If the definition is disabled, the job will not be started/ initialized
       * during startup of Alpakkeer.
       *
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> disabled() {
         this.enabled = false;
         return this;
      }

      /**
       * Set whether the job should be enabled or not. By default it is enabled. If the definition is disabled,
       * the job will not be started/ initialized during startup of Alpakkeer.
       *
       * @param enabled Whether the job is enabled or not.
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> enabled(boolean enabled) {
         this.enabled = enabled;
         return this;
      }

      /**
       * Enable the job. By default it is enabled. If the definition is disabled,
       * the job will not be started/ initialized during startup of Alpakkeer.
       *
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> enabled() {
         return enabled(true);
      }

      /**
       * Specify an additional API endpoint for the job. The procedure passed to this method may use the {@link Javalin}
       * instance to define API endpoints which also use/ access the related {@link Job} instance.
       *
       * @param apiExtension A procedure which can extend the {@link Javalin} API.
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withApiEndpoint(Procedure2<Javalin, Job<P, C>> apiExtension) {
         apiExtensions.add(apiExtension);
         return this;
      }

      /**
       * Read configurations for the job from the provided config object.
       *
       * @param configuration The configuration object
       * @return The current instance of the builder
       */
      @SuppressWarnings("unchecked")
      public JobSettingsConfiguration<P, C> withConfiguration(JobConfiguration configuration) {
         if (configuration.isClearMonitors()) {
            this.monitors.clearMonitors();
         }

         if (configuration.isClearSchedule()) {
            this.scheduleExecutions.clear();
         }

         var next = this.enabled(configuration.isEnabled());

         for (String m : configuration.getMonitors()) {
            switch (m) {
               case "logging":
                  next = next.withLoggingMonitor();
                  break;
               case "prometheus":
                  next = next.withPrometheusMonitor();
                  break;
               case "history":
                  next = next.withHistoryMonitor();
                  break;
            }
         }

         for (ScheduledExecutionConfiguration c : configuration.getSchedule()) {
            var props = Operators.suppressExceptions(
               () -> runtime.getObjectMapper().readValue(c.getProperties(), (Class<P>) jobTypes.defaultProperties.getClass()));

            next = next.withScheduledExecution(
               ScheduleExecution.apply(props, c.isQueue(), CronExpression.apply(c.getCronExpression())));
         }

         return next;
      }

      /**
       * Use this method to enable configuration overrides (e.g. for different environments, etc.); The configuration
       * will be taken from default location `alpakkeer.jobs` (which is a list of job configurations)
       *
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withConfiguration() {
         var config = runtime.getConfiguration().getJobConfiguration(name);

         if (config.isPresent()) {
            return withConfiguration(config.get());
         } else {
            LOG.warn("No configuration object found for job `{}` within `alpakkeer.job`", name);
            return this;
         }
      }

      /**
       * Enable a {@link InMemoryHistoryJobMonitor} for the job. A history monitor stores errors and results of executions. This default
       * history monitor will store the information about the last 10 executions.
       *
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withHistoryMonitor() {
         return withHistoryMonitor(10);
      }

      /**
       * Enable a {@link InMemoryHistoryJobMonitor} for the job. A history monitor stores errors and results of executions.
       *
       * @param limit Number of executions for which information should be kept
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withHistoryMonitor(int limit) {
         return withMonitor(InMemoryHistoryJobMonitor.apply(
            limit,
            runtime.getObjectMapper(),
            runtime.getSystem()));
      }

      /**
       * Enables a {@link LoggingJobMonitor} for the job. The monitor will log information and errors about the executions of the
       * job to the default logging infrastructure.
       *
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withLoggingMonitor() {
         return withMonitor(LoggingJobMonitor.apply(name, logger, runtime.getObjectMapper()));
      }

      /**
       * Enables a {@link PrometheusJobMonitor} for the job. The monitor will log information about the executions of the job
       * in Prometheus Metrics.
       *
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withPrometheusMonitor() {
         return withMonitor(PrometheusJobMonitor.apply(name, runtime.getCollectorRegistry()));
      }

      /**
       * Enable an additional monitor for the job.
       *
       * @param monitor The monitor to be enabled.
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withMonitor(JobMonitor<P, C> monitor) {
         monitors.withMonitor(monitor);
         return this;
      }

      /**
       * Schedule executions of the job.
       *
       * @param execution The definition of the scheduled execution.
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withScheduledExecution(ScheduleExecution<P> execution) {
         this.scheduleExecutions.add(execution);
         return this;
      }

      /**
       * Schedule executions of the job.
       *
       * @param cron       The cron expression which defines the times when the job is executed
       * @param properties The properties which will be passed to the job execution factory when starting the job
       * @param queue      Whether the job execution should be queued if it already running when the execution gets triggered by schedule
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withScheduledExecution(CronExpression cron, P properties, boolean queue) {
         return withScheduledExecution(ScheduleExecution.apply(properties, queue, cron));
      }

      /**
       * Schedule executions of the job with queueing enabled.
       *
       * @param cron       The cron expression which defines the times when the job is executed
       * @param properties The properties which will be passed to the job execution factory when starting the job
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withScheduledExecution(CronExpression cron, P properties) {
         return withScheduledExecution(cron, properties, true);
      }

      /**
       * Schedule executions of the job with queueing enabled and default properties.
       *
       * @param cron The cron expression which defines the times when the job is executed
       * @return The current instance of the builder
       */
      public JobSettingsConfiguration<P, C> withScheduledExecution(CronExpression cron) {
         return withScheduledExecution(cron, jobTypes.defaultProperties, true);
      }

   }

   @AllArgsConstructor(staticName = "apply")
   static class SimpleJobDefinition<P, C> implements JobDefinition<P, C> {

      private final String name;

      private final boolean enabled;

      private final JobTypeConfiguration<P, C> jobTypes;

      private final Function<JobStreamBuilder<P, C>, CompletionStage<JobHandle<C>>> run;

      private final Logger logger;

      private final List<ScheduleExecution<P>> schedule;

      private final JobMonitorGroup<P, C> monitors;

      private final List<Procedure2<Javalin, Job<P, C>>> apiExtensions;

      private final AlpakkeerRuntime runtime;

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
      public boolean isEnabled() {
         return enabled;
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
         return Operators.suppressExceptions(() -> run.apply(JobStreamBuilder.apply(runtime, monitors, executionId, properties, context)));
      }

   }

   /**
    * Start creating a new job.
    *
    * @param name The name of the job
    * @param cfg  A helper-function to configure job's types and default properties and context
    * @param <P>  The type of the properties
    * @param <C>  The type of the context
    * @return A new builder instance
    */
   public <P, C> JobRunnableConfiguration<P, C> create(String name, Function<JobTypeConfiguration<Nothing, Done>, JobTypeConfiguration<P, C>> cfg) {
      return JobRunnableConfiguration.apply(name, runtimeConfiguration, cfg.apply(JobTypeConfiguration.apply()));
   }

   /**
    * Start creating a new job with default properties and default context. Properties are arguments which are passed to
    * every job execution. The context is state of the job which can be changed with each execution. Also the context
    * will be passed to the job execution factory.
    * <p>
    * The default properties will be passed to the job execution factory when no other properties are defined when the job
    * is initiated.
    *
    * @param name              The name of the job
    * @param defaultProperties The default properties of the job
    * @param initialContext    The initial context of the job
    * @param <P>               The type of the properties
    * @param <C>               The type of the context
    * @return A new builder instance
    */
   public <P, C> JobRunnableConfiguration<P, C> create(String name, P defaultProperties, C initialContext) {
      return create(name, cfg -> JobTypeConfiguration.apply(defaultProperties, initialContext));
   }

   /**
    * Start creating a new job with default properties. Properties are arguments which are passed to
    * every job execution.
    * <p>
    * The default properties will be passed to the job execution factory when no other properties are defined when the job
    * is initiated.
    *
    * @param name              The name of the job
    * @param defaultProperties The default properties of the job
    * @param <P>               The type of the properties
    * @return A new builder instance
    */
   public <P> JobRunnableConfiguration<P, Done> create(String name, P defaultProperties) {
      return create(name, defaultProperties, Done.getInstance());
   }

   /**
    * Start creating a new job.
    *
    * @param name The name of the job
    * @return A new builder instance
    */
   public JobRunnableConfiguration<Nothing, Done> create(String name) {
      return create(name, Nothing.getInstance());
   }

   /**
    * Access other Alpakkeer runtime components during job definition.
    *
    * @return The initialized Alpakkeer runtime
    */
   public AlpakkeerRuntime getRuntime() {
      return runtimeConfiguration;
   }

}

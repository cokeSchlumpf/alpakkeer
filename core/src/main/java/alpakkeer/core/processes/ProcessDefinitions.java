package alpakkeer.core.processes;

import akka.japi.Pair;
import akka.japi.function.Function;
import akka.japi.function.Function2;
import akka.stream.UniqueKillSwitch;
import akka.stream.javadsl.RunnableGraph;
import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.processes.monitor.LoggingProcessMonitor;
import alpakkeer.core.processes.monitor.ProcessMonitor;
import alpakkeer.core.processes.monitor.ProcessMonitorGroup;
import alpakkeer.core.stream.StreamBuilder;
import alpakkeer.core.util.Operators;
import alpakkeer.core.util.Strings;
import alpakkeer.core.values.Name;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class ProcessDefinitions {

   private final RuntimeConfiguration runtimeConfiguration;

   private final StreamBuilder sb;

   public static ProcessDefinitions apply(RuntimeConfiguration runtimeConfiguration) {
      return apply(runtimeConfiguration, null); // TODO wm0
   }

   @AllArgsConstructor(staticName = "apply")
   public static class ProcessRunnableBuilder {

      private final String name;

      private final RuntimeConfiguration runtimeConfiguration;

      private final StreamBuilder sb;

      public ProcessDefinitionBuilder runCancellableCS(ProcessRunner run) {
         return ProcessDefinitionBuilder.apply(name, run);
      }

      public ProcessDefinitionBuilder runCancellable(Function<String, ProcessHandle> run) {
         return runCancellableCS(s -> CompletableFuture.completedFuture(Operators.suppressExceptions(() -> run.apply(s))));
      }

      public ProcessDefinitionBuilder runCS(Function<String, CompletionStage<?>> run) {
         return runCancellable(s -> ProcessHandles.createFromCS(run.apply(s)));
      }

      public ProcessDefinitionBuilder runGraph(Function2<String, StreamBuilder, RunnableGraph<CompletionStage<?>>> graph) {
         return runCS(s -> graph.apply(s, sb).run(runtimeConfiguration.getSystem()));
      }

      public ProcessDefinitionBuilder runGraph(Function<StreamBuilder, RunnableGraph<CompletionStage<?>>> graph) {
         return runGraph((s, sb) -> graph.apply(sb));
      }

      public ProcessDefinitionBuilder runCancellableGraph(Function2<String, StreamBuilder, RunnableGraph<Pair<UniqueKillSwitch, CompletionStage<?>>>> graph) {
         return runCancellable(s -> ProcessHandles.createFromCancellableGraph(graph.apply(s, sb).run(runtimeConfiguration.getSystem())));
      }

      public ProcessDefinitionBuilder runCancellableGraph(Function<StreamBuilder, RunnableGraph<Pair<UniqueKillSwitch, CompletionStage<?>>>> graph) {
         return runCancellableGraph((s, sb) -> graph.apply(sb));
      }

   }

   @AllArgsConstructor(staticName = "apply")
   public static class ProcessDefinitionBuilder {

      private final String name;

      private final ProcessRunner runner;

      private Duration initialRetryBackoff;

      private Duration completionRestartBackoff;

      private Duration retryBackoffResetTimeout;

      private Logger logger;

      private boolean initiallyStarted;

      private final ProcessMonitorGroup monitors;

      public static ProcessDefinitionBuilder apply(String name, ProcessRunner runner) {
         var logger = LoggerFactory.getLogger(String.format(
            "alpakkeer.processes.%s",
            Strings.convert(name).toSnakeCase()));

         return apply(
            name, runner, Duration.ofSeconds(10), Duration.ofMinutes(10),
            Duration.ofMinutes(1), logger, true, ProcessMonitorGroup.apply());
      }

      public ProcessDefinition build() {
         return SimpleProcessDefinition.apply(
            name, initialRetryBackoff, completionRestartBackoff, retryBackoffResetTimeout, runner,
            logger, initiallyStarted, monitors);
      }

      public ProcessDefinitionBuilder initializeStarted() {
         this.initiallyStarted = true;
         return this;
      }

      public ProcessDefinitionBuilder initializeStopped() {
         this.initiallyStarted = false;
         return this;
      }

      public ProcessDefinitionBuilder withCompletionRestartBackoff(Duration completionRestartBackoff) {
         this.completionRestartBackoff = completionRestartBackoff;
         return this;
      }

      public ProcessDefinitionBuilder withInitialRetryBackoff(Duration initialRetryBackoff) {
         this.initialRetryBackoff = initialRetryBackoff;
         return this;
      }

      public ProcessDefinitionBuilder withMonitor(ProcessMonitor monitor) {
         this.monitors.withMonitor(monitor);
         return this;
      }

      public ProcessDefinitionBuilder withLoggingMonitor() {
         return withMonitor(LoggingProcessMonitor.apply(name, logger));
      }

      public ProcessDefinitionBuilder withRetryBackoffResetTimeout(Duration retryBackoffResetTimeout) {
         this.retryBackoffResetTimeout = retryBackoffResetTimeout;
         return this;
      }

   }

   @AllArgsConstructor(staticName = "apply")
   private static class SimpleProcessDefinition implements ProcessDefinition {

      private final String name;

      private final Duration initialBackoff;

      private final Duration completionRestartBackoff;

      private final Duration retryBackoffResetTimeout;

      private final ProcessRunner runner;

      private final Logger logger;

      private final boolean initiallyStarted;

      private final ProcessMonitorGroup monitors;

      @Override
      public boolean isInitiallyStarted() {
         return initiallyStarted;
      }

      @Override
      public Name getName() {
         return Name.apply(name);
      }

      @Override
      public Logger getLogger() {
         return logger;
      }

      @Override
      public ProcessMonitorGroup getMonitors() {
         return monitors;
      }

      @Override
      public Duration getInitialRetryBackoff() {
         return initialBackoff;
      }

      @Override
      public Duration getCompletionRestartBackoff() {
         return completionRestartBackoff;
      }

      @Override
      public Duration getRetryBackoffResetTimeout() {
         return retryBackoffResetTimeout;
      }

      @Override
      public CompletionStage<ProcessHandle> run(String executionId) {
         return runner.run(executionId);
      }

   }

   public ProcessRunnableBuilder create(String name) {
      return ProcessRunnableBuilder.apply(name, runtimeConfiguration, sb);
   }

}

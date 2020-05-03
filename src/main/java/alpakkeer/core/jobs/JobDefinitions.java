package alpakkeer.core.jobs;

import alpakkeer.core.jobs.model.ScheduleExecution;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
import alpakkeer.core.values.Nothing;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class JobDefinitions {

   public interface WithRunnableBuilderFactory<P> {
      
      Name getName();
      
      P getDefaultProperties();

      default WithRunnableBuilder<P> withCancelableRunnable(BiFunction<String, P, CompletionStage<JobHandle>> handleFactory) {
         return WithRunnableBuilder.apply(getName(), getDefaultProperties(), handleFactory, Lists.newArrayList());
      }

      default WithRunnableBuilder<P> withCancelableRunnableFromId(String name, Function<String, CompletionStage<JobHandle>> handleFactory) {
         return withCancelableRunnable((id, p) -> handleFactory.apply(id));
      }

      default WithRunnableBuilder<P> withCancelableRunnableFromProperties(String name, Function<P, CompletionStage<JobHandle>> handleFactory) {
         return withCancelableRunnable((id, p) -> handleFactory.apply(p));
      }

      default WithRunnableBuilder<P> withRunnableCS(BiFunction<String, P, CompletionStage<?>> run) {
         return WithRunnableBuilder.apply(
            getName(), getDefaultProperties(), (id, p) -> CompletableFuture.completedFuture(JobHandles.create(run.apply(id, p))), Lists.newArrayList());
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

      Name name;

      public <P> WithPropertiesBuilder<P> withProperties(P defaultProperties) {
         return WithPropertiesBuilder.apply(name, defaultProperties);
      }

      @Override
      public Nothing getDefaultProperties() {
         return Nothing.getInstance();
      }

   }

   @Getter
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class WithPropertiesBuilder<P> implements WithRunnableBuilderFactory<P> {

      Name name;

      P defaultProperties;

   }

   @Value
   @AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
   public static class WithRunnableBuilder<P> {

      Name name;

      P defaultProperties;

      BiFunction<String, P, CompletionStage<JobHandle>> run;

      List<ScheduleExecution<P>> scheduleExecutions;

      public JobDefinition<P> build() {
         return SimpleJobDefinition.apply(run, name, defaultProperties, scheduleExecutions);
      }

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

      private final List<ScheduleExecution<P>> schedule;

      @Override
      public P getDefaultProperties() {
         return defaultProperties;
      }

      @Override
      public Name getName() {
         return name;
      }

      @Override
      public List<ScheduleExecution<P>> getSchedule() {
         return ImmutableList.copyOf(schedule);
      }

      @Override
      public CompletionStage<JobHandle> run(String runId, P properties) {
         return run.apply(runId, properties);
      }

   }

   private JobDefinitions() {

   }

   public static InitialBuilder create(Name name) {
      return InitialBuilder.apply(name);
   }

   public static InitialBuilder create(String name) {
      return create(Name.apply(name));
   }

}

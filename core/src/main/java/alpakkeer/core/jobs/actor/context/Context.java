package alpakkeer.core.jobs.actor.context;

import alpakkeer.core.jobs.context.ContextStore;
import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.model.ScheduledExecution;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.util.Operators;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Value;
import scala.Tuple2;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(staticName = "apply")
public class Context<P, C> {

   JobDefinition<P, C> jobDefinition;

   CronScheduler scheduler;

   ContextStore contextStore;

   List<ScheduledExecutionReference<P>> schedule;

   List<QueuedExecutionInternal<P, C>> queue;

   public static <P, C> Context<P, C> apply(JobDefinition<P, C> jobDefinition, CronScheduler scheduler, ContextStore contextStore) {
      return apply(jobDefinition, scheduler, contextStore, Lists.newArrayList(), Lists.newArrayList());
   }

   public void addScheduledExecution(ScheduledExecutionReference<P> scheduled) {
      schedule.add(scheduled);
   }

   public CompletionStage<List<ScheduledExecution<P>>> getSchedule() {
      return Operators
         .allOf(schedule
            .stream()
            .map(ref -> scheduler.getJob(ref.getName()).thenApply(details -> details.map(d -> Tuple2.apply(ref, d))))
            .collect(Collectors.toList()))
         .thenApply(list -> list
            .stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(tuple -> {
               var ref = tuple._1();
               var details = tuple._2();

               return ScheduledExecution.apply(
                  ref.getProperties(), ref.isQueue(),
                  ref.getCron(), details.getNextExecution());
            })
            .collect(Collectors.toList()));
   }

   public void removeScheduledExecution(String name) {
      schedule.removeIf(s -> s.getName().equals(name));
   }

}

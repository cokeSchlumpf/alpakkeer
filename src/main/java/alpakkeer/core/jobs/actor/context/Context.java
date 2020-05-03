package alpakkeer.core.jobs.actor.context;

import alpakkeer.core.jobs.JobDefinition;
import alpakkeer.core.jobs.model.QueuedExecution;
import alpakkeer.core.jobs.model.ScheduledExecution;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
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
public class Context<P> {

   JobDefinition<P> jobDefinition;

   CronScheduler scheduler;

   List<ScheduledExecutionReference<P>> schedule;

   List<QueuedExecution<P>> queue;

   public static <P> Context<P> apply(JobDefinition<P> jobDefinition, CronScheduler scheduler) {
      return apply(jobDefinition, scheduler, Lists.newArrayList(), Lists.newArrayList());
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

   public void removeScheduledExecution(Name name) {
      schedule.removeIf(s -> s.getName().equals(name));
   }

}

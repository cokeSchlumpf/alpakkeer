package alpakkeer.core.jobs.model;

import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.values.Name;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduleExecution<P> {

   P properties;

   boolean queue;

   CronExpression cron;

}

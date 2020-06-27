package alpakkeer.core.jobs.model;

import alpakkeer.core.scheduler.model.CronExpression;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduleExecution<P> {

   P properties;

   boolean queue;

   CronExpression cron;

}

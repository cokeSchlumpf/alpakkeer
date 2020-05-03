package alpakkeer.core.jobs.model;

import alpakkeer.core.scheduler.model.CronExpression;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduledExecution<P> {

   P properties;

   boolean queue;

   CronExpression cron;

   LocalDateTime nextExecution;


}

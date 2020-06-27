package alpakkeer.core.jobs.actor.context;

import alpakkeer.core.scheduler.model.CronExpression;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduledExecutionReference<P> {

   String name;

   P properties;

   boolean queue;

   CronExpression cron;

}

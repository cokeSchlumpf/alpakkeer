package alpakkeer.core.jobs.actor.context;

import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.values.Name;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduledExecutionReference<P> {

   Name name;

   P properties;

   boolean queue;

   CronExpression cron;

}

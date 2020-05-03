package alpakkeer.core.scheduler.model;

import alpakkeer.core.values.Name;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.quartz.CronTrigger;
import org.quartz.JobKey;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduledJob {

   Name name;

   String cronExpression;

   JobKey key;

   CronTrigger trigger;

}

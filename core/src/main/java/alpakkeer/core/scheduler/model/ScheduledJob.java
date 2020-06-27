package alpakkeer.core.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.quartz.CronTrigger;
import org.quartz.JobKey;

@Value
@AllArgsConstructor(staticName = "apply")
public class ScheduledJob {

   String name;

   String cronExpression;

   JobKey key;

   CronTrigger trigger;

}

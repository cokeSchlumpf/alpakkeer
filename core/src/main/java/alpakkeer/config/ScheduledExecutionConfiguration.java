package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Optional;
import alpakkeer.core.config.annotations.Value;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class ScheduledExecutionConfiguration {

   /**
    * A valid cron expression to schedule a job.
    */
   @Value("cron-expression")
   private String cronExpression;

   /**
    * Whether the execution should be queued if job is still running.
    */
   @Value("queue")
   private boolean queue;

   /**
    * Optional. The properties for the job execution, if not set, default-properties will be used.
    */
   @Optional
   @Value("properties")
   private String properties;

}

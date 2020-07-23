package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class JobConfiguration {

   /**
    * The name of the job
    */
   @Value("name")
   private String name;

   /**
    * Whether the job is enabled or not
    */
   @Value("enabled")
   private boolean enabled;

   /**
    * Whether to clear list of configured monitors when this configuration is applied
    */
   @Value("clear-monitors")
   private boolean clearMonitors;

   /**
    * Whether to clear existing schedule when this configuration is applied
    */
   @Value("clear-schedule")
   private boolean clearSchedule;

   /**
    * List of enabled monitors; possible values: `history`, `logging` & `prometheus`
    */
   @Value("monitors")
   private List<String> monitors;

   /**
    * A list of scheduled executions
    */
   @Value("schedule")
   private List<ScheduledExecutionConfiguration> schedule;

}

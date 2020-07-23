package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Optional;
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
public final class ProcessConfiguration {

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
    * Whether the job should be initialized
    */
   @Value("initialize-started")
   private boolean initializeStarted;

   /**
    * List of enabled monitors; possible values: `history`, `logging` & `prometheus`
    */
   @Value("monitors")
   private List<String> monitors;

}

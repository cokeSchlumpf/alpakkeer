package alpakkeer.config;

import alpakkeer.core.config.Configs;
import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class AlpakkeerConfiguration {

   private final String name;

   private final String environment;

   private final String version;

   @Value("show-banner")
   private final boolean showBanner;

   @Value("expose-config")
   private final boolean exposeConfig;

   private final ServerConfiguration api;

   @Value("context-store")
   private final ContextStoreConfiguration contextStore;

   private final MessagingConfiguration messaging;

   private final List<JobConfiguration> jobs;

   private final List<ProcessConfiguration> processes;

   public static AlpakkeerConfiguration apply() {
      return Configs.mapToConfigClass(AlpakkeerConfiguration.class, "alpakkeer");
   }

   public Optional<JobConfiguration> getJobConfiguration(String name) {
      if (jobs != null) {
         return jobs
            .stream()
            .filter(j -> j.getName().equals(name))
            .findFirst();
      } else {
         return Optional.empty();
      }
   }

   public Optional<ProcessConfiguration> getProcessConfiguration(String name) {
      if (processes != null) {
         return processes
            .stream()
            .filter(p -> p.getName().equals(name))
            .findFirst();
      } else {
         return Optional.empty();
      }
   }

}

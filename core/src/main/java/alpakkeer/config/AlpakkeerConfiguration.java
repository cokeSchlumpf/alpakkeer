package alpakkeer.config;

import alpakkeer.core.config.Configs;
import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlpakkeerConfiguration {

   private final String name;

   private final String environment;

   private final String version;

   @Value("show-banner")
   private final boolean showBanner;

   private final ServerConfiguration api;

   public static AlpakkeerConfiguration apply() {
      return Configs.mapToConfigClass(AlpakkeerConfiguration.class, "alpakkeer");
   }

}

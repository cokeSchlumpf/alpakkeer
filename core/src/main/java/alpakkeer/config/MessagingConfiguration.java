package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class MessagingConfiguration {

   /**
    * Specifies the used messaging-configuration type.
    * Possible values: in-memory, fs, kafka
    */
   private final String type;

   private final FileSystemStreamMessagingConfiguration fs;

   private final KafkaMessagingAdapterConfiguration kafka;

}

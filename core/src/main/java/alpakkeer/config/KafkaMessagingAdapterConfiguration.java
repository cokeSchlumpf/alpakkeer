package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import com.typesafe.config.Config;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class KafkaMessagingAdapterConfiguration {

   @Value("bootstrap-server")
   private String bootstrapServer;

   @Value("consumer")
   private Config consumer;

   @Value("producer")
   private Config producer;

}

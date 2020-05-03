package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServerConfiguration {

   private final String hostname;

   private final int port;

}

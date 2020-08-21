package alpakkeer.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import com.typesafe.config.Config;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class PostgresContextStoreConfiguration {

   private final String schema;

   private final String table;

   private final Config database;

}

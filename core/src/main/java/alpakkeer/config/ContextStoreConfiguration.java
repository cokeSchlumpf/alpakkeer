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
public final class ContextStoreConfiguration {

   /**
    * Specifies the used context store configuration type.
    * Possible values: in-memory, fs, db
    */
   String type;

   private final FileSystemContextStoreConfiguration fs;

   private final PostgresContextStoreConfiguration db;

}

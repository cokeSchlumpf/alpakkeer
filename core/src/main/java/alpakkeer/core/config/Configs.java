package alpakkeer.core.config;

import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import com.typesafe.config.*;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class wraps {@link com.typesafe.config.Config} and adds some features like environment and role awareness.
 *
 * @author Michael Wellner (info@michaelwellner.de).
 */
public final class Configs {

   /**
    * The name of the system variable which is used to store environment name
    */
   public static final String ENV_ALPAKKEER_ENVIRONMENT = "alpakkeer.environment";

   /**
    * The name of the system variable which is used to store the role of the ire instance
    */
   public static final String ENV_ALPAKKEER_ROLE = "alpakkeer.role";

   /**
    * The default environment name
    */

   public static final String LOCAL_ENVIRONMENT = "local";

   /**
    * The application configuration
    */
   public static final Config application;

   /**
    * Environment variables
    */
   public static final Config environment = ConfigFactory.systemEnvironment();

   /**
    * System variables/ JVM arguments
    */
   public static final Config system = ConfigFactory.systemProperties();

   /**
    * The name of the current environment, based on "alpakkeer.environment" system environment variable.
    */
   private static final String environmentName = system.hasPath(ENV_ALPAKKEER_ENVIRONMENT) ?
      system.getString(ENV_ALPAKKEER_ENVIRONMENT) : LOCAL_ENVIRONMENT;

   /**
    * The name of the current instance's role, based on "alpakkeer.role" system environment variable.
    */
   private static final String roleName = system.hasPath(ENV_ALPAKKEER_ROLE) ?
      system.getString(ENV_ALPAKKEER_ROLE) : null;

   /**
    * The logger for this class.
    */
   private static final Logger LOG = LoggerFactory.getLogger(Configs.class);

   static {
      /*
       * Configurations are loaded in the order as provided below, the latter ones override the ones before.
       */
      application = ConfigBuilder
         .create()
         .withEnvironmentAwareness()
         .withRoleAwareness()
         .withEnvironmentRoleAwareness()
         .withSecureConfig()
         .withEnvironmentVariables()
         .withSystemProperties()
         .getConfig();
   }

   /**
    * @return The name of the current environment, based on "alpakkeer.environment" system environment variable.
    */
   public static String getEnvironmentName() {
      return environmentName;
   }

   /**
    * @return The name of the current instance's role, based on "alpakkeer.role" system environment variable.
    */
   public static Optional<String> getRoleName() {
      return Optional.ofNullable(roleName);
   }

   /**
    * @return The name of the current environment, based on "alpakkeer.environment" system environment variable.
    */
   public static String environmentName() {
      return getEnvironmentName();
   }

   /**
    * @return The name of the current instance's role, based on "alpakkeer.role" system environment variable.
    */
   public static Optional<String> roleName() {
      return getRoleName();
   }

   /**
    * A simple helper function to return a {@link Config} object as human readable map.
    *
    * @param config The config to be transformed
    * @return A map including all configuration key-value-pairs
    */
   public static Map<String, String> asMap(Config config) {
      Map<String, String> configs = Maps.newLinkedHashMap();
      config.entrySet().forEach(entry -> {
         try {
            configs.put(entry.getKey(), entry.getValue().unwrapped().toString());
         } catch (ConfigException.NotResolved e) {
            configs.put(entry.getKey(), "<Not Resolved>");
         }
      });
      return configs;
   }

   /**
    * Returns a config object in a human-readable string.
    *
    * @param config The config object to print
    * @return A human readable string including all config values.
    */
   public static String asString(Config config) {
      StringBuilder sb = new StringBuilder();
      Map<String, String> configs = asMap(config);

      for (String key : configs.keySet().stream().sorted().collect(Collectors.toList())) {
         sb
            .append(key)
            .append(": ")
            .append(configs.get(key))
            .append("\n");
      }

      return sb.toString();
   }

   /**
    * This method maps a Config object into a POJO
    *
    * @param configClass The type of the POJO.
    * @param configPath  The config path of the config object from {@link Configs#application}
    * @param <T>         The type of the POJO.
    * @return The type of the POJO.
    */
   public static <T> T mapToConfigClass(Class<T> configClass, String configPath) {
      return mapToConfigClass(configClass, application.getConfig(configPath));
   }

   /**
    * This method maps a Config object into a POJO.
    *
    * @param configClass The type of the POJO.
    * @param config      The config which should be mapped.
    * @param <T>         The type of the POJO.
    * @return The type of the POJO.
    */
   public static <T> T mapToConfigClass(Class<T> configClass, Config config) {
      try {
         LOG.debug(String.format("Instantiating class '%s' and expanding configuration values.", configClass.getName()));

         final Constructor<T> constructor = configClass.getDeclaredConstructor();
         constructor.setAccessible(true);

         final T configObject = constructor.newInstance();

         FieldUtils
            .getAllFieldsList(configClass)
            .forEach(field -> {
               LOG.debug(String.format("Fetching configuration value for '%s.%s'", configClass.getSimpleName(), field.getName()));

               try {
                  final Value valueAnnotation = field.getAnnotation(Value.class);
                  final String configPath = valueAnnotation != null ? valueAnnotation.value() : field.getName();
                  Optional<Object> value = Optional.empty();

                  if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                     // Boolean, boolean
                     value = Optional.of(config.getBoolean(configPath));
                  } else if (Config.class.isAssignableFrom(field.getType())) {
                     // Config object
                     value = Optional.of(config.getConfig(configPath));
                  } else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                     // Double, double
                     value = Optional.of(config.getDouble(configPath));
                  } else if (field.getType().equals(Duration.class)) {
                     // Duration
                     value = Optional.of(config.getDuration(configPath));
                  } else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                     // Integer, int
                     value = Optional.of(config.getInt(configPath));
                  } else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                     // Long, long
                     value = Optional.of(config.getLong(configPath));
                  } else if (field.getType().equals(Object.class)) {
                     // Object
                     value = Optional.of(config.getAnyRef(configPath));
                  } else if (field.getType().equals(Number.class)) {
                     // Number
                     value = Optional.of(config.getNumber(configPath));
                  } else if (field.getType().equals(Period.class)) {
                     // Period
                     value = Optional.of(config.getPeriod(configPath));
                  } else if (field.getType().equals(String.class)) {
                     // String
                     value = Optional.of(config.getString(configPath));
                  } else if (field.getType().equals(TemporalAmount.class)) {
                     // TemporalAmount
                     value = Optional.of(config.getTemporal(configPath));
                  } else if (field.getType().getAnnotation(ConfigurationProperties.class) != null) {
                     // Any type annotated with ConfigurationProperties
                     value = Optional.of(Configs.mapToConfigClass(field.getType(), config.getConfig(configPath)));
                  } else if (field.getType().equals(List.class) && field.getGenericType() instanceof ParameterizedType) {
                     // Lists
                     Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

                     if (genericType.equals(Boolean.class)) {
                        // List<Boolean>
                        value = Optional.of(ImmutableList.copyOf(config.getBooleanList(configPath)));
                     } else if (genericType.equals(Config.class)) {
                        // List<Double>
                        value = Optional.of(ImmutableList.copyOf(config.getConfigList(configPath)));
                     } else if (genericType.equals(Double.class)) {
                        // List<Double>
                        value = Optional.of(ImmutableList.copyOf(config.getDoubleList(configPath)));
                     } else if (genericType.equals(Duration.class)) {
                        // List<Duration>
                        value = Optional.of(ImmutableList.copyOf(config.getDurationList(configPath)));
                     } else if (genericType.equals(Integer.class)) {
                        // List<Integer>
                        value = Optional.of(ImmutableList.copyOf(config.getIntList(configPath)));
                     } else if (genericType.equals(Long.class)) {
                        // List<Long>
                        value = Optional.of(ImmutableList.copyOf(config.getLongList(configPath)));
                     } else if (genericType.equals(Object.class)) {
                        // List<Object>
                        value = Optional.of(ImmutableList.copyOf(config.getAnyRefList(configPath)));
                     } else if (genericType.equals(Number.class)) {
                        // List<Double>
                        value = Optional.of(ImmutableList.copyOf(config.getNumberList(configPath)));
                     } else if (genericType.equals(String.class)) {
                        // List<String>
                        value = Optional.of(ImmutableList.copyOf(config.getStringList(configPath)));
                     } else if (genericType instanceof Class && ((Class<?>) genericType).getAnnotation(ConfigurationProperties.class) != null) {
                        value = Optional.of(ImmutableList.copyOf(config
                           .getConfigList(configPath)
                           .stream()
                           .map(c -> Configs.mapToConfigClass((Class<?>) genericType, c))
                           .collect(Collectors.toList())));
                     } else {
                        LOG.warn(String.format("Field '%s.%s' has unknown generic type for configuration: List<%s>", configClass.getSimpleName(), field.getName(), genericType.toString()));
                     }
                  } else {
                     LOG.warn(String.format("Field '%s.%s' has unknown type for configuration: %s", configClass.getSimpleName(), field.getName(), field.getType().getName()));
                  }

                  if (value.isPresent()) {
                     LOG.debug(String.format(
                        "Configuration value for '%s.%s': '%s'",
                        configClass.getSimpleName(),
                        field.getName(),
                        value.get()));

                     FieldUtils.writeField(field, configObject, value.get(), true);

                     if (Objects.isNull(FieldUtils.readField(field, configObject))) {
                        LOG.warn(String.format(
                           "Could not write field '%s' in config object of type '%s.",
                           field.getName(),
                           config.getClass().getName()));
                     }
                  } else {
                     LOG.warn(String.format(
                        "No value found for field '%s' with config path '%s' for class '%s'",
                        field.getName(),
                        configPath,
                        configObject.getClass().getSimpleName()));
                  }
               } catch (ConfigException.Missing e) {
                  if (field.getAnnotationsByType(alpakkeer.core.config.annotations.Optional.class) == null ||
                     field.getAnnotationsByType(alpakkeer.core.config.annotations.Optional.class).length == 0) {
                     LOG.error(String.format(
                        "No value found for field '%s' for class '%s'",
                        field.getName(),
                        configObject.getClass().getSimpleName()), e);

                     throw e;
                  } else {
                     LOG.warn(String.format(
                        "No value found for field '%s' for class '%s'",
                        field.getName(),
                        configObject.getClass().getSimpleName()), e);
                  }
               } catch (IllegalAccessException e) {
                  LOG.error(String.format(
                     "Exception occurred while fetching field '%s' for class '%s'",
                     field.getName(),
                     configObject.getClass().getSimpleName()), e);

                  throw new RuntimeException(e);
               }
            });

         return configObject;
      } catch (Exception e) {
         LOG.error("Exception while parsing configuration", e);
         throw new RuntimeException(e);
      }
   }

   /**
    * Helper class to allow declarative definition of config file loading order.
    */
   private static class ConfigBuilder {

      /**
       * The actual configuration which is built by the {@link ConfigBuilder} instance.
       */
      private final Config config;

      /**
       * Creates a new instance.
       *
       * @param config The actual configuration which is built by the {@link ConfigBuilder} instance
       */
      private ConfigBuilder(Config config) {
         this.config = config;
      }

      /**
       * Creates a new config builder; default configuration will be loaded first.
       * <p>
       * The environment name can be set in the system variable/ JVM argument {@link ConfigBuilder#ENV_ALPAKKEER_ENVIRONMENT}.
       *
       * @return The {@link ConfigBuilder} instance
       */
      static ConfigBuilder create() {
         LOG.info("Loading configuration from default resources 'application.conf' and 'reference.conf'");
         return new ConfigBuilder(ConfigFactory.load());
      }

      /**
       * Finalizes the configuration object and returns it.
       *
       * @return The {@link Config} instance assembled by the builder.
       */
      public Config getConfig() {
         return config.resolve();
      }

      /**
       * Creates a new configuration which overrides existing values with environment specific config file.
       *
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withEnvironmentAwareness() {
         final String envConfigFilePath = String.format("application.%s.conf", environmentName);
         return withResource(envConfigFilePath);
      }

      /**
       * Creates a new configuration which overrides existing values with configuration file from resources.
       *
       * @param resourceFileName The filename/ uri of the resource file
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withResource(String resourceFileName) {
         if (this.getClass().getResource(String.format("/%s", resourceFileName)) != null) {
            LOG.info(String.format("Loading configuration from resource file '%s'", resourceFileName));
            return withOverwrites(ConfigFactory.parseResources(resourceFileName));
         } else {
            LOG.debug(String.format("Configuration resource with name '%s' does not exist - Will be skipped", resourceFileName));
            return this;
         }
      }

      /**
       * Creates a new configuration which overrides existing values with new provided {@link Config}.
       *
       * @param config The config file which overwrites existing values
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withOverwrites(Config config) {
         return new ConfigBuilder(config.withFallback(this.config));
      }

      /**
       * Creates a new configuration which overrides existing values with environment specific config file.
       *
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withEnvironmentRoleAwareness() {
         if (roleName != null) {
            final String envConfigFilePath = String.format("application.%s-%s.conf", environmentName, roleName);
            return withResource(envConfigFilePath);
         } else {
            return this;
         }
      }

      /**
       * Creates a new configuration extended by values found in environment variables.
       *
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withEnvironmentVariables() {
         LOG.info("Loading configuration values from environment variables");
         return withOverwrites(ConfigFactory.systemEnvironment());
      }

      /**
       * Creates a new configuration which overrides existing values with role specific config file.
       * <p>
       * The role can be defined in the system variable/ JVM argument {@link ConfigBuilder#ENV_ALPAKKEER_ROLE}.
       *
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withRoleAwareness() {
         if (roleName != null) {
            final String roleConfigFilePath = String.format("application.%s.conf", roleName);
            return withResource(roleConfigFilePath);
         } else {
            return this;
         }
      }

      /**
       * Checks whether a file 'application.secure.conf' exists in the application's runtime directory, if yes
       * it uses this file as overwrites for the existing config.
       *
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withSecureConfig() {
         final String executionDirectory = system.getString("user.dir");
         final String secureConfigFilePath = String.format("%s/application.secure.conf", executionDirectory);
         final File secureConfigFile = new File(secureConfigFilePath);

         if (secureConfigFile.exists()) {
            LOG.info(String.format("Loading configuration from secure configuration file '%s'", secureConfigFilePath));
            return withOverwrites(ConfigFactory.parseFile(secureConfigFile));
         } else {
            LOG.debug(String.format("Configuration file '%s' does not exist - Will be skipped", secureConfigFilePath));
            return this;
         }
      }

      /**
       * Overwrites existing values with properties passed as system properties.
       *
       * @return A new {@link ConfigBuilder} instance
       */
      public ConfigBuilder withSystemProperties() {
         LOG.info("Loading configuration values from system properties");
         return withOverwrites(ConfigFactory.systemProperties());
      }

   }

}


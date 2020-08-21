package alpakkeer.core.jobs.context;

import alpakkeer.javadsl.AlpakkeerBaseRuntime;

public final class  ContextStores {

   private ContextStores() {

   }

   public static ContextStore createFromConfiguration(AlpakkeerBaseRuntime runtime) {
      var configuration = runtime.getConfiguration().getContextStore();
      var om = runtime.getObjectMapper();
      var system = runtime.getSystem();

      switch (configuration.getType().toLowerCase()) {
         case "in-memory":
         case "in-mem":
         case "default":
            return InMemoryContextStore.apply(om);
         case "fs":
         case "file-system":
         case "files":
            return FileSystemContextStore.apply(configuration.getFs(), om);
         case "postgres":
         case "db":
         case "database":
            return PostgresContextStore.apply(configuration.getDb(), system, om);
         default:
            throw new RuntimeException(String.format("Unknown context store type `%s`. " +
               "Allowed values are `in-memory`, `file-system`", configuration.getType()));
      }
   }

}

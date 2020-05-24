package alpakkeer.api;

import alpakkeer.config.AlpakkeerConfiguration;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;

public final class AlpakkeerOpenApi {

   private AlpakkeerOpenApi() {

   }

   public static OpenApiPlugin apply(AlpakkeerConfiguration config) {
      var info = new Info()
         .version(config.getVersion())
         .title(config.getName())
         .description("Alpakkeer REST API to run and operate streams.");

      var options = new OpenApiOptions(info)
         .path("/api/v1")
         .swagger(new SwaggerOptions("/").title(String.format("%s Open API Documentation", config.getName())));

      return new OpenApiPlugin(options);
   }

}

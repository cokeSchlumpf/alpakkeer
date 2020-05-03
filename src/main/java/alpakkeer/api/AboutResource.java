package alpakkeer.api;

import alpakkeer.config.AlpakkeerConfiguration;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public final class AboutResource {

   @Value
   @AllArgsConstructor(staticName = "apply")
   public static class About {

      String environment;

      String version;

   }

   private final AlpakkeerConfiguration config;

   @OpenApi(
      summary = "About this application",
      description = "Returns basic meta information of the application.",
      responses = {
         @OpenApiResponse(status = "200", content = @OpenApiContent(from = About.class))
      })
   public void getAbout(Context ctx) {
      ctx.json(About.apply(config.getEnvironment(), config.getVersion()));
   }

}

package alpakkeer.api;

import alpakkeer.config.AlpakkeerConfiguration;
import alpakkeer.core.resources.Resources;
import alpakkeer.core.scheduler.CronScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public final class AlpakkeerAPI {

   private final Javalin javalin;

   public static AlpakkeerAPI apply(
      AlpakkeerConfiguration config, CronScheduler scheduler, Resources resources, ObjectMapper om) {

      var about = new AboutResource(config);
      var jobs = JobsResource.apply(resources);
      var admin = new AdminResource(scheduler);

      JavalinJackson
         .configure(om);

      Javalin app = Javalin
         .create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.registerPlugin(AlpakkeerOpenApi.apply(config));
         })
         .get("/api/v1/about", about::getAbout)
         .get("/api/v1/jobs", jobs::getJobs)
         .get("/api/v1/jobs/:name", jobs::getJob)
         .get("/api/v1/admin/crontab", admin::getJobs)
         .start(config.getApi().getHostname(), config.getApi().getPort());

      return AlpakkeerAPI.apply(app);
   }

   public void stop() {
      javalin.stop();
   }

}

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

      var jobs = new JobsResource(resources, om);
      var admin = new AdminResource(config, scheduler);

      JavalinJackson.configure(om);

      Javalin app = Javalin
         .create(cfg -> {
            cfg.showJavalinBanner = false;
            cfg.registerPlugin(AlpakkeerOpenApi.apply(config));
         })

         // Jobs
         .get("/api/v1/jobs", jobs.getJobs())
         .get("/api/v1/jobs/:name", jobs.getJob())
         .options("/api/v1/jobs/:name", jobs.getTriggerJobExample())
         .post("/api/v1/jobs/:name", jobs.triggerJob())

         // Admin
         .get("/api/v1/about", admin.getAbout())
         .get("/api/v1/admin/crontab", admin.getJobs())

         // run ...
         .start(config.getApi().getHostname(), config.getApi().getPort());

      return AlpakkeerAPI.apply(app);
   }

   public void stop() {
      javalin.stop();
   }

}

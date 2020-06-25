package alpakkeer.api;

import alpakkeer.config.RuntimeConfiguration;
import alpakkeer.core.resources.Resources;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(staticName = "apply", access = AccessLevel.PRIVATE)
public final class AlpakkeerAPI {

   private final Javalin javalin;

   public static AlpakkeerAPI apply(
      RuntimeConfiguration runtime,
      Resources resources) {

      var jobs = new JobsResource(resources, runtime.getObjectMapper());
      var processes = new ProcessesResource(resources);
      var admin = new AdminResource(runtime.getConfiguration(), runtime.getScheduler());
      var metrics = new MetricsResource(resources, runtime);

      JavalinJackson.configure(runtime.getObjectMapper());

      runtime.getApp()
         // Jobs
         .get("/api/v1/jobs", jobs.getJobs())
         .get("/api/v1/jobs/:name", jobs.getJob())
         .get("/api/v1/jobs/:name/sample", jobs.getTriggerJobExample())
         .post("/api/v1/jobs/:name", jobs.triggerJob())
         .delete("/api/v1/jobs/:name", jobs.stop())

         // Processes
         .get("/api/v1/processes", processes.getProcesses())
         .get("/api/v1/processes/:name", processes.getProcess())
         .delete("/api/v1/processes/:name", processes.stop())
         .post("/api/v1/processes/:name", processes.start())

         // Metrics
         .get("/api/v1/metrics", metrics.getPrometheusMetrics())
         .get("/api/v1/metrics/search", metrics.getGrafanaMetrics())
         .post("/api/v1/metrics/search", metrics.search())
         .post("/api/v1/metrics/query", metrics.query())
         .get("/api/v1/metrics/annotations", metrics.searchAnnotations())
         .post("/api/v1/metrics/annotations", metrics.queryAnnotations())
         .head("/api/v1/metrics/annotations", metrics.getAnnotationsHeader())

         // Admin
         .get("/api/v1/about", admin.getAbout())
         .get("/api/v1/admin/crontab", admin.getJobs());

      return AlpakkeerAPI.apply(runtime.getApp());
   }

   public void stop() {
      javalin.stop();
   }

}

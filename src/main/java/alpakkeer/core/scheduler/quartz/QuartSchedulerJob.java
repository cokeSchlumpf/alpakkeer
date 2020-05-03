package alpakkeer.core.scheduler.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class QuartSchedulerJob implements Job {

   @Override
   public void execute(JobExecutionContext context) {
      var jdm = context.getJobDetail().getJobDataMap();
      var job = (Runnable) jdm.get(QuartzCronScheduler.CONTEXT_KEY);
      job.run();
}

}

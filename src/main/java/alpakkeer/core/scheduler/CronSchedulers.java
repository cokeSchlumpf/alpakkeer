package alpakkeer.core.scheduler;

import alpakkeer.core.scheduler.quartz.QuartzCronScheduler;

public final class CronSchedulers {

   private CronSchedulers() {

   }

   public static CronScheduler apply() {
      return QuartzCronScheduler.apply();
   }

}

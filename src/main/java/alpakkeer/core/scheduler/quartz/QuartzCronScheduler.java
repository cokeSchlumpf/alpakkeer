package alpakkeer.core.scheduler.quartz;

import akka.Done;
import alpakkeer.core.scheduler.CronScheduler;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.scheduler.model.JobDetails;
import alpakkeer.core.scheduler.model.ScheduledJob;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Name;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuartzCronScheduler implements CronScheduler {

   static final String CONTEXT_KEY = "JOB_CONTEXT";

   private final Scheduler scheduler;

   private final AtomicReference<Map<Name, ScheduledJob>> jobs;

   public static QuartzCronScheduler apply() {
      var properties = new Properties();
      properties.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, String.format("scheduler-%s", UUID.randomUUID().toString()));
      properties.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, RAMJobStore.class.getName());
      properties.setProperty("org.quartz.threadPool.threadCount", String.valueOf(10));

      var scheduler = Operators.suppressExceptions(() -> new StdSchedulerFactory(properties).getScheduler());
      Operators.suppressExceptions(scheduler::start);
      return new QuartzCronScheduler(scheduler, new AtomicReference<>(new HashMap<>()));
   }

   @Override
   public CompletionStage<Done> schedule(Name name, CronExpression cron, Runnable job) {
      jobs.getAndUpdate(jobs -> {
         var key = new JobKey(name.getValue());
         var triggerKey = new TriggerKey(name.getValue());
         var jobData = new JobDataMap();
         var jobBuilder = JobBuilder.newJob(QuartSchedulerJob.class);

         jobData.put(CONTEXT_KEY, job);

         var jobDetail = jobBuilder
            .usingJobData(jobData)
            .withIdentity(key)
            .build();

         var trigger = TriggerBuilder
            .newTrigger()
            .startNow()
            .withIdentity(triggerKey)
            .forJob(jobDetail)
            .withSchedule(CronScheduleBuilder.cronSchedule(cron.getValue()))
            .build();

         Operators.suppressExceptions(() -> scheduler.scheduleJob(jobDetail, trigger));

         if (jobs.containsKey(name)) {
            Operators.suppressExceptions(() -> this.scheduler.deleteJob(jobs.get(name).getKey()));
         }

         var scheduled = ScheduledJob.apply(name, cron.getValue(), key, trigger);
         jobs.put(name, scheduled);

         return jobs;
      });

      return CompletableFuture.completedFuture(Done.getInstance());
   }

   @Override
   public CompletionStage<Optional<JobDetails>> getJob(Name name) {
      var result = Optional
         .ofNullable(jobs.get().get(name))
         .map(job -> JobDetails.apply(job, scheduler));

      return CompletableFuture.completedFuture(result);
   }

   @Override
   public CompletionStage<List<JobDetails>> getJobs() {
      var result = jobs
         .get()
         .values()
         .stream()
         .map(job -> JobDetails.apply(job, scheduler))
         .collect(Collectors.toList());

      return CompletableFuture.completedFuture(result);
   }

   @Override
   public CompletionStage<Done> remove(Name name) {
      jobs.getAndUpdate(jobs -> {
         if (jobs.containsKey(name)) {
            Operators.suppressExceptions(() -> scheduler.deleteJob(jobs.get(name).getKey()));
         }

         jobs.remove(name);

         return jobs;
      });

      return CompletableFuture.completedFuture(Done.getInstance());
   }

   @Override
   public CompletionStage<Done> terminate() {
      Operators.suppressExceptions((Operators.ExceptionalRunnable) scheduler::shutdown);
      return CompletableFuture.completedFuture(Done.getInstance());
   }

}

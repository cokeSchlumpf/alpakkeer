package sample;

import alpakkeer.Alpakkeer;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;

import java.time.Duration;

public class Application {

   public static void main(String ...args) {
      var job = JobDefinitions
         .create("sample-job")
         .withRunnable(() -> {
            System.out.println("Starting job!");
            Thread.sleep(5000);
            System.out.println("Finishing job!");
         })
         .withScheduledExecution(CronExpression.everySeconds(3))
         .build();

      var alpakkeer = Alpakkeer
         .create()
         .withJob(job)
         .start();

      Operators.suppressExceptions(() -> Thread.sleep(Duration.ofMinutes(10).toMillis()));

      alpakkeer.stop();
   }

}

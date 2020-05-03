package sample;

import alpakkeer.Alpakkeer;
import alpakkeer.core.jobs.JobDefinitions;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.Duration;

public class Application {

   @Value
   @AllArgsConstructor(staticName = "apply")
   @NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
   public static class MyProperties {
      String text;
   }

   public static void main(String ...args) {
      var job = JobDefinitions
         .create("sample-job")
         .withProperties(MyProperties.apply("hello"))
         .withRunnableFromProperties(p -> {
            System.out.println("Starting job - text: " + p.getText());
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

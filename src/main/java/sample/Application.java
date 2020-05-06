package sample;

import alpakkeer.Alpakkeer;
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

   public static void main(String... args) {
      var alpakkeer = Alpakkeer
         .create()
         .withJob(builder -> builder
            .create("sample-job", MyProperties.apply("hello"))
            .run((s, p) -> {
               System.out.println("Starting job - text: " + p.getText());
               Thread.sleep(5000);
               System.out.println("Finishing job!");
            })
            .withHistoryMonitor(3)
            .withLoggingMonitor()
            .withScheduledExecution(CronExpression.everyMinutes(1))
            .build())
         .start();

      Operators.suppressExceptions(() -> Thread.sleep(Duration.ofMinutes(300).toMillis()));

      alpakkeer.stop();
   }

}

# Alpakkeer

**Note:** The project is under active development and not ready for use yet.

Alpakkeer is an opinionated toolkit to build, run and monitor light-weight integration applications (not necessarily) based on [Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html) and [Alpakka](https://doc.akka.io/docs/alpakka/current/index.html). 

```java
public class HelloWorld {
   public static void main(String[] args) {
      Alpakkeer
       .create()
       .withJob(builder -> builder
          .create("sample-job")
          .runGraph((id, sb) -> SampleStreams
             .tweets()
             .via(sb.createCheckpointMonitor("tweet-count"))
             .to(Sink.ignore()))
          .withPrometheusMonitor()
          .withScheduledExecution(CronExpression.everyMinute())
          .build())
       .start();     
   }
}
```

This small application starts a server with a REST API to control your Akka Streams based jobs including metrics to monitor the stream with Prometheus and Grafana. Run the application and visit [http://localhost:8042](http://localhost:8042/) to see all available endpoints and functions.

Continue with [Tutorials](#TODO) or see the detailed [Docs](#TODO) to discover more. 

## Main Features 

Alpakkeer bundles various libraries and components to quickly build, run and operate light-weight integration applications:

* A Web Server based on [Javalin](#) to provide simple access via REST APIs to control and monitor jobs and to serve Metrics for Prometheus and Grafana. [More Details ...](#)
 
* Configuration Management based on [Typesafe Configuration](#...) including some extensions for environment-based configurations and automatic mapping to POJOs. [More Details ...](#)

* Prometheus Client to record application and stream metrics. Alpakkeer also provides custom FlowStages to argument your stream with Akka Streams specific metrics. [More Details ...](#)

All components are made simply composable and accessible via an easy to use Java* DSL.

<sup>\* Alpakkeer is intended to be used with Java. Although not intended, the DSL can be used with Scala 2.13 as well, Scala 2.12 is not supported. An adapted Scala DSL is not planned yet.</sup> 
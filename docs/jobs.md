# Jobs

Alpakkeer jobs are wrappers around streams with sources which have a definite count of records to process (e.g. JDBC sources, directory listings, etc.). Jobs can be triggered following a schedule, manually via API call or programmatically via its API. 

Alpakkeer ensures that different executions of a job are not running in parallel. If a job is already running when it is triggered, it can be either queued or discarded.

When triggering jobs, properties can be passed to it to customize its behaviour; jobs also have a context to share state between executions of a job.

## Defining Jobs

A job can be defined using a builder API.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create("hello-world")
        .runGraph(b -> Source
            .single("Hello World")
            .toMat(Sink.foreach(System.out::println), Keep.right()))
    ```

=== "Scala"

    ```scala
    // TODO
    ```

## Properties and Context

Every job has Properties type and a Context type. By default these types are set to `alpakkeer.core.values.Nothing` and `akka.Done`.

Job **poperties** are the arguments which are passed to a job when it is triggered. The properties can be used while constructing the stream. Every job has default properties which are used if no properties are explicitly passed while triggering. 

The job's **context** can be seen as the shared state between job executions. The current context of the job is passed to the stream constructor, when the job is triggered - just like the properties. The result aka the materialzed value  of the job execution is the updated context which will be passed to the next execution. To make the context persistent between restarts of the application, Alpakkeer by default offers file-based and database based context persistence.

The job types can be defined by passing default properties and the initial context to the job builder API:

=== "Java"

```java
@Value
@NoArgsConstructor
@AllArgsConstructor(staticName = "apply")
public class Properties {
    final String name;
}

@NoArgsConstructor
@AllArgsConstructor(staticName = "apply")
public class Context {
    Context addGreeted(String name) {
        // ...
    }
}

alpakkeer.withJob(jobs -> jobs
        .create(
            "hello-world", 
            Properties.apply("Anonymous"), 
            Context.apply())
        .runGraph(b -> {
            var name = b.getProperties().getName();
            var hello = "Hello " + name + "!";
            var nextContext = b.getContext().addGreeted(name);

            return Source
                .single()
                .toMat(Sink.foreach(System.out::println), Keep.right())
                .mapMaterializedValue(done -> done.thenApply(i -> nextContext));
        })
```

To enable serialization for the REST API as well as for Alpakkeer's built-in persistence providers, make sure that the class is serializable and de-serializable with Jackson.

## Triggering Jobs

To trigger a job via the REST API with its default properties, call:

```bash
curl \
    --header "Content-Type: application/json" \
    --request POST \
    http://localhost:8042/api/v1/jobs/hello-world
```

To trigger the job with properties, just pass them within the request body:

```bash
curl \
    --header "Content-Type: application/json" \
    --request POST \
    --data '{"queue": true, "properties": { "name": "Edgar"} }' \
    http://localhost:8042/api/v1/jobs/hello-world
```

Jobs can also be triggered programmatically:

```java
var app = Alpakkeer.create()/* ... job definitions ... */.start();
var job = app.getResources().<Context, Properties>getJob("hello-world");

job.start();
job.start(false); // do not queue
job.start(false, Properties.apply("Emil"));
```

## Scheduling Jobs

Jobs can be configured to be triggered following a cron expression based schedule.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withScheduledExecution(
            CronExpression.apply("0 0 12 * * ?"), 
            Properties.apply("Emil"), 
            false))
    ```

=== "Scala"

    ```scala

    ```

`CronExpression` offer various helper methods to simplify the creation of the expression, e.g. `CronExpression.everySeconds(10)`. You may register multiple expressions to schedule different kinds of executions for the job, e.g. an incremental-load every day and a full-load every month for crawler applications.

## Logging & Monitoring

Jobs can be monitored using the interface `alpakkeer.core.jobs.monitor.JobMonitor`. The monitor can implement methods to collect information about stream execution time, exceptions, results and other stream metrics collected with [monitoring flows](#TODO) from Alpakkeer stream utilities.

=== "Java"

    ```java
    public interface JobMonitor<P, C> {
        void onTriggered(String executionId, P properties);
        void onStarted(String executionId);
        void onFailed(String executionId, Throwable cause);
        void onCompleted(String executionId, C result);
        void onCompleted(String executionId);
        void onStopped(String executionId, C result);
        void onStopped(String executionId);
        void onQueued(int newQueueSize);
        void onEnqueued(int newQueueSize);
        CompletionStage<Optional<Object>> getStatus();

        void onStats(String executionId, String name, CheckpointMonitor.Stats statistics);
        void onStats(String executionId, String name, LatencyMonitor.Stats statistics);
    }
    ```

The method `getStatus` can return an arbitary object which will be part of the job status in the job's REST API. Alpakkeer comes with a few built-in monitors:

The **LoggingMonitor** will log all information into the SLF4J logger.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withLoggingMonitor())
    ```

=== "Scala"

    ```scala

    ```

The **PrometheusMonitor** will feed all information into Alpakkeer's Prometheus collector registry. These metrics can then be accessed by Prometheus via `/metrics`.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withPrometheusMonitor())
    ```

=== "Scala"

    ```scala

    ```

The **HistoryMonitor** stores information of executions up-to a define age or count of executions. The information can be kept in memory or can be stored in a database.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withHistoryMonitor())
    ```

=== "Scala"

    ```scala

    ```

A job can be configured to have multipe monitors of course.


## Persisting Context

By default all job contexts are kept in-memory only. The context store can be defined in the application configuration:

=== "application.conf"

    ```hocon
    alpakkeer.contexts.type = "in-memory" 
    ```

Other possible values are `fs` (file-system) or `db` (database).

The context store can also be configured programmatically:

=== "Java"

    ```java
    Alpakkeer
         .create()
         .configure(r -> r.withContextStore(ContextStores.fileSystem()))
         .withJob(/* ... */)
         /* ... */
         .start()
    ```

=== "Scala"

    ```scala

    ```

## Updating Job Context during Runtime

When implementing a job which processes a huge amount of documents or where a long runtime can be expected, it might make sense to update the context already during job runtime. 

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create("hello-world")
        .runGraph(b -> Documents
            .getDocuments(from = context.date)
            .via(/* some persisting flow */)
            .statefulMapConcat(() -> {
                LocalDateTime processed = LocalDateTime.MIN;
                int count = 0;
                return record -> {
                    processed = record.updated;

                    if (count < 1_000) {
                        count++;
                        return List.of();
                    } else {
                        count = 0;
                        return List.of(processed);
                    }
                }
            })
            .mapAsync(latest -> b.setContext(Context.apply(latest)))
            .toMat(Sink.ignore, Keep.right()))
    ```

=== "Scala"

    ```scala

    ```

As in the assemble above the context should not be updated for every single document, but for some kind of batches. In the example the record is updated every thousand records. It is also important to only update the context with information for already processed records.

## Disabling Jobs

Jobs can be disabled programmatically. This feature can be used to disable jobs by application configurations. See also [Deployment](/deployment) for more information.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .disable(config.isJobEnabled()))
    ```

=== "Scala"

    ```scala

    ```

## Custom API endpoints

The default REST API can be extended with custom REST endpoints, e.g. to trigger the job with custom inputs than job properties.

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withApiEndpoint((api, job) -> {
            api.get("process", ctx -> {
               ctx.result(job.start().toCompletableFuture());
            });
        }))
    ```

=== "Scala"

    ```scala

    ```

The `api` object is an instance of [Javalin](https://javalin.io), you can also extend the endpint with Open API documentation. See [Javalin Docs](https://javalin.io/plugins/openapi#getting-started) for detailed information.

## Configuration Overrides

Job configurations can be overridden or extended by (environment-specific) configurations. This feature might be useful if the stream should run with different configurations (e.g. schedule) in various environments. The default configuration key is `alpakkeer.jobs`:

=== "application.conf"

    ```
    alpakkeer.jobs = [
        {
            name = "hello-world"
            enabled = true
            clear-monitors = false
            clear-schedule = true
            monitors = ["logging"]
            schedule = [
                {
                    cron-expression = "0 0 12 * * ?"
                    queue = false
                    properties = """{"queue": true, "properties": { "name": "Edgar"} }"""
                },
                {
                    cron-expression = "0 0 6 * * ?"
                    queue = false
                    properties = """{"queue": true, "properties": { "name": "Antigone"} }"""
                }
            ]
        }
    ]
    ```

In the job definition, these configurations can be used:

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withConfiguration())
    ```

=== "Scala"

    ```scala

    ```

## REST API

Jobs can be managed using Alpakkeers REST API. The endpoints for jobs are:

* `GET /api/v1/jobs` - List all configured jobs
* `GET /api/v1/jobs/{name}` - Get job details
* `POST /api/v1/jobs/{name}` - Trigger job execution
* `DELETE /api/v1/jobs/{name}` - Stop current job execution
* `GET /api/v1/jobs/{name}/sample` - Get example properties which need to be posted when triggering the job

For more details, visit the Open API spec on a running instance, e.g. [http://localhost:8042](http://localhost:8042).
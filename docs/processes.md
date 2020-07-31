# Processes

Processes are wrappers around streams with sources which have an indefinte count of records to process (e.g. Kafka consumers, directory change listeners, ...). Processes are usually running continously as long as the application is up and running, but they can also be stopped and restarted manually. If the stream fails, Alpakkeer will automatically try to restart the stream with a restart-with-backoff mechanism.

## Defining Processes

A process can be defined using a builder API.

=== "Java"

    ```java
    alpakkeer.withProcess(process -> process
        .create("hello-world")
        .runGraph(b -> Source
            .repeat("Hello World")
            .throttle(1, Duration.ofSeconds(1))
            .toMat(Sink.foreach(System.out::println), Keep.right())))
    ```

=== "Scala"

    ```scala
    // TODO
    ```

## Starting and Stopping

Processes can be started and stopped via REST API.

=== "Start"

    ```bash
    curl \
        --header "Content-Type: application/json" \
        --request POST \
        http://localhost:8042/api/v1/processes/hello-world
    ```

=== "Stop"

    ```bash
    curl \
        --header "Content-Type: application/json" \
        --request DELETE \
        http://localhost:8042/api/v1/processes/hello-world
    ```

Starting and stopping is also possible programmatically:

=== "Java"

    ```java
    var app = Alpakkeer.create()/* ... process definitions ... */.start();
    var process = app.getResources().getProcess("hello-world");

    process.stop();
    process.start();
    ```

=== "Scala"

    ```scala
    ```

Processes are by default started upon application startup, this behavior can be disabled:

=== "Java"

    ```java
    alpakkeer.withProcess(processes -> processes
        .create(/* ... */)
        .runGraph(/* ... */)
        .initializeStopped())
    ```

=== "Scala"

    ```scala

    ```

## Configuring Restart Backoffs

Alpakkeer will try to keep streams running as long as the process is not stopped. Thus Alpakkeer restarts gracefully repeated streams, when a stream fails with an exception it is also restarted, but with an increasing backoff-timeout. The restart timeouts can be defined:

=== "Java"

    ```java
    alpakkeer.withProcess(processes -> processes
        .create(/* ... */)
        .runGraph(/* ... */)
        .withCompletionRestartBackoff(Duration.ofMinutes(5))
        .withInitialRetryBackoff(Duration.ofSeconds(30))
        .withRetryBackoffResetTimeout(Duration.ofMinutes(10)))
    ```

=== "Scala"

    ```scala

    ```

The `retry-backoff-reset-timeout` specifies the duration how long a stream must be running without failure to reset the retry-backoff to its initial value.

## Logging & Monitoring

Processes can be monitored using the interface `alpakkeer.core.processes.monitor.ProcessMonitor`. The monitor can implement methods to collect information about exceptions, completions, start- and stop-events and other stream metrics collected with [monitoring flows](#TODO) from Alpakkeer stream utilities.

=== "Java"

    ```java
    public interface ProcessMonitor {
        void onStarted(String executionId);
        void onFailed(String executionId, Throwable cause, Instant nextRetry);
        void onCompletion(String executionId, Instant nextStart);
        void onStopped(String executionId);
        CompletionStage<Optional<Object>> getStatus();

        void onStats(String executionId, String name, CheckpointMonitor.Stats statistics);
        void onStats(String executionId, String name, LatencyMonitor.Stats statistics);
    }
    ```

The method `getStatus` can return an arbitary object which will be part of the stream status in the processes' REST API. Alpakkeer comes with a few built-in monitors:

The **LoggingMonitor** will log all information into the SLF4J logger.

=== "Java"

    ```java
    alpakkeer.withProcess(processes -> processes
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
    alpakkeer.withProcess(processes -> processes
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
    alpakkeer.withProcess(processes -> processes
        .create(/* ... */)
        .runGraph(/* ... */)
        .withHistoryMonitor())
    ```

=== "Scala"

    ```scala

    ```

Aa process can be configured to have multipe monitors of course.

## Disabling Processes

Processes can be disabled programmatically. This feature can be used to disable processes by application configurations. See also [Deployment](/deployment) for more information.

=== "Java"

    ```java
    alpakkeer.withProcess(processes -> processes
        .create(/* ... */)
        .runGraph(/* ... */)
        .disable(config.isProcessEnabled()))
    ```

=== "Scala"

    ```scala

    ```

## API endpoints

The default REST API can be extended with custom REST endpoints.

=== "Java"

    ```java
    alpakkeer.withProcess(processes -> processes
        .create(/* ... */)
        .runGraph(/* ... */)
        .withApiEndpoint((api, process) -> {
            api.get("process", ctx -> {
                process.stop()
                ctx.result("stopped");
            });
        }))
    ```

=== "Scala"

    ```scala

    ```

The `api` object is an instance of [Javalin](https://javalin.io), you can also extend the endpint with Open API documentation. See [Javalin Docs](https://javalin.io/plugins/openapi#getting-started) for detailed information.

## Configuration Overrides

Process configurations can be overridden or extended by (environment-specific) configurations. This feature might be useful if the stream should run with different configurations (e.g. different backoff-timeouts) in various environments. The default configuration key is `alpakkeer.processes`:

**TODO** Backoff Configuration

=== "application.conf"

    ```
    alpakkeer.processes = [
        {
            name = "hello-world"
            enabled = true
            clear-monitors = false
            monitors = ["logging"]
            initialize-started = false
        }
    ]
    ```

In the process definition, these configurations can be used:

=== "Java"

    ```java
    alpakkeer.withProcess(processes -> processes
        .create(/* ... */)
        .runGraph(/* ... */)
        .withConfiguration())
    ```

=== "Scala"

    ```scala

    ```

## REST API

Processes can be managed using Alpakkeers REST API. The endpoints for processes are:

* `GET /api/v1/processes` - List all defined processes
* `GET /api/v1/processes/{name}` - Get process details
* `POST /api/v1/processes/{name}` - Start the process
* `DELETE /api/v1/processes/{name}` - Stop the process

For more details, visit the Open API spec on a running instance, e.g. [http://localhost:8042](http://localhost:8042).
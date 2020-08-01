# Stream Utilities

Alpakkeer offers easy accessible building-blocks for composing reactive streams with Akka Streams. These utilities can be accessed via the builder object which is passed to `runGraph`-methods of the Job- and Process-Builders.

=== "Java"

    ```java
    alpakkeer.withProcess(process -> process
        .create("hello-world")
        .runGraph(streamBuilder -> streamBuilder
            .messaging()
            .recordsSource("topic", String.class)
            .throttle(1, Duration.ofSeconds(1))
            .toMat(Sink.foreach(System.out::println), Keep.right())))
    ```

=== "Scala"

    ```scala
    // TODO
    ```

## Messaging

The messaging building-blocks are an abstraction over messaging technologies, such as Kafka, Google PubSub or JMS. Alpakkeer also offers simple built-in messaging in-memory and file-system implementations which can be used for local development or simple systems.

The abstraction allows developers to develop, run and test a stream-topology fast and quickly on the local machine w/o involving other technologies like Docker etc. for running Kafka, JMS brokers, etc.. Also automated component tests can be written quite easily using in-memory implementations.

The messaging implementations take also care about re-processing failed (not committed) messages and committing offsets or elements (depending on the technology).

### Supported Messaging Patterns

Alpakkeer supports the **message-queue** and **publish-subscribe** messaging pattern. While message-queue has a queue where each message is processed **at-least-once** from any consumer, the publish-subscribe pattern has a topic where each message is processed **at-least-once** by every listening consumer. Details like message retention etc. are specific to messaging providers. 

By default messages which is sent via the messaging layer is serialized and deserialized using Alpakkeer's internal `ObjectMapper` instance, thus all messages will be serialized and sent as JSON messages. While this is not the most performant way of serializing and deserializing, it's good enough for most real-world usecases. Nevertheless, custom messaging providers may also user other serializers and deserializers.

Every message must have a unique-key. If there is no meaningful key available in the used data model, Alpakkeer creates a unique key automatically.

### Producing Messages

A sink to a messaging queue or topic can be created as follows:

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create("hello-world")
        .runGraph(b -> Source
            .single("Hello World")
            .toMat(
                b.messaging().itemsSink("some_topic", s -> s.toLowerCase()), 
                Keep.right()))
    ```

=== "Scala"

    ```scala
    // TODO
    ```

The 2nd parameter of `itemsSink` is a function which maps an item to its id. If the function is not defined, Aalpakkeer will create a randomized unique id.

Single messages can also be produced without a stream, e.g. when a message should be created upon a Web API call:

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create(/* ... */)
        .runGraph(/* ... */)
        .withApiEndpoint((app, api, job) -> {
            api.get("process", ctx -> {
                var request = ctx.bodyAsClass(Request.class);
                var result = app
                    .getMessaging()
                    .putItem("input", request)
                    .thenApply(done -> "Ok")

                ctx.result(result.toCompletableFuture());
            });
        }))
    ```

=== "Scala"

    ```scala
        
    ```

### Consuming Messages

Messages can be consumed by using `Source`-methods from Alpakkeer's messaging interface. Optionally a consumer group name can be passed to each of the  sources to identify multiple consumers. If the consumer group name is ommitted, a default will be used. At runtime there might be multipe instances using the same consumer group name, the whole group will process each message at least once.

The output of messaging sources is always a `Record`. A record is an envelope around the actual message type which carries context information and the actual message.

Each record should be committed, when it is successfully processed. All Alpakkeer sinks will automatically commit processed messages, but if no built-in sink is used, the record must be committed manually.

=== "Java"

    ```java
    alpakkeer.withProcess(process -> process
        .create("hello-world")
        .runGraph(b -> b
            .getMessaging()
            .recordsSource("process-input", classOf[Document], "processors")
            .map(record -> record.withValue(record.getValue().getName()))
            .toMat(
                b.messaging().recordsSink("some_topic"), 
                Keep.right())))
    ```

=== "Scala"

    ```scala

    ```

### Consuming and Producing Messages

When a stream consumes, processes messaages and writes them to another topic or queue, it should use a record sink.

=== "Java"

    ```java
    alpakkeer.withProcess(process -> process
        .create("hello-world")
        .runGraph(b -> b
            .getMessaging()
            .recordsSource("process-input", classOf[Document], "processors")
            .map(record -> {
                record.commit();
                return record.getValue();
            })
            .toMat(Sink.foreach(System.out::println), Keep.right())))
    ```

=== "Scala"

    ```scala

    ```

### Configuration

The messaging type can be specified in Alpakkeer's messaaging configuration:

=== "application.conf"

    ```hocon
    alpakkeer.messaging {
        # Possible values: in-memory, fs, kafka
        type = "in-memory"

        fs {
            directory = "./messaging"
        }

        kafka {
            bootstrap-server: "localhost:9042"

            consumer {
                # See Alpakka Kafka Connector consumer configurations
            }

            producer {
                # See Alpakka Kafka Connector producer configurations
            }
        }
    }
    ```

**TODO:** Topic and Queue configurations.

## Stream Monitoring

Alpakkeer offers sub-flows to argument streams with additional metrics for monitoring. **Checkpoints** collect simple measures at any point within a stream and report them in a defined interval to the proccess- or job-monitors:

=== "Java"

    ```java
    alpakkeer.withJob(jobs -> jobs
        .create("hello-world")
        .runGraph(b -> Source
            .single("Hello World")
            .via(b.getMonitoring().createCheckpointMonitor("step_1", Duration.ofSeconds(10)))
            .toMat(Sink.foreach(System.out::println), Keep.right()))
    ```

=== "Scala"

    ```scala
    // TODO
    ```

The checkpoint will report the following metrics:

* `moment`: The moment when the checkpoint published its statistic as Java `Instant`
* `timeElapsedNanos`: The time elapsed in the interval in nano seconds
* `count`: The number of documents processed in the interval
* `totalPushPullLatencyNanos`: The total latency of the checkpoint's graph stage between push and next pull-call from the downstream
* `totalPullPushLatencyNanos`: The total latency of the checkpoint's graph stage between pull and push of the upstrean

Thus the checkpoint metrics can detect whether there are problems in the down- or the upstream processing, as well as simply count the number of processed documents in a stream.

To monitor processing time of a sub-flow a **Latency Monitor** can be used. The sub-flow which is monitored must not filter or loose any elements.

=== "Java"

    ```java
    var flow = Flow.<String>create().map(String::toUpperCase);

    alpakkeer.withJob(jobs -> jobs
        .create("hello-world")
        .runGraph(b -> Source
            .single("Hello World")
            .via(b.getMonitoring().createLatencyMonitor("sub-flow", flow, Duration.ofSeconds(10)))
            .toMat(Sink.foreach(System.out::println), Keep.right()))
    ```

=== "Scala"

    ```scala
    // TODO
    ```

Like the checkpoint monitor, the latency monitor emits statistics in a given interval. The statistics reported include:

* `moment`: The moment when the monitor published its statistic as Java `Instant`
* `timeElapsedNanos`: The time elapsed in the interval in nano seconds
* `count`: The number of documents processed in the interval
* `sumLatency`: The complete processing time within the interval in nano seconds
* `avgLatency`: The average processing time measured in the interval

All statistics are reported to the configured job- or process-monitors. Depending on the monitor implemention the statistics are logged, summerized or collected (e.g. for Prometheus).

## Error Handling

**TODO**
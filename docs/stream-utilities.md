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

## Stream Monitoring

## Error Handling

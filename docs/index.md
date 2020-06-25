# Alpakkeer

![Alpakkeer Logo](./assets/logo-text.png)

<div style="text-align: center; font-style: italic">
Build, Run and Operate Akka Streams applications quickly, ready for production.
</div>

---
**Documentation:** [cokeschlumpf.github.io/alpakkeer](https://cokeschlumpf.github.io/alpakkeer/)<br />
**GitHub Repository:** [github.com/cokeSchlumpf/alpakkeer](https://github.com/cokeSchlumpf/alpakkeer)
---

Alpakkeer is an opinionated Java/ Scala toolkit to build, run and operate light-weight integration applications based on [Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html) and [Alpakka](https://doc.akka.io/docs/alpakka/current/index.html).

Alpakkeer bundles various libraries and components:

* A Web Server based on [Javalin](https://javalin.io/) to provide simple access via REST APIs to manage Akka Streams processes and expose metrics to Prometheus and Grafana.
 
* Configuration Management based on [Lightbend Config](https://github.com/lightbend/config) including some extensions for environment-based configurations and automatic mapping to POJOs.

* Prometheus Client to record application and stream metrics. Alpakkeer also provides custom FlowStages to argument your stream with Akka Streams specific metrics. 

* An easy to use DSL for Java and Scala to compose applications.

## Getting Started



## Requirements

Alpakkeer requires:

* Java 11+ 
* Scala 2.13

Alpakkeer runs on:

* Akka 2.6.4+
* Akka Streams 2.6.4+

## License

This project is licensed under the terms of the Apache 2.0 License.
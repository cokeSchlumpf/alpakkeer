# Concepts

Alpakkeer is a toolkit which bundles various other frameworks and tools to make it easy to develop streaming applications based  on Akka Streams. Such applications are usually used for creating simple application integrations, prediction pipelines which integrate enterprise data with AI services, or automated machine learning pipelines. 

## Ports and Adapters

The design of Alpakkeer aims to let developers focus on actual business requirements and business logic of the application instead of technical details like integrating streaming technologies like Kafka or Flink, or storage technologies like object storage, document databases etc.. Thus Alpakkeer strictly follows the [Ports and Adapters Pattern](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)) [^1] and provides deault ports for messaging, job and stream management as well as default implementations for these ports with various technologies.

![Hexagonal Architecture](https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Hexagonal_Architecture.svg/1920px-Hexagonal_Architecture.svg.png)

With Alpakkeer applications can be run on minimal resources on a local machine, but also distributed on scalable cloud infrastructure and robust clusters for messaging and data stores, just by changing the application configuration.

For all its default ports, Alpakkeer offers simple in-memory implementations (e.g. for messaging, context store) which can be used for local development, demos or very simple application which have no requirements for robustness. 

## Single Project - Distributed Deployment

Systems which are composed of multiple streams often share common data models or common stream logics. Also running the whole system to make manual tests and experiments for a optimal stream topology design help to quickly develop the application. Thus streams and services which might run distribited in a real system are easier to develop if all source code belongs to one project.

For example, the following stream topology can be put in a single Alpakkeer Java Application.

![Sample Stream Topology](/assets/sample-stream-topology.png)

*The crawlers can be implemented with Alpakkeer Jobs. The enricher and the processing streams can be implemented using Alpakkeer Processes.*

But at runtime, the application can run in different roles in multiple Kubernetes Deployments or on different virtual machines. See [Configuration](/configuration) for more details.

## Convention over Configuration

Alpakkeers DSL and configuration always offers fine-grained settings, but also all API methods have alternatives with minimal user-input. Also all configurations have senseful defaults. Thus most Alpakkeer applications don't require much customization on defaults if application designs follow common conventions.

## Extensibility

All internal Alpakkeer components are defined by Interfaces. Thus almost all components can be replaced or extended with custom implementations. 

[^1]: Ports & Adapters image source: [Wikimedia](https://commons.wikimedia.org/wiki/File:Hexagonal_Architecture.svg)
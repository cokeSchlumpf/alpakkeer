# Configuration

## Configuration Files

## Logging configuration

Alpakkeer uses SLF4J as a general logging facade. Thus an Alpakkeer application can use any SLF4J compatible logging provider.

A feasible logging configuration using [Logback](http://logback.qos.ch/) could be:

=== "Maven"

    ```xml
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.3</version>
        <scope>test</scope>
    </dependency>
    ```

=== "Gradle"

    ```groovy
    compile group: 'ch.qos.logback', name: 'logback-classic', version: '1.2.3'
    ```

=== "SBT"

    ```scala
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
    ```

... and the following configuration in `src/main/resources/logback.xml`

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{"yyyy-MM-dd'T'HH:mm:ss.SSSXXX", UTC} %level [%thread] [%logger]: %msg%n</pattern>
        </encoder>
    </appender>


    <root level="info">
        <appender-ref ref="STDOUT" />
    </root>

    <logger name="org.flywaydb" level="warn" />
    <logger name="org.quartz" level="warn" />
    <logger name="scala.slick" level="info" />
</configuration>
```
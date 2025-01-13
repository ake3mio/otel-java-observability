# otel-java-observability

This project contains an experiment on instrumenting java applications for distributed tracing, log aggregation, metrics
and profiling. It also contains a base setup of loki for log aggregation, tempo for traces, pyroscope for profiles,
prometheus for metrics, grafana alloy for telemetry collection and processing and grafana for observability
visualisation.

Observability technologies allow for insight into the performance and behaviour of applications and systems. However,
each pillar of observability alone may not be enough to resolve issues, and it can be difficult and time-consuming to
tie together to build a full picture of what occurred at a given point in time.

In this project I used opentelemetry tracing to inject trace and span ids into logs and produce exemplars with tracing
info for profiles and metrics.
This allowed me to tie together each pillar of observability and link between them in grafana.

![Screenshot 2025-01-13 at 08.19.40.png](docs%2FScreenshot%202025-01-13%20at%2008.19.40.png)

### Traces to logs
![Screenshot 2025-01-13 at 08.19.57.png](docs%2FScreenshot%202025-01-13%20at%2008.19.57.png)

### Traces to profiles
![Screenshot 2025-01-13 at 08.29.59.png](docs%2FScreenshot%202025-01-13%20at%2008.29.59.png)

### Traces to metrics
![Screenshot 2025-01-13 at 08.33.21.png](docs%2FScreenshot%202025-01-13%20at%2008.33.21.png)

### Metrics exemplars
![Screenshot 2025-01-13 at 08.34.38.png](docs%2FScreenshot%202025-01-13%20at%2008.34.38.png)

I also provisioned a JVM metrics dashboard to visualise the performance of the java applications and a span metrics
dashboard to see the performance of traced operations. See the dashboards here: http://localhost:3000/dashboards


#### (Spanmetrics)
![Screenshot 2025-01-13 at 08.36.16.png](docs%2FScreenshot%202025-01-13%20at%2008.36.16.png)

There are two applications in this project:

- [temperature-poller-service](temperature-poller-service): This polls a rest endpoint at https://api.open-meteo.com
  every 10 seconds to
  check for temperatures for a number of cities listed in [cities.json](src/main/resources/cities.json). The recorded
  temperatures are published over activemq on a TEMPERATURES queue.
- [temperature-persistence-service](temperature-persistence-service): This subscribes to the TEMPERATURES queue and
  writes the temperatures to influxdb to be later visualised in grafana on a
  geomap: http://localhost:3000/d/influxdb/map?orgId=1
  ![Screenshot 2025-01-13 at 08.16.57.png](docs%2FScreenshot%202025-01-13%20at%2008.16.57.png)

## Running this project

### Dependencies

- Java 21
- Maven >= 3.9.6
- Docker and docker compose

### Starting

#### Compile sources

```shell
mvn clean install
```

#### Run observability stack and external services

```shell
docker-compose up -d
```

#### Run temperature-poller-service

```shell
java -jar ./temperature-poller-service/target/temperature-poller-service.jar
```

#### Run temperature-persistence-service

```shell
java -jar ./temperature-persistence-service/target/temperature-persistence-service.jar
```

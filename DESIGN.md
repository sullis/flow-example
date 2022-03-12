# Design

This application is written in Java. It is built on top of the Dropwizard framework. I chose Dropwizard because it includes these out of the box capabilities:

- HTTP connectors
- REST API support (via JAX-RS annotations)
- app configuration
- JSON serialization
- validation framework
- logging

# REST API

The `/flows` endpoint accepts HTTP POST requests and HTTP GET requests.

The [FlowResource](https://github.com/sullis/flow-example/blob/main/server/src/main/java/io/github/sullis/flow/server/FlowsResource.java) class implements the REST API. It uses JAX-RS annotations: @GET and @POST. These annotations map HTTP operations to Java methods. 

# Assumptions
- Requests will be a mix of read operations (HTTP GET) and write operations (HTTP POST)
- The application must be able to handle concurrent read and write operations

The ratio of read-to-write operations was not specified in the design document.

## Core classes:
- [FlowResource](https://github.com/sullis/flow-example/blob/main/server/src/main/java/io/github/sullis/flow/server/FlowsResource.java)
- [FlowAggregator](https://github.com/sullis/flow-example/blob/main/server/src/main/java/io/github/sullis/flow/server/FlowAggregator.java)

FlowResource receives HTTP requests. FlowResource is responsible for calling appropriate FlowAggregator method.

| HTTP request | Operation                                    |
|--------------|----------------------------------------------|
| HTTP POST    | FlowResource calls FlowAggregator record     |
| HTTP GET     | FlowResource calls FlowAggregator findByHour |

## FlowAggregator

FlowAggregator exposes two public methods:
```java
public void record(final List<FlowLog> logs)
```

and

```java
public Map<LookupKey, FlowTotal> findByHour(final Integer hour)
```

## OpenAPI
- [api.yaml](https://github.com/sullis/flow-example/blob/main/openapi/src/main/resources/api.yaml)

## Core libraries
- [openapi-generator Maven plugin](https://github.com/OpenAPITools/openapi-generator)
- [Dropwizard](https://www.dropwizard.io/en/latest/)
- [jackson-databind](https://github.com/FasterXML/jackson-databind)

## Testing libraries
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [junit-pioneer](https://junit-pioneer.org/docs/)
- [dropwizard-testing](https://www.dropwizard.io/en/latest/manual/testing.html)
- [awaitility](https://github.com/awaitility/awaitility)
- [assertj-core](https://github.com/assertj/assertj-core)
- [JsonUnit](https://github.com/lukas-krecan/JsonUnit) 

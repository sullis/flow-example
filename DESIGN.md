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

The [FlowResource](https://github.com/sullis/flow-example/blob/main/server/src/main/java/io/github/sullis/flow/server/FlowsResource.java) class implements the REST API. It uses JAX-RS annotations @GET and @POST. These annotations map HTTP methods to Java methods. 

# Assumptions
- Requests will be a mix of read operations (HTTP GET) and write operations (HTTP POST)
- The application must be able to handle concurrent read and write operations

The ratio of read-to-write operations was not specified in the design document.

## Core classes:
- [FlowResource](https://github.com/sullis/flow-example/blob/main/server/src/main/java/io/github/sullis/flow/server/FlowsResource.java)
- [FlowAggregator](https://github.com/sullis/flow-example/blob/main/server/src/main/java/io/github/sullis/flow/server/FlowAggregator.java)

FlowResource receives HTTP requests. FlowResource is responsible for calling the appropriate FlowAggregator method.

| HTTP request            | Operation                                          |
|-------------------------|----------------------------------------------------|
| HTTP POST /flows        | FlowResource calls FlowAggregator record(flowLogs) |
| HTTP GET  /flows?hour=3 | FlowResource calls FlowAggregator findByHour(hour) |

## FlowAggregator

FlowAggregator exposes two public methods:
```java
public void record(final List<FlowLog> logs)
```

and

```java
public Map<LookupKey, FlowTotal> findByHour(final Integer hour)
```
## Internal data structure

FlowAggregator's internal data structure is an array of ConcurrentHashMap's.

The array is a fixed size: 24.  Each slot in the array will be used to store data for a given hour.

We assume that the range of valid hours is 0 to 23 (inclusive).

Each array element is an object of type ConcurrentHashMap<LookupKey, FlowTotal>.

LookupKey is a composite key, derived from a FlowLog's significant fields.
FlowTotal is an object that contains two fields:
1) the number of bytes received
2) the number of bytes transmitted

For each FlowLog object, the FlowAggregator will:
1) fetch the ConcurrentHashMap at array index `flowLog.getHour`
2) create a LookupKey from the FlowLog object
3) use the LookupKey to fetch the FlowTotal object from the ConcurrentHashMap
4) increment FlowTotal.bytesRx
5) increment FlowTotal.bytesTx

ConcurrentHashMap was chosen because:
- ConcurrentHashMap is threadsafe
- retrieval operations generally do not block
- retrieval operations may overlap with update operations
- ConcurrentHashMap can be tuned for concurrently updating threads (concurrencyLevel)

# ConcurrentHashMap constructor

The ConcurrentHashMap class provides a constructor that accepts three parameters:
```java
ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel)
```

It is important to construct the map with parameter values that match the expected workload.

Since there will be multiple updating threads, we should give special consideration to the `concurrencyLevel` parameter:

```
concurrencyLevel - the estimated number of concurrently updating threads
```

# FlowTotal
FlowTotal contains two fields:
```java
    public final LongAdder bytesRx = new LongAdder();
    public final LongAdder bytesTx = new LongAdder();
```

The JDK documentation describes LongAdder as an alternative to AtomicLong: 
```
This class [LongAdder] is usually preferable to AtomicLong when multiple threads update a common sum
```

If I had more time, I would evaluate the suitability of LongAdder vs AtomicLong.

# Concurrency testing:  FlowAggregatorTest

FlowAggregatorTest contains a test called `concurrency`:

```java
    @CartesianTest
    public void concurrency(
            @Values(ints = { 1, 10, 20 }) final int numThreads,
            @Values(ints = { 1, 3, 10 }) final int numReaders,
            @Values(ints = { 1, 3, 10 }) final int numWriters)
```

The test uses junit-pioneer [@CartesianTest](https://junit-pioneer.org/docs/cartesian-product/) to execute different scenarios:
- number of threads
- number of read operations
- number of write operations

# Concurrency testing:  AppConcurrencyTest

AppConcurrencyTest generates multiple HTTP requests. It attempts to intersperse read operations with write operations.

```java
    @CartesianTest
    public void concurrency(
            @CartesianTest.Values(ints = { 1, 2, 10 }) final int numReaders,
            @CartesianTest.Values(ints = { 1, 2, 10 }) final int numWriters)
```

The @CartesianTest annotation tells JUnit Pioneer to run the test with a varying number of:
- readers
- writers

## OpenAPI
I created an OpenAPI spec that defines the Flows API:
- [api.yaml](https://github.com/sullis/flow-example/blob/main/openapi/src/main/resources/api.yaml)

I was planning to use the OpenAPI generator to generate a JAX-RX resource class. However, there was a problem 
with the code generator. I decided to use only one of the generated source files: ```FlowLog.java```

If I had additional time, I would re-examine the generated code.

# Side note
I am a committer on two OpenAPI projects
- [openapi-generator](https://github.com/OpenAPITools/openapi-generator)
- [guardrail](https://guardrail.dev/)

## Core libraries

This project uses:
- [openapi-generator Maven plugin](https://github.com/OpenAPITools/openapi-generator) - OpenAPI code generator
- [Dropwizard](https://www.dropwizard.io/en/latest/) - REST API framework
- [jackson-databind](https://github.com/FasterXML/jackson-databind) - JSON serialization

## Testing libraries

This project uses:
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [junit-pioneer](https://junit-pioneer.org/docs/)
- [dropwizard-testing](https://www.dropwizard.io/en/latest/manual/testing.html)
- [awaitility](https://github.com/awaitility/awaitility)
- [assertj-core](https://github.com/assertj/assertj-core)
- [JsonUnit](https://github.com/lukas-krecan/JsonUnit) 

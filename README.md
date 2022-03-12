# flow-example

This is a Java application. It requires:
- JDK 17
- Apache Maven

# Setup

- install [Apache Maven](https://maven.apache.org/index.html)
- install [Azul Zulu JDK 17](https://www.azul.com/downloads/?version=java-17-lts&package=jdk)

# Maven build command

```
mvn clean test
```

# Running the server locally

```
make run
```

# curl command

The server will be listening for requests on port 8080

```
curl 'http://localhost:8080/flows?hour=3'
```

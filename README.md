# A demo-kafka-app

This project provides a simple example of how to use Spring Boot and Kafka to build a messaging application.

This project is a demonstration of a Spring Boot application that uses Apache Kafka to send and receive messages. It's a great starting point for anyone looking to learn how to integrate Kafka into their own Spring Boot applications.

## Project Requirements

To build and run this project, you will need the following:

* Java 25 or later
* Maven 3.6.3 or later
* Docker and Docker Compose (for running containerized Kafka broker)

## Dependencies

This project uses the following dependencies:

* **Spring Boot:** The core framework for building the application.
* **Spring for Apache Kafka:** Provides the necessary components for integrating with Kafka.
* **Spring Boot Actuator:** Adds production-ready features like health checks and metrics.

For a complete list of dependencies, please see the `pom.xml` file.

## Getting Started

To get started with this project, you can clone the repository to your local machine. Once you have cloned the repository, you can import it into your favorite IDE.

### Environment Setup

* The project uses SDKMAN for managing Java and Maven versions.
* Initialize your development environment using **SDKMAN** CLI and sdkman env file [`sdkmanrc`](.sdkmanrc)

```shell
sdk env install
sdk env
```

#### Note: To install SDKMAN refer: [sdkman.io](https://sdkman.io/install)

---

## How to run the application

There are two ways to run the application:

### 1. Running the Spring Boot application directly

You can also run the Spring Boot application directly from your IDE or by using the Maven wrapper.

To run the application using the Maven wrapper, use the following command:

```shell
sdk env
./mvnw spring-boot:run
```

This will use spring-boot docker-compose support to start a Kafka broker along with the application.

### 2. Using Docker Compose

To run the application in production mode, use the `compose-prod.yml` file:

```shell
docker compose -f compose-prod.yml up;docker compose -f compose-prod.yml down -v
```

This will build the Docker image for the application and start it along with a Kafka broker in docker compose setup.

## Testing the application

### Run the tests using Maven

To run the tests for the application, you can use the following Maven command:

```shell
sdk env
./mvnw clean test -Dtest="DemoKafkaAppTests"
./mvnw test
```

This will execute all the `unit-tests` and `integration-tests` for the application using `test-containers` to spin up a Kafka broker in a docker container for testing purposes.

## Build the application

To build the application, you can use the following Maven command:

```shell
sdk env
./mvnw clean package -DskipTests
```

This will create a JAR file in the `target` directory.

---

### Kafka Consumer

The [AppKafkaListener](src/main/java/io/akikr/event/consumer/AppKafkaListener.java) class is responsible for receiving messages from Kafka. It uses the `@KafkaListener` annotation to listen for messages on the topic specified in the `app.kafka.consumer.topics` property.

To produce a message to Kafka topic via `kafka` container running in docker, you can use the following `kafka-console-producer` command:

```shell
echo '{"id": 101, "data": "test"}' | docker exec -i kafka kafka-console-producer --broker-list localhost:9092 --topic app-in-topic
```

where:

* `localhost:9092` is the value of property: `spring.kafka.consumer.bootstrap-servers`
* `app-in-topic` is topic name configured from the value of property: `app.kafka.consumer.topics`

### Kafka Producer

The [AppKafkaProducer](src/main/java/io/akikr/event/producer/AppKafkaProducer.java) class is responsible for sending messages to Kafka. It uses the `KafkaTemplate` to send messages to the topic specified in the `app.kafka.producer.topics` property.

To see the messages being sent to Kafka topic via `kafka` container running in docker, you can use the following `kafka-console-consumer` command:

```shell
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic app-out-topic --from-beginning
```

where:

* `localhost:9092` is the value of property: `spring.kafka.producer.bootstrap-servers`
* `app-out-topic` is topic name(s) configured from the value of property: `app.kafka. producer.topics`

---

## Contributing

Feel free to contribute to this project!

For questions or issues, please open a GitHub issue or submit a pull request.

Happy coding! ✌️

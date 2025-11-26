# A demo-kafka-app

This project provides a simple example of how to use Spring Boot and Kafka to build a messaging application.

This project is a demonstration of a Spring Boot application that uses Apache Kafka to send and receive messages. It's a great starting point for anyone looking to learn how to integrate Kafka into their own Spring Boot applications.

## Project Requirements

To build and run this project, you will need the following:

*   Java 25 or later
*   Maven 3.6.3 or later
*   Docker and Docker Compose (for running containerized Kafka broker)

## Dependencies

This project uses the following dependencies:

*   **Spring Boot:** The core framework for building the application.
*   **Spring for Apache Kafka:** Provides the necessary components for integrating with Kafka.
*   **Spring Boot Actuator:** Adds production-ready features like health checks and metrics.

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

## Relevant Code Examples

Here are some code examples from the project to help you understand how it works.

### Kafka Consumer

The `AppKafkaListener` class is responsible for receiving messages from Kafka. It uses the `@KafkaListener` annotation to listen for messages on the topic specified in the `app.kafka.consumer.topics` property.

```java
@Component
public class AppKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AppKafkaListener.class);

    @Value("${app.kafka.consumer.topics:app-in-topic}")
    private String[] appConsumerTopics;
    @Value("${spring.kafka.consumer.group-id:app-group}")
    private String appConsumerGroupId;

    private final AppService appService;

    public AppKafkaListener(AppService appService) {
        this.appService = appService;
    }

    @KafkaListener(
            topics = "${app.kafka.consumer.topics:app-in-topic}",
            groupId = "${spring.kafka.consumer.group-id:app-group}"
    )
    public void listen(String message) {
        log.info("Received Message:[{}] from Kafka topics:{} and groupId:[{}]", message,
                Arrays.asList(appConsumerTopics), appConsumerGroupId);
        appService.delegateMessage(message);
    }
}
```

### Kafka Producer

The `AppKafkaProducer` class is responsible for sending messages to Kafka. It uses the `KafkaTemplate` to send messages to the topic specified in the `app.kafka.producer.topics` property.

```java
@Component
public class AppKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(AppKafkaProducer.class);
    
    @Value("${app.kafka.producer.topics:app-out-topic}")
    private String appProducerTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    public AppKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String message) {
        log.info("Sending Message:[{}] to Kafka topic:[{}]", message, appProducerTopic);
        CompletableFuture<SendResult<String, String>> sendResult = kafkaTemplate.send(appProducerTopic, message);
        sendResult.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", message, result.getRecordMetadata().offset());
            } else {
                log.info("Unable to send message=[{}] due to : {}", message, ex.getMessage());
            }
        });
    }
}
```

## Contributing

Feel free to contribute to this project!

For questions or issues, please open a GitHub issue or submit a pull request.

Happy coding! ✌️

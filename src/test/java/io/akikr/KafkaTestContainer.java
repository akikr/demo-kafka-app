package io.akikr;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@TestPropertySource(properties = {"spring.config.location=classpath:application-test.properties"})
public abstract class KafkaTestContainer {

    protected static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"))
            // Set the reuse property to true to allow reusing the container across tests
            .withReuse(true);

    static {
        KAFKA_CONTAINER.start();
        System.out.println("Kafka container started");
        //KAFKA_CONTAINER.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaTestContainer.class)));
        Runtime.getRuntime().addShutdownHook(new Thread(KAFKA_CONTAINER::close));
    }

    @BeforeAll
    static void setUpKafka() {
        if (KAFKA_CONTAINER.isRunning())
            System.out.println("Kafka container running !!");
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        // Consumer properties
        registry.add("spring.kafka.consumer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        // Producer properties
        registry.add("spring.kafka.producer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }
}

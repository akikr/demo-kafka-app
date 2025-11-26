package io.akikr;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Map;

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

    ///
    /// Creates a test Kafka consumer wired to the Testcontainers `KAFKA_CONTAINER`
    ///
    /// Uses the container's bootstrap servers and the supplied deserializers and return a consumer that is already subscribed to the provided topics.
    ///
    /// Example:
    /// ```java
    /// var consumer = createTestKafkaConsumer(java.util.Collections.singletonList("topic"),
    ///                                        "test-group",
    ///                                        "earliest",
    ///                                        org.apache.kafka.common.serialization.StringDeserializer.class,
    ///                                        org.apache.kafka.common.serialization.StringDeserializer.class);
    /// ```
    /// @param topics            The topics to subscribe to
    /// @param groupId           The consumer group id
    /// @param autoOffsetReset   The auto offset reset policy (e.g., **earliest**, **latest**)
    /// @param keyDeserializer   The key deserializer class. (e.g., **StringDeserializer.class**)
    /// @param valueDeserializer The value deserializer class. (e.g., **StringDeserializer.class**)
    ///
    /// @return A Kafka consumer of type: `Consumer<K, V>` subscribed to the specified topics
    ///
    public static <K, V> Consumer<K, V> createTestKafkaConsumer(Collection<String> topics,
                                                                String groupId,
                                                                String autoOffsetReset,
                                                                Class<?> keyDeserializer,
                                                                Class<?> valueDeserializer) {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer
        );
        DefaultKafkaConsumerFactory<K, V> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<K, V> consumer = consumerFactory.createConsumer();
        consumer.subscribe(topics);
        return consumer;
    }
}

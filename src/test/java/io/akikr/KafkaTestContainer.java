package io.akikr;

import java.util.Collection;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Testcontainers(disabledWithoutDocker = true)
public class KafkaTestContainer {

    @Container
    @ServiceConnection
    public static final ConfluentKafkaContainer KAFKA_CONTAINER =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    static {
        KAFKA_CONTAINER.start();
        System.out.println("Kafka container started");
        // KAFKA_CONTAINER.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger(KafkaTestContainer.class)));
        Runtime.getRuntime().addShutdownHook(new Thread(KAFKA_CONTAINER::close));
    }

    @BeforeAll
    static void setUpKafka() {
        if (KAFKA_CONTAINER.isRunning()) System.out.println("Kafka container running !!");
    }

    ///
    /// Creates a test Kafka consumer wired to the Testcontainers `KAFKA_CONTAINER`
    ///
    /// Uses the container's bootstrap servers and the supplied deserializers and return a consumer that is already
    /// subscribed to the provided topics.
    ///
    /// Example:
    /// ```java
    /// var consumer = createTestKafkaConsumerWithOnePartition(java.util.Collections.singletonList("topic"),
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
    public static <K, V> Consumer<K, V> createTestKafkaConsumerWithOnePartition(
            Collection<String> topics,
            String groupId,
            String autoOffsetReset,
            Class<?> keyDeserializer,
            Class<?> valueDeserializer) {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        DefaultKafkaConsumerFactory<K, V> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<K, V> consumer = consumerFactory.createConsumer();
        var partitions =
                topics.stream().map(topic -> new TopicPartition(topic, 0)).toList();
        consumer.assign(partitions);
        if ("earliest".equals(autoOffsetReset)) {
            consumer.seekToBeginning(partitions);
        }
        return consumer;
    }

    ///
    /// Creates a test Kafka producer wired to the Testcontainers `KAFKA_CONTAINER`.
    ///
    /// Uses the container's bootstrap servers and the supplied serializers and returns a KafkaTemplate (producer)
    /// configured to produce messages using the given compression type and serializers.
    ///
    /// Example:
    ///
    /// ```java
    /// var producer = createTestKafkaProducer("all", "gzip",
    /// org.apache.kafka.common.serialization.StringSerializer.class,
    /// org.apache.kafka.common.serialization.StringSerializer.class);
    /// ```
    ///
    /// @param ackConfig        The number of acknowledgments the producer requires the leader to have received
    /// before considering a request complete (e.g, **0**, **1**, **all**)
    /// @param compressionType  The compression type to use for the producer (e.g., "**gzip**", "**snappy**",
    /// "**none**")
    /// @param keySerializer    The key serializer class (e.g., **StringSerializer.class**)
    /// @param valueSerializer  The value serializer class (e.g., **.StringSerializer.class**)
    ///
    /// @return A Kafka producer/template of type: `KafkaTemplate<K, V>` configured to use the Testcontainers Kafka
    /// bootstrap servers
    ///
    public static <K, V> KafkaTemplate<K, V> createTestKafkaProducer(
            String ackConfig, String compressionType, Class<?> keySerializer, Class<?> valueSerializer) {
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers(),
                ProducerConfig.ACKS_CONFIG, ackConfig,
                ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        DefaultKafkaProducerFactory<K, V> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        return new KafkaTemplate<>(producerFactory);
    }
}

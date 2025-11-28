package io.akikr.event;

import io.akikr.KafkaTestContainer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.out;
import static java.time.Duration.ofSeconds;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.config.location=classpath:application-event-test.properties"})
class AppKafkaListenerToKafkaProducerITest extends KafkaTestContainer {

    private final CopyOnWriteArrayList<String> receivedMessageList = new CopyOnWriteArrayList<>();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    ///
    /// These values must be loaded from application TestPropertySource: `application-event-test.properties`
    ///
    @Value("${app.kafka.consumer.topics}")
    private String[] appConsumerTopics;
    @Value("${app.kafka.producer.topics}")
    private String appProducerTopic;

    private Consumer<String, String> testConsumer;

    @BeforeEach
    void setUp() {
        // Verify Kafka container is running
        assertThat(KAFKA_CONTAINER.isRunning()).isTrue();
        out.println("Setting up Kafka TestContainer and creating a testConsumer for topic:[" + appProducerTopic + "]");
        // Initialize test consumer
        this.testConsumer = createTestKafkaConsumer(
                Collections.singletonList(appProducerTopic),
                UUID.randomUUID().toString().concat("test-consumer-group"),
                "earliest",
                StringDeserializer.class,
                StringDeserializer.class);
    }

    @AfterEach
    void  tearDown() {
        out.println("Clearing receivedMessageList and closing testConsumer");
        receivedMessageList.clear();
        if (nonNull(testConsumer)) {
            testConsumer.unsubscribe();
            testConsumer.close();
        }
    }

    @Test
    @Order(value = 1)
    @DisplayName("A valid message should be listen by AppKafkaListener#listen from topics:[appConsumerTopics] and processed by App and send by AppKafkaProducer#sendMessage to topic: [appProducerTopic]")
    void shouldListenAndProcessTheMessageSuccessfully() {
        //Arrange
        var testMessage = "Test Message at " + System.currentTimeMillis();

        //Act and Assert:

        // Send message to all [appConsumerTopics] listener-topics
        Arrays.stream(appConsumerTopics).forEach(topic -> {
            var sendResult = kafkaTemplate.send(topic, testMessage);
            await().pollInterval(ofSeconds(3))
                    .atMost(5, SECONDS)
                    .untilAsserted(() -> {
                        assertThat(sendResult).isNotNull();
                        sendResult.whenComplete((result, ex) -> {
                            assertThat(ex).isNull();
                            assertThat(result).isNotNull();
                            assertThat(result.getRecordMetadata()).isNotNull();
                            assertThat(result.getRecordMetadata().topic()).isEqualTo(topic);
                            assertThat(result.getRecordMetadata().hasOffset()).isTrue();
                            assertThat(result.getRecordMetadata().offset()).isGreaterThanOrEqualTo(0);
                            assertThat(result.getProducerRecord()).isNotNull();
                            assertThat(result.getProducerRecord().topic()).isEqualTo(topic);
                            assertThat(result.getProducerRecord().value()).contains(testMessage);
                        });
                    });
        });

        // Receive all messages from [appProducerTopic] producer-topic
        var consumerRecords = testConsumer.poll(ofSeconds(3));
        await().pollInterval(ofSeconds(3))
                .atMost(5, SECONDS)
                .untilAsserted(() -> {
                    assertThat(consumerRecords).isNotEmpty();
                    out.printf("ConsumedRecords Count:[%s]%n", consumerRecords.count());
                    assertThat(consumerRecords.count()).isGreaterThanOrEqualTo(0);
                    assertThat(consumerRecords.partitions().size()).isGreaterThanOrEqualTo(0);
                    List<String> consumerTopics = consumerRecords.partitions().parallelStream()
                            .filter(Objects::nonNull)
                            .map(TopicPartition::topic)
                            .toList();
                    out.printf("ConsumedRecords topics:%s%n", consumerTopics);
                    assertThat(consumerTopics.contains(appProducerTopic)).isTrue();
                    consumerRecords.forEach(record -> receivedMessageList.add(record.value()));
                    out.printf("Messages:%s%n", receivedMessageList);
                    assertThat(receivedMessageList.parallelStream()
                            .anyMatch(message -> message.contains(testMessage)))
                            .isTrue();
                });
    }

    @Test
    @Order(value = 2)
    @DisplayName("A invalid message should be listen by AppKafkaListener#listen from topics:[appConsumerTopics] and processed by App and send by AppKafkaProducer#sendMessage to topic: [appProducerTopic]")
    void shouldListenAndProcessTheInvalidMessage() {
        //Arrange
        var testMessage = StringUtils.SPACE;
        var invalidMessage = "Invalid message received";

        //Act and Assert:

        // Send message to all [appConsumerTopics] listener-topics
        Arrays.stream(appConsumerTopics).forEach(topic -> {
            var sendResult = kafkaTemplate.send(topic, testMessage);
            await().pollInterval(ofSeconds(3))
                    .atMost(5, SECONDS)
                    .untilAsserted(() -> {
                        assertThat(sendResult).isNotNull();
                        sendResult.whenComplete((result, ex) -> {
                            assertThat(ex).isNull();
                            assertThat(result).isNotNull();
                            assertThat(result.getRecordMetadata()).isNotNull();
                            assertThat(result.getRecordMetadata().topic()).isEqualTo(topic);
                            assertThat(result.getRecordMetadata().hasOffset()).isTrue();
                            assertThat(result.getRecordMetadata().offset()).isGreaterThanOrEqualTo(0);
                            assertThat(result.getProducerRecord()).isNotNull();
                            assertThat(result.getProducerRecord().topic()).isEqualTo(topic);
                            assertThat(result.getProducerRecord().value()).contains(testMessage);
                        });
                    });
        });

        // Receive all messages from [appProducerTopic] producer-topic
        var consumerRecords = testConsumer.poll(ofSeconds(3));
        await().pollInterval(ofSeconds(3))
                .atMost(5, SECONDS)
                .untilAsserted(() -> {
                    assertThat(consumerRecords).isNotEmpty();
                    out.printf("ConsumedRecords Count:[%s]%n", consumerRecords.count());
                    assertThat(consumerRecords.count()).isGreaterThanOrEqualTo(0);
                    assertThat(consumerRecords.partitions().size()).isGreaterThanOrEqualTo(0);
                    List<String> consumerTopics = consumerRecords.partitions().parallelStream()
                            .filter(Objects::nonNull)
                            .map(TopicPartition::topic)
                            .toList();
                    out.printf("ConsumedRecords topics:%s%n", consumerTopics);
                    assertThat(consumerTopics.contains(appProducerTopic)).isTrue();
                    consumerRecords.forEach(record -> receivedMessageList.add(record.value()));
                    out.printf("Messages:%s%n", receivedMessageList);
                    assertThat(receivedMessageList.parallelStream()
                            .anyMatch(message -> message.contains(invalidMessage)))
                            .isTrue();
                });
    }
}

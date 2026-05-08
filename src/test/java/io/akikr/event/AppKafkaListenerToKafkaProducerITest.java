package io.akikr.event;

import static io.akikr.KafkaTestContainer.KAFKA_CONTAINER;
import static io.akikr.KafkaTestContainer.createTestKafkaConsumerWithOnePartition;
import static io.akikr.KafkaTestContainer.createTestKafkaProducer;
import static java.lang.System.out;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.akikr.KafkaTestContainer;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ImportTestcontainers(value = {KafkaTestContainer.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.config.location=classpath:application-event-test.properties"})
class AppKafkaListenerToKafkaProducerITest {

    @Value("${app.kafka.consumer.topics}")
    private String[] appConsumerTopics;

    @Value("${app.kafka.producer.topics}")
    private String appProducerTopic;

    private KafkaTemplate<String, String> testKafkaTemplate;
    private Consumer<String, String> testKafkaConsumer;
    private TopicPartition appProducerPartition;

    @BeforeEach
    void setUp() {
        assertThat(KAFKA_CONTAINER.isRunning()).isTrue();
        out.println("Setting up Kafka TestContainer and creating a testConsumer for topic:[" + appProducerTopic + "]");

        this.testKafkaTemplate =
                createTestKafkaProducer("all", "snappy", StringSerializer.class, StringSerializer.class);

        this.testKafkaConsumer = createTestKafkaConsumerWithOnePartition(
                Collections.singletonList(appProducerTopic),
                UUID.randomUUID().toString().concat("test-consumer-group"),
                "earliest",
                StringDeserializer.class,
                StringDeserializer.class);
        this.appProducerPartition = new TopicPartition(appProducerTopic, 0);
    }

    @AfterEach
    void tearDown() {
        if (testKafkaConsumer != null) {
            testKafkaConsumer.close();
        }
        if (testKafkaTemplate != null) {
            testKafkaTemplate.destroy();
        }
    }

    @Test
    @Order(value = 1)
    @DisplayName(
            "A valid message should be listen by AppKafkaListener#listen from topics:[appConsumerTopics] and processed by App and send by AppKafkaProducer#sendMessage to topic: [appProducerTopic]")
    void shouldListenAndProcessTheMessageSuccessfully() {
        var testMessage = "Test Message at " + System.currentTimeMillis();
        var baselineOffset = testKafkaConsumer
                .endOffsets(Collections.singleton(appProducerPartition))
                .get(appProducerPartition);

        Arrays.stream(appConsumerTopics).forEach(topic -> {
            var sendResult = testKafkaTemplate
                    .send(topic, 0, UUID.randomUUID().toString(), testMessage)
                    .join();
            assertThat(sendResult.getRecordMetadata()).isNotNull();
            assertThat(sendResult.getRecordMetadata().topic()).isEqualTo(topic);
        });

        await().pollInterval(ofSeconds(1)).atMost(10, SECONDS).untilAsserted(() -> {
            var consumerRecords = testKafkaConsumer.poll(ofSeconds(1));
            out.printf("ConsumedRecords Count:[%s]%n", consumerRecords.count());

            var allMessages = StreamSupport.stream(
                            consumerRecords.records(appProducerTopic).spliterator(), false)
                    .filter(record -> record.offset() >= baselineOffset)
                    .map(ConsumerRecord::value)
                    .toList();
            out.printf("Messages:%s%n", allMessages);

            assertThat(allMessages).contains(testMessage);
        });
    }

    @Test
    @Order(value = 2)
    @DisplayName(
            "A invalid message should be listen by AppKafkaListener#listen from topics:[appConsumerTopics] and processed by App and send by AppKafkaProducer#sendMessage to topic: [appProducerTopic]")
    void shouldListenAndProcessTheInvalidMessage() {
        var testMessage = " ";
        var invalidMessage = "Invalid message received: Message cannot be null or blank";
        var baselineOffset = testKafkaConsumer
                .endOffsets(Collections.singleton(appProducerPartition))
                .get(appProducerPartition);

        Arrays.stream(appConsumerTopics).forEach(topic -> {
            var sendResult = testKafkaTemplate
                    .send(topic, 0, UUID.randomUUID().toString(), testMessage)
                    .join();
            assertThat(sendResult.getRecordMetadata()).isNotNull();
            assertThat(sendResult.getRecordMetadata().topic()).isEqualTo(topic);
        });

        await().pollInterval(ofSeconds(1)).atMost(10, SECONDS).untilAsserted(() -> {
            var consumerRecords = testKafkaConsumer.poll(ofSeconds(1));
            out.printf("ConsumedRecords Count:[%s]%n", consumerRecords.count());

            var allMessages = StreamSupport.stream(
                            consumerRecords.records(appProducerTopic).spliterator(), false)
                    .filter(record -> record.offset() >= baselineOffset)
                    .map(ConsumerRecord::value)
                    .toList();
            out.printf("Messages:%s%n", allMessages);

            assertThat(allMessages).contains(invalidMessage);
        });
    }
}

package io.akikr.event.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

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

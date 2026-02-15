package io.akikr.event.producer;

import static java.util.Objects.isNull;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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
        var key = UUID.randomUUID().toString();
        log.info("Sending Message:[{}] with key:[{}] to Kafka topic:[{}]", message, key, appProducerTopic);
        var sendResult = kafkaTemplate.send(appProducerTopic, key, message);
        sendResult.whenComplete((result, ex) -> {
            if (isNull(ex)) {
                log.info(
                        "Sent message=[{}] with offset=[{}]",
                        message,
                        result.getRecordMetadata().offset());
            } else {
                log.info("Unable to send message=[{}] due to : {}", message, ex.getMessage());
            }
        });
    }
}

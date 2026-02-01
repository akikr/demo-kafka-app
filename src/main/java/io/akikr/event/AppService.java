package io.akikr.event;

import static java.util.Objects.isNull;

import io.akikr.event.producer.AppKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AppService {

    private static final Logger log = LoggerFactory.getLogger(AppService.class);
    private final AppKafkaProducer appKafkaProducer;

    public AppService(AppKafkaProducer appKafkaProducer) {
        this.appKafkaProducer = appKafkaProducer;
    }

    public void delegateMessage(String message) {
        log.info("Processing message:[{}]", message);
        try {
            if (isNull(message) || message.isBlank()) {
                throw new IllegalArgumentException("Message cannot be null or blank");
            }
            appKafkaProducer.sendMessage(message);
        } catch (IllegalArgumentException e) {
            appKafkaProducer.sendMessage("Invalid message received: " + e.getMessage());
        }
    }
}

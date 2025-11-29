package io.akikr.event;

import io.akikr.event.producer.AppKafkaProducer;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;

@Service
public class AppService {

    private final AppKafkaProducer appKafkaProducer;

    public AppService(AppKafkaProducer appKafkaProducer) {
        this.appKafkaProducer = appKafkaProducer;
    }

    public void delegateMessage(String message) {
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

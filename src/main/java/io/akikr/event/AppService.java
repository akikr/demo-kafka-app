package io.akikr.event;

import io.akikr.event.producer.AppKafkaProducer;
import org.springframework.stereotype.Service;

@Service
public class AppService {

    private final AppKafkaProducer appKafkaProducer;

    public AppService(AppKafkaProducer appKafkaProducer) {
        this.appKafkaProducer = appKafkaProducer;
    }

    public void delegateMessage(String message) {
        appKafkaProducer.sendMessage(message);
    }
}

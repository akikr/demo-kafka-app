package io.akikr.event.consumer;

import io.akikr.event.AppService;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(AppKafkaListener.class);

    @Value("${app.kafka.consumer.topics:app-in-topic}")
    private String[] appConsumerTopics;

    @Value("${spring.kafka.consumer.group-id:app-group}")
    private String appConsumerGroupId;

    private final AppService appService;

    public AppKafkaListener(AppService appService) {
        this.appService = appService;
    }

    @KafkaListener(
            topics = "${app.kafka.consumer.topics:app-in-topic}",
            groupId = "${spring.kafka.consumer.group-id:app-group}")
    public void listen(String message) {
        var consumers = Arrays.asList(appConsumerTopics);
        log.info("Received Message:[{}] from Kafka topics:{} and groupId:[{}]", message, consumers, appConsumerGroupId);
        int sizeInBytes = message.getBytes().length;
        log.info("Message size in bytes:[{}]", sizeInBytes);
        appService.delegateMessage(message);
    }
}

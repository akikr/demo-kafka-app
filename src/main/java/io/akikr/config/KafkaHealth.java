package io.akikr.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("kafka")
public class KafkaHealth implements HealthIndicator {

    @Value("${spring.kafka.consumer.bootstrap-servers:localhost:9092}")
    private String consumerBootstrapServers;

    @Value("${spring.kafka.producer.bootstrap-servers:localhost:9092}")
    private String producerBootstrapServers;

    @Override
    public Health health() {
        Map<String, Object> configs = new HashMap<>();
        String bootstrapServers = consumerBootstrapServers + "," + producerBootstrapServers;
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient client = AdminClient.create(configs)) {
            client.describeCluster().clusterId().get(5L, TimeUnit.SECONDS);

            return Health.up()
                    .withDetail("consumerBootstrapServers", consumerBootstrapServers)
                    .withDetail("producerBootstrapServers", producerBootstrapServers)
                    .build();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return Health.down(e).build();
        }
    }
}

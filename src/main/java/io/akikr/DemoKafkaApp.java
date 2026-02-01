package io.akikr;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoKafkaApp {

    private static final Logger log = LoggerFactory.getLogger(DemoKafkaApp.class);

    static void main(String[] args) {
        log.info("Starting app with arguments: {}", Arrays.asList(args));
        SpringApplication.run(DemoKafkaApp.class, args);
    }
}

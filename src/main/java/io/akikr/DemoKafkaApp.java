package io.akikr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class DemoKafkaApp {

	private static final Logger log = LoggerFactory.getLogger(DemoKafkaApp.class);

	static void main(String[] args) {
		SpringApplication.run(DemoKafkaApp.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(ServerProperties serverProperties) {
		return args -> {
			log.info("Starting app with arguments: {}", Arrays.asList(args));
		};
	}
}

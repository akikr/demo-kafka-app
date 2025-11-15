package io.akikr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoKafkaAppTests extends KafkaTestContainer {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("Test Spring Application Context Loads")
	void contextLoads() {
		// Verify Spring application context is not null
		assertThat(applicationContext).isNotNull();

		// Verify the main application class is loaded
		assertThat(applicationContext.getBean("demoKafkaApp")).isInstanceOf(DemoKafkaApp.class);

		// Verify Kafka container is running
		assertThat(KAFKA_CONTAINER.isRunning()).isTrue();
	}
}

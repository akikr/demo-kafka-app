package io.akikr.event;

import io.akikr.event.producer.AppKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class AppServiceTest {

    private AppService appService;

    @BeforeEach
    void setUp() {
        var appKafkaProducer = mock(AppKafkaProducer.class);
        appService = new AppService(appKafkaProducer);
    }

    @Test
    @DisplayName("delegateMessage should call sendMessage method of AppKafkaProducer")
    void delegateMessage() {
        //Act and Assert
        assertDoesNotThrow(() -> appService.delegateMessage(null));
        assertDoesNotThrow(() -> appService.delegateMessage(StringUtils.EMPTY));
        assertDoesNotThrow(() -> appService.delegateMessage(StringUtils.SPACE));

        assertDoesNotThrow(() -> appService.delegateMessage("Test Message"));
    }
}

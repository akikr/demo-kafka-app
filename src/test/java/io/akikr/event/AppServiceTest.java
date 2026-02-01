package io.akikr.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.akikr.event.producer.AppKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AppServiceTest {

    private AppKafkaProducer appKafkaProducer;
    private AppService appService;

    @BeforeEach
    void setUp() {
        this.appKafkaProducer = mock(AppKafkaProducer.class);
        appService = new AppService(appKafkaProducer);
    }

    @Test
    @DisplayName("delegateMessage should call sendMessage method of AppKafkaProducer")
    void delegateMessage() {
        // Arrange and Mock
        doNothing().when(appKafkaProducer).sendMessage(anyString());

        // Act and Assert
        assertDoesNotThrow(() -> appService.delegateMessage(null));
        assertDoesNotThrow(() -> appService.delegateMessage(""));
        assertDoesNotThrow(() -> appService.delegateMessage(" "));
        assertDoesNotThrow(() -> appService.delegateMessage("Test Message"));

        // Verify
        verify(appKafkaProducer, Mockito.times(4)).sendMessage(anyString());
    }
}

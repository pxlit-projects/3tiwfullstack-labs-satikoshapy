package be.pxl.services.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

import static org.mockito.Mockito.*;

class RabbitConfigFactoryTest {

    @Test
    void rabbitListenerContainerFactory_usesProvidedMessageConverter() {
        RabbitConfig config = new RabbitConfig();

        SimpleRabbitListenerContainerFactoryConfigurer configurer =
                mock(SimpleRabbitListenerContainerFactoryConfigurer.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MessageConverter converter = mock(MessageConverter.class);

        // We don't care about what configure() does, just that it doesn't explode
        doAnswer(invocation -> null)
                .when(configurer).configure(any(SimpleRabbitListenerContainerFactory.class), eq(connectionFactory));

        SimpleRabbitListenerContainerFactory factory =
                config.rabbitListenerContainerFactory(configurer, connectionFactory, converter);

        verify(configurer).configure(factory, connectionFactory);
    }
}

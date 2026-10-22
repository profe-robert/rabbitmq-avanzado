package com.example.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitListenerConfig {

    // Factory para procesar órdenes con concurrencia escalable (entre 3 y 5 hilos)
    @Bean(name = "orderListenerFactory")
    public SimpleRabbitListenerContainerFactory orderListenerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }

    // Factory para pagos con un solo consumidor concurrente (procesamiento secuencial)
    @Bean(name = "paymentListenerFactory")
    public SimpleRabbitListenerContainerFactory paymentListenerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        return factory;
    }
}
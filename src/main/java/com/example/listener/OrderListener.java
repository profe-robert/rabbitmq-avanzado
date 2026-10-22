package com.example.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderListener {

    // Listener asignado a la factory configurada previamente con ID para control dinámico
    @RabbitListener(
            id = "order-listener",
            queues = "orders.queue",
            containerFactory = "orderListenerFactory"
    )
    public void processOrder(String orderId) {
        try {
            log.info("Procesando orden: {}", orderId);
            Thread.sleep(2000); // Simulación de carga de trabajo (2 segundos)
            log.info("Orden procesada: {}", orderId);
        } catch (InterruptedException e) {
            log.error("Error al procesar orden: {}", orderId, e);
            Thread.currentThread().interrupt();
        }
    }
}
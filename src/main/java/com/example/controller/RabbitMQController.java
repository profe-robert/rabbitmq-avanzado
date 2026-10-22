package com.example.controller;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.service.RabbitResourceManager;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rabbitmq")
@RequiredArgsConstructor
public class RabbitMQController {

    private final RabbitResourceManager resourceManager;
    private final RabbitTemplate rabbitTemplate; // Inyectar RabbitTemplate

    // Endpoint para publicar una orden de prueba en orders.queue
    @PostMapping("/orders")
    public ResponseEntity<String> sendOrder(@RequestParam String orderId) {
        // Envía el mensaje directamente a orders.queue usando el exchange por defecto ("")
        rabbitTemplate.convertAndSend("", "orders.queue", orderId);
        return ResponseEntity.ok("Orden enviada a la cola: " + orderId);
    }

    // Envío en ráfaga indicando solo una cantidad numérica y el prefijo del mensaje
    @PostMapping("/orders/burst")
    public ResponseEntity<String> sendBurstOrders(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "ORD") String prefix) {

        for (int i = 1; i <= count; i++) {
            String orderId = prefix + "-" + i + "-" + String.format("%03d", i) + "-" + UUID.randomUUID().toString().substring(0, 4);
            rabbitTemplate.convertAndSend("", "orders.queue", orderId);
        }

        return ResponseEntity.ok("Se generaron y enviaron " + count + " órdenes con prefijo '" + prefix + "'");
    }

    // Endpoint POST para crear colas
    @PostMapping("/queues")
    public ResponseEntity<String> createQueue(@RequestParam String queueName) {
        resourceManager.createQueue(queueName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Cola '" + queueName + "' creada exitosamente");
    }

    // Endpoint POST para crear exchanges
    @PostMapping("/exchanges")
    public ResponseEntity<String> createExchange(@RequestParam String exchangeName) {
        resourceManager.createExchange(exchangeName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Exchange '" + exchangeName + "' creado exitosamente");
    }

    // Endpoint POST para crear enlaces (bindings)
    @PostMapping("/bindings")
    public ResponseEntity<String> createBinding(
            @RequestParam String queueName,
            @RequestParam String exchangeName,
            @RequestParam String routingKey) {
        resourceManager.createBinding(queueName, exchangeName, routingKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Binding creado: " + queueName + " <-> " + exchangeName);
    }

    // Endpoint GET para consultar el estado de una cola
    @GetMapping("/queues/{queueName}")
    public ResponseEntity<?> getQueueInfo(@PathVariable String queueName) {
        var info = resourceManager.getQueueInfo(queueName);
        return info != null ? ResponseEntity.ok(info) : ResponseEntity.notFound().build();
    }

    // Endpoint DELETE para eliminar una cola
    @DeleteMapping("/queues/{queueName}")
    public ResponseEntity<String> deleteQueue(@PathVariable String queueName) {
        resourceManager.deleteQueue(queueName);
        return ResponseEntity.ok("Cola '" + queueName + "' eliminada");
    }

    // Endpoint POST para purgar todos los mensajes de una cola
    @PostMapping("/queues/{queueName}/purge")
    public ResponseEntity<String> purgeQueue(@PathVariable String queueName) {
        resourceManager.purgeQueue(queueName);
        return ResponseEntity.ok("Cola '" + queueName + "' purgada");
    }
}
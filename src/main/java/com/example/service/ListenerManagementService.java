package com.example.service;

import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListenerManagementService {

    // Registro central de Spring AMQP para acceder a los listeners activos
    private final RabbitListenerEndpointRegistry registry;

    // Pausa el consumo deteniendo el contenedor del listener
    public void pauseListener(String listenerId) {
        try {
            MessageListenerContainer container = registry.getListenerContainer(listenerId);
            if (container != null && container.isRunning()) {
                container.stop();
                log.info("Listener pausado (detenido): {}", listenerId);
            } else if (container == null) {
                log.warn("No se encontró el listener con ID: {}", listenerId);
            }
        } catch (Exception e) {
            log.error("Error al pausar listener: {}", e.getMessage());
        }
    }

    // Reanuda el consumo iniciando nuevamente el contenedor
    public void resumeListener(String listenerId) {
        try {
            MessageListenerContainer container = registry.getListenerContainer(listenerId);
            if (container != null && !container.isRunning()) {
                container.start();
                log.info("Listener reanudado (iniciado): {}", listenerId);
            } else if (container == null) {
                log.warn("No se encontró el listener con ID: {}", listenerId);
            }
        } catch (Exception e) {
            log.error("Error al reanudar listener: {}", e.getMessage());
        }
    }

    // Retorna todos los IDs de los listeners registrados
    public List<String> getAllListenerIds() {
        return new ArrayList<>(registry.getListenerContainerIds());
    }
}
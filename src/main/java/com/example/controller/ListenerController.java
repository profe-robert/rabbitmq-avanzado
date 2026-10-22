package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.service.ListenerManagementService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/listeners")
@RequiredArgsConstructor
public class ListenerController {

    private final ListenerManagementService listenerService;

    // Retorna la lista de listeners disponibles
    @GetMapping
    public ResponseEntity<List<String>> getAllListeners() {
        return ResponseEntity.ok(listenerService.getAllListenerIds());
    }

    // Endpoint POST para pausar un listener por su ID
    @PostMapping("/{listenerId}/pause")
    public ResponseEntity<String> pauseListener(@PathVariable String listenerId) {
        listenerService.pauseListener(listenerId);
        return ResponseEntity.ok("Listener pausado: " + listenerId);
    }

    // Endpoint POST para reanudar un listener por su ID
    @PostMapping("/{listenerId}/resume")
    public ResponseEntity<String> resumeListener(@PathVariable String listenerId) {
        listenerService.resumeListener(listenerId);
        return ResponseEntity.ok("Listener reanudado: " + listenerId);
    }
}
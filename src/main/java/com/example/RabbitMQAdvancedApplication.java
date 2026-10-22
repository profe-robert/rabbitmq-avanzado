package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Habilita soporte para tareas programadas (@Scheduled)
public class RabbitMQAdvancedApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMQAdvancedApplication.class, args);
    }
}
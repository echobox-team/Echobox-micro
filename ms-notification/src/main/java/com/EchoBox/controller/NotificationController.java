package com.EchoBox.controller;

import com.EchoBox.model.NotificationRecord;
import com.EchoBox.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Support Service (ms-notification)", description = "Microsservico de Suporte de E-mail integrado via RabbitMQ e SMTP")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lista todas as notificacoes por e-mail enviadas (via RabbitMQ ou REST)")
    public List<NotificationRecord> findAll() {
        return notificationService.findAll();
    }

    @PostMapping("/send")
    @Operation(summary = "Dispara um e-mail manualmente via REST")
    public ResponseEntity<NotificationRecord> sendManual(@RequestBody Map<String, String> payload) {
        String to = payload.getOrDefault("recipientEmail", "contato@echobox.com");
        String subject = payload.getOrDefault("subject", "Notificacao Manual Echobox");
        String body = payload.getOrDefault("body", "Mensagem de teste enviada pelo ms-notification.");
        NotificationRecord record = notificationService.sendEmailAndRecord("MANUAL_EMAIL", to, subject, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }
}

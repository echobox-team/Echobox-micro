package com.EchoBox.service;

import com.EchoBox.model.NotificationRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<NotificationRecord> history = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(1);

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @RabbitListener(queues = "${rabbitmq.queue:echobox.notifications}")
    public void consumeRabbitMessage(String rawJson) {
        log.info("Received RabbitMQ message: {}", rawJson);
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String eventType = root.path("eventType").asText("NOTIFICATION");
            JsonNode data = root.path("data");

            String recipient = data.path("recipientEmail").asText("contato@echobox.com");
            String title = data.path("title").asText("Nova notificacao Echobox");
            String review = data.path("review").asText("");
            String subject = "[Echobox - " + eventType + "] " + title;
            String body = "Evento: " + eventType + "\nTítulo: " + title + "\nMensagem: " + review;

            sendEmailAndRecord(eventType, recipient, subject, body);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ message: {}", e.getMessage());
        }
    }

    public NotificationRecord sendEmailAndRecord(String eventType, String recipient, String subject, String body) {
        String deliveryStatus = "SENT_SMTP";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@echobox.com");
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("E-mail enviado com sucesso para {}", recipient);
        } catch (Exception e) {
            log.warn("SMTP server indisponivel ({}), registrando envio simulado em memoria.", e.getMessage());
            deliveryStatus = "LOGGED_IN_MEMORY";
        }

        NotificationRecord record = NotificationRecord.builder()
                .id(counter.getAndIncrement())
                .eventType(eventType)
                .recipientEmail(recipient)
                .subject(subject)
                .body(body)
                .status(deliveryStatus)
                .sentAt(LocalDateTime.now())
                .build();
        history.add(record);
        return record;
    }

    public List<NotificationRecord> findAll() {
        return Collections.unmodifiableList(history);
    }
}

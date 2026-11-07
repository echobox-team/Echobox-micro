package com.EchoBox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecord {
    private Integer id;
    private String eventType;
    private String recipientEmail;
    private String subject;
    private String body;
    private String status;
    private LocalDateTime sentAt;
}

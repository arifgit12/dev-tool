package com.oraclejms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JmsMessage {
    private String messageId;
    private String queue;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        SENT, RECEIVED
    }
}

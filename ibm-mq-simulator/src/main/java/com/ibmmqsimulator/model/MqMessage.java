package com.ibmmqsimulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage {
    private String messageId;
    private String content;
    private String queue;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        SENT, RECEIVED
    }
}

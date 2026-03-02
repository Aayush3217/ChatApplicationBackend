package com.chat.chatapplicationbackend.entities;

import com.chat.chatapplicationbackend.Enum.MessageStatus;
import com.chat.chatapplicationbackend.Enum.MessageType;
import lombok.*;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    public Message(String sender, String content, MessageType type, MessageStatus status) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    private String sender;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private String id = UUID.randomUUID().toString();
    private boolean deleted = false;
}

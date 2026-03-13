package com.chat.chatapplicationbackend.entities;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    private String id;

    private String roomId;

    // -- Room metadata --
    private String roomName;
    private String roomDescription;
    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();

    // -- Members: username -> UserPresence --
    private Map<String, UserPresence> members = new HashMap<>();

    // -- Messages --
    private List<Message> message = new ArrayList<>();

    // -- Pinned message IDs --
    private List<String> pinnedMessageIds = new ArrayList<>();

    // -- Message retention (disappearing messages) --
    private Integer messageRetentionHours; // null = forever

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPresence {
        private boolean online = false;
        private LocalDateTime lastSeen;
        private String typingStatus; // null or "typing..."
    }
}


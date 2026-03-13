package com.chat.chatapplicationbackend.entities;

import com.chat.chatapplicationbackend.Enum.MessageStatus;
import com.chat.chatapplicationbackend.Enum.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String id = UUID.randomUUID().toString();

    private String sender;

    private String content;

    private MessageType type;

    private LocalDateTime timestamp;

    private MessageStatus status;

    // -- Soft delete --
    private boolean deleted = false;

    // -- Edit history --
    private boolean edited = false;
    private String originalContent;
    private LocalDateTime editedAt;

    // -- Reactions: emoji -> list of usernames who reacted --
    private Map<String, List<String>> reactions = new HashMap<>();

    // -- Reply / thread --
    private String replyToMessageId;
    private String replyToSenderName;
    private String replyToContentPreview;

    // -- Pinned messages --
    private boolean pinned = false;
    private LocalDateTime pinnedAt;
    private String pinnedBy;

    // -- Scheduled messages --
    private boolean scheduled = false;
    private LocalDateTime scheduledAt;

    // -- Read receipts: list of usernames who have seen this message --
    private List<String> seenBy = new ArrayList<>();

    // -- File metadata (for IMAGE/FILE/VIDEO types) --
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMimeType;

    public Message(String sender, String content, MessageType type, MessageStatus status) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}

package com.chat.chatapplicationbackend.payload;

import com.chat.chatapplicationbackend.Enum.MessageType;
import lombok.*;
import java.time.LocalDateTime;

@Setter @Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class MessageRequest {
    private String content;
    private String sender;
    private String roomId;
    private MessageType type;

    // Reply support
    private String replyToMessageId;

    // Scheduled message
    private LocalDateTime scheduledAt;

    // File metadata
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileMimeType;
}

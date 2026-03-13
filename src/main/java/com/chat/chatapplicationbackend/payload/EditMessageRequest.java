package com.chat.chatapplicationbackend.payload;

import lombok.*;

@Setter @Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class EditMessageRequest {
    private String roomId;
    private String messageId;
    private String sender;
    private String newContent;
}

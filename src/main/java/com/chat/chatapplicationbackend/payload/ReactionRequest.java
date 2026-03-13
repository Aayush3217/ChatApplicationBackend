package com.chat.chatapplicationbackend.payload;

import lombok.*;

// Payload for adding/removing emoji reactions
@Setter @Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class ReactionRequest {
    private String roomId;
    private String messageId;
    private String sender;     // username reacting
    private String emoji;      // e.g. "👍", "❤️", "😂"
}

package com.chat.chatapplicationbackend.payload;

import com.chat.chatapplicationbackend.Enum.MessageType;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {
    private String content;
    private String sender;
    private String roomId;
    private MessageType type; // enum
}

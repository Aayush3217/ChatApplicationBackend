package com.chat.chatapplicationbackend.payload;

import lombok.*;

@Setter @Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class CreateRoomRequest {
    private String roomId;
    private String roomName;
    private String roomDescription;
    private String createdBy;
    private Integer messageRetentionHours; // null = keep forever
}
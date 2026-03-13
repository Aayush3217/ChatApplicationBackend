package com.chat.chatapplicationbackend.controllers;

import com.chat.chatapplicationbackend.Enum.MessageStatus;
import com.chat.chatapplicationbackend.Enum.MessageType;
import com.chat.chatapplicationbackend.config.AppConstant;
import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.payload.*;
import com.chat.chatapplicationbackend.repositories.RoomRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@CrossOrigin(AppConstant.FRONT_END_BASE_URL)
public class ChatController {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(RoomRepository roomRepository, SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ─────────────────────────────────────────────
    // 1. SEND MESSAGE (with reply + scheduling support)
    // ─────────────────────────────────────────────
    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message sendMessage(
            @DestinationVariable String roomId,
            MessageRequest request
    ) {
        Room room = getRoomOrThrow(request.getRoomId());

        Message message = new Message();
        message.setContent(request.getContent());
        message.setSender(request.getSender());
        message.setType(request.getType() != null ? request.getType() : MessageType.TEXT);
        message.setStatus(MessageStatus.SENT);
        message.setTimestamp(LocalDateTime.now());

        // File metadata
        if (request.getFileUrl() != null) {
            message.setFileUrl(request.getFileUrl());
            message.setFileName(request.getFileName());
            message.setFileSize(request.getFileSize());
            message.setFileMimeType(request.getFileMimeType());
        }

        // Reply chain
        if (request.getReplyToMessageId() != null) {
            message.setReplyToMessageId(request.getReplyToMessageId());
            room.getMessage().stream()
                    .filter(m -> m.getId().equals(request.getReplyToMessageId()))
                    .findFirst()
                    .ifPresent(original -> {
                        message.setReplyToSenderName(original.getSender());
                        String preview = original.getContent();
                        message.setReplyToContentPreview(
                                preview != null && preview.length() > 60
                                        ? preview.substring(0, 60) + "…"
                                        : preview
                        );
                    });
        }

        // Scheduled message — store but don't broadcast yet
        if (request.getScheduledAt() != null) {
            message.setScheduled(true);
            message.setScheduledAt(request.getScheduledAt());
            room.getMessage().add(message);
            roomRepository.save(room);
            return null; // not broadcast until scheduled time
        }

        room.getMessage().add(message);

        // Clean up expired messages if retention is set
        if (room.getMessageRetentionHours() != null) {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(room.getMessageRetentionHours());
            room.getMessage().removeIf(m -> m.getTimestamp() != null && m.getTimestamp().isBefore(cutoff));
        }

        roomRepository.save(room);
        return message;
    }

    // ─────────────────────────────────────────────
    // 2. TYPING INDICATOR
    // ─────────────────────────────────────────────
    @MessageMapping("/typing/{roomId}")
    @SendTo("/topic/typing/{roomId}")
    public Map<String, Object> typing(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ) {
        String sender = payload.get("sender");
        boolean isTyping = Boolean.parseBoolean(payload.getOrDefault("isTyping", "true"));
        return Map.of("sender", sender, "isTyping", isTyping);
    }

    // ─────────────────────────────────────────────
    // 3. SOFT DELETE
    // ─────────────────────────────────────────────
    @MessageMapping("/delete/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message deleteMessage(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ) {
        String messageId = payload.get("messageId");
        String requester = payload.get("sender");
        Room room = getRoomOrThrow(roomId);

        for (Message msg : room.getMessage()) {
            if (msg.getId().equals(messageId)) {
                // Only sender can delete their own message
                if (!msg.getSender().equals(requester)) return null;
                msg.setDeleted(true);
                msg.setContent("This message was deleted");
                roomRepository.save(room);
                return msg;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // 4. EDIT MESSAGE (UNIQUE FEATURE)
    // ─────────────────────────────────────────────
    @MessageMapping("/edit/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message editMessage(
            @DestinationVariable String roomId,
            EditMessageRequest request
    ) {
        Room room = getRoomOrThrow(roomId);

        for (Message msg : room.getMessage()) {
            if (msg.getId().equals(request.getMessageId())
                    && msg.getSender().equals(request.getSender())
                    && !msg.isDeleted()) {

                if (!msg.isEdited()) {
                    msg.setOriginalContent(msg.getContent()); // preserve original
                }
                msg.setContent(request.getNewContent());
                msg.setEdited(true);
                msg.setEditedAt(LocalDateTime.now());
                roomRepository.save(room);
                return msg;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // 5. EMOJI REACTIONS (UNIQUE FEATURE)
    // ─────────────────────────────────────────────
    @MessageMapping("/react/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message reactToMessage(
            @DestinationVariable String roomId,
            ReactionRequest request
    ) {
        Room room = getRoomOrThrow(roomId);

        for (Message msg : room.getMessage()) {
            if (msg.getId().equals(request.getMessageId())) {
                Map<String, List<String>> reactions = msg.getReactions();
                List<String> users = reactions.computeIfAbsent(request.getEmoji(), k -> new ArrayList<>());

                // Toggle: add if not present, remove if already reacted
                if (users.contains(request.getSender())) {
                    users.remove(request.getSender());
                    if (users.isEmpty()) reactions.remove(request.getEmoji());
                } else {
                    users.add(request.getSender());
                }

                roomRepository.save(room);
                return msg;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // 6. PIN / UNPIN MESSAGE (UNIQUE FEATURE)
    // ─────────────────────────────────────────────
    @MessageMapping("/pin/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message pinMessage(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ) {
        String messageId = payload.get("messageId");
        String pinnedBy = payload.get("sender");
        Room room = getRoomOrThrow(roomId);

        for (Message msg : room.getMessage()) {
            if (msg.getId().equals(messageId)) {
                boolean newPinnedState = !msg.isPinned();
                msg.setPinned(newPinnedState);
                msg.setPinnedBy(newPinnedState ? pinnedBy : null);
                msg.setPinnedAt(newPinnedState ? LocalDateTime.now() : null);

                if (newPinnedState) {
                    room.getPinnedMessageIds().add(messageId);
                } else {
                    room.getPinnedMessageIds().remove(messageId);
                }

                roomRepository.save(room);

                // Broadcast a system notification about the pin
                Message systemMsg = new Message();
                systemMsg.setType(MessageType.SYSTEM);
                systemMsg.setSender("System");
                systemMsg.setTimestamp(LocalDateTime.now());
                systemMsg.setStatus(MessageStatus.SENT);
                systemMsg.setContent(newPinnedState
                        ? pinnedBy + " pinned a message"
                        : pinnedBy + " unpinned a message");
                messagingTemplate.convertAndSend("/topic/room/" + roomId, systemMsg);

                return msg;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // 7. MARK AS SEEN / READ RECEIPTS (UNIQUE FEATURE)
    // ─────────────────────────────────────────────
    @MessageMapping("/seen/{roomId}")
    @SendTo("/topic/seen/{roomId}")
    public Map<String, Object> markAsSeen(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ) {
        String username = payload.get("sender");
        String messageId = payload.get("messageId");
        Room room = getRoomOrThrow(roomId);

        for (Message msg : room.getMessage()) {
            if (msg.getId().equals(messageId)) {
                if (!msg.getSeenBy().contains(username)) {
                    msg.getSeenBy().add(username);
                    msg.setStatus(MessageStatus.SEEN);
                    roomRepository.save(room);
                }
                break;
            }
        }
        return Map.of("messageId", messageId, "seenBy", username);
    }

    // ─────────────────────────────────────────────
    // 8. ONLINE PRESENCE (UNIQUE FEATURE)
    // ─────────────────────────────────────────────
    @MessageMapping("/presence/{roomId}")
    @SendTo("/topic/presence/{roomId}")
    public Map<String, Object> updatePresence(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ) {
        String username = payload.get("sender");
        boolean online = Boolean.parseBoolean(payload.getOrDefault("online", "true"));
        Room room = getRoomOrThrow(roomId);

        Room.UserPresence presence = room.getMembers()
                .computeIfAbsent(username, k -> new Room.UserPresence());
        presence.setOnline(online);
        presence.setLastSeen(online ? null : LocalDateTime.now());
        room.getMembers().put(username, presence);
        roomRepository.save(room);

        return Map.of("sender", username, "online", online,
                "lastSeen", presence.getLastSeen() != null
                        ? presence.getLastSeen().toString() : "");
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────
    private Room getRoomOrThrow(String roomId) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) throw new RuntimeException("Room not found: " + roomId);
        return room;
    }
}

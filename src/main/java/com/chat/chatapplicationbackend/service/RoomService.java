package com.chat.chatapplicationbackend.service;


import com.chat.chatapplicationbackend.Enum.MessageType;
import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.payload.CreateRoomRequest;
import com.chat.chatapplicationbackend.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    // ─────────────────────────────────────────────
    // CREATE ROOM
    // ─────────────────────────────────────────────
    public Room createRoom(CreateRoomRequest request) {
        if (roomRepository.findByRoomId(request.getRoomId()) != null) {
            throw new RuntimeException("Room already exists: " + request.getRoomId());
        }
        Room room = new Room();
        room.setRoomId(request.getRoomId());
        room.setRoomName(request.getRoomName() != null ? request.getRoomName() : request.getRoomId());
        room.setRoomDescription(request.getRoomDescription());
        room.setCreatedBy(request.getCreatedBy());
        room.setCreatedAt(LocalDateTime.now());
        room.setMessageRetentionHours(request.getMessageRetentionHours());
        return roomRepository.save(room);
    }

    // ─────────────────────────────────────────────
    // GET ROOM
    // ─────────────────────────────────────────────
    public Room getRoom(String roomId) {
        Room room = roomRepository.findByRoomId(roomId);
        if (room == null) throw new RuntimeException("Room not found: " + roomId);
        return room;
    }

    // ─────────────────────────────────────────────
    // PAGINATED MESSAGES (newest-last ordering)
    // ─────────────────────────────────────────────
    public List<Message> getMessages(String roomId, int page, int size) {
        Room room = getRoom(roomId);
        List<Message> messages = room.getMessage().stream()
                .filter(m -> !m.isScheduled()) // exclude scheduled not yet sent
                .collect(Collectors.toList());

        int total = messages.size();
        int start = Math.max(0, total - (page + 1) * size);
        int end = Math.min(total, start + size);
        return messages.subList(start, end);
    }

    // ─────────────────────────────────────────────
    // UNIQUE FEATURE: SEARCH MESSAGES
    // ─────────────────────────────────────────────
    public List<Message> searchMessages(String roomId, String query) {
        Room room = getRoom(roomId);
        if (query == null || query.isBlank()) return Collections.emptyList();
        String lowerQuery = query.toLowerCase();

        return room.getMessage().stream()
                .filter(m -> !m.isDeleted())
                .filter(m -> m.getType() == MessageType.TEXT || m.getType() == null)
                .filter(m -> m.getContent() != null
                        && m.getContent().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UNIQUE FEATURE: PINNED MESSAGES
    // ─────────────────────────────────────────────
    public List<Message> getPinnedMessages(String roomId) {
        Room room = getRoom(roomId);
        return room.getMessage().stream()
                .filter(Message::isPinned)
                .sorted(Comparator.comparing(Message::getPinnedAt).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // UNIQUE FEATURE: ONLINE PRESENCE
    // ─────────────────────────────────────────────
    public Map<String, Room.UserPresence> getRoomPresence(String roomId) {
        return getRoom(roomId).getMembers();
    }

    // ─────────────────────────────────────────────
    // UNIQUE FEATURE: ROOM STATISTICS
    // ─────────────────────────────────────────────
    public Map<String, Object> getRoomStats(String roomId) {
        Room room = getRoom(roomId);
        List<Message> messages = room.getMessage();

        long totalMessages = messages.stream().filter(m -> !m.isDeleted()).count();
        long deletedMessages = messages.stream().filter(Message::isDeleted).count();
        long editedMessages = messages.stream().filter(Message::isEdited).count();
        long imageMessages = messages.stream().filter(m -> m.getType() == MessageType.IMAGE).count();
        long fileMessages = messages.stream().filter(m -> m.getType() == MessageType.FILE).count();
        long pinnedMessages = messages.stream().filter(Message::isPinned).count();
        long onlineMembers = room.getMembers().values().stream()
                .filter(Room.UserPresence::isOnline).count();

        // Top sender
        Map<String, Long> senderCounts = messages.stream()
                .filter(m -> !m.isDeleted() && m.getSender() != null)
                .collect(Collectors.groupingBy(Message::getSender, Collectors.counting()));
        String topSender = senderCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("roomId", roomId);
        stats.put("roomName", room.getRoomName());
        stats.put("totalMessages", totalMessages);
        stats.put("deletedMessages", deletedMessages);
        stats.put("editedMessages", editedMessages);
        stats.put("imageMessages", imageMessages);
        stats.put("fileMessages", fileMessages);
        stats.put("pinnedMessages", pinnedMessages);
        stats.put("totalMembers", room.getMembers().size());
        stats.put("onlineMembers", onlineMembers);
        stats.put("topSender", topSender);
        stats.put("createdAt", room.getCreatedAt());
        stats.put("retentionHours", room.getMessageRetentionHours());
        return stats;
    }

    // ─────────────────────────────────────────────
    // UPDATE ROOM SETTINGS
    // ─────────────────────────────────────────────
    public Room updateRoom(String roomId, Map<String, Object> updates) {
        Room room = getRoom(roomId);
        if (updates.containsKey("roomName"))
            room.setRoomName((String) updates.get("roomName"));
        if (updates.containsKey("roomDescription"))
            room.setRoomDescription((String) updates.get("roomDescription"));
        if (updates.containsKey("messageRetentionHours")) {
            Object val = updates.get("messageRetentionHours");
            room.setMessageRetentionHours(val != null ? (Integer) val : null);
        }
        return roomRepository.save(room);
    }
}

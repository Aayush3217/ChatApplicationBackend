package com.chat.chatapplicationbackend.controllers;

import com.chat.chatapplicationbackend.config.AppConstant;
import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.payload.CreateRoomRequest;
import com.chat.chatapplicationbackend.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin(AppConstant.FRONT_END_BASE_URL)
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // Create room (now with name, description, retention settings)
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request) {
        try {
            Room room = roomService.createRoom(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Join room
    @GetMapping("/{roomId}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId) {
        try {
            Room room = roomService.getRoom(roomId);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get paginated messages
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<Message> messages = roomService.getMessages(roomId, page, size);
        return ResponseEntity.ok(messages);
    }

    // ── UNIQUE FEATURE: Search messages ──
    @GetMapping("/{roomId}/messages/search")
    public ResponseEntity<List<Message>> searchMessages(
            @PathVariable String roomId,
            @RequestParam String query
    ) {
        List<Message> results = roomService.searchMessages(roomId, query);
        return ResponseEntity.ok(results);
    }

    // ── UNIQUE FEATURE: Get pinned messages ──
    @GetMapping("/{roomId}/pinned")
    public ResponseEntity<List<Message>> getPinnedMessages(@PathVariable String roomId) {
        List<Message> pinned = roomService.getPinnedMessages(roomId);
        return ResponseEntity.ok(pinned);
    }

    // ── UNIQUE FEATURE: Get online members ──
    @GetMapping("/{roomId}/presence")
    public ResponseEntity<?> getPresence(@PathVariable String roomId) {
        Map<String, Room.UserPresence> presence = roomService.getRoomPresence(roomId);
        return ResponseEntity.ok(presence);
    }

    // ── UNIQUE FEATURE: Get room statistics ──
    @GetMapping("/{roomId}/stats")
    public ResponseEntity<?> getRoomStats(@PathVariable String roomId) {
        Map<String, Object> stats = roomService.getRoomStats(roomId);
        return ResponseEntity.ok(stats);
    }

    // ── UNIQUE FEATURE: Update room settings (name, description, retention) ──
    @PatchMapping("/{roomId}")
    public ResponseEntity<?> updateRoom(
            @PathVariable String roomId,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            Room updated = roomService.updateRoom(roomId, updates);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


package com.chat.chatapplicationbackend.controllers;

import com.chat.chatapplicationbackend.config.AppConstant;
import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin(AppConstant.FRONT_END_BASE_URL)
public class RoomController {

    private RoomService roomService;

    public RoomController(RoomService roomService){
        this.roomService = roomService;
    }

    //create room
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody String roomId){
        try{
            Room room = roomService.createRoom(roomId);
            return ResponseEntity.status(HttpStatus.CREATED).body(room);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //join room
    @GetMapping("/{roomId}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId){
        try{
            Room room = roomService.getRoom(roomId);
            return ResponseEntity.ok(room);
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //get message
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        List<Message> messages = roomService.getMessages(roomId, page, size);
        return ResponseEntity.ok(messages);
    }

}

package com.chat.chatapplicationbackend.controllers;

import com.chat.chatapplicationbackend.Enum.MessageStatus;
import com.chat.chatapplicationbackend.config.AppConstant;
import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.payload.MessageRequest;
import com.chat.chatapplicationbackend.repositories.RoomRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@CrossOrigin(AppConstant.FRONT_END_BASE_URL)
public class ChatController {

    private RoomRepository roomRepository;

    public ChatController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @MessageMapping("/sendMessage/{roomId}") // message received
    @SendTo("/topic/room/{roomId}")   // message send
    public Message sendMessage(
            @DestinationVariable String roomId,
            @RequestBody MessageRequest request
    ){
        Room room = roomRepository.findByRoomId(request.getRoomId());
        Message message = new Message();
        message.setContent(request.getContent());
        message.setSender(request.getSender());
        message.setType(request.getType()); // typing indicater
        message.setStatus(MessageStatus.SENT);
        message.setTimestamp(LocalDateTime.now());
        if(room != null){
            room.getMessage().add(message);
            roomRepository.save(room);
        }else{
            throw new RuntimeException("Room not found");
        }
        return message;
    }

    // Typing Indicator
    @MessageMapping("/typing/{roomId}")
    @SendTo("/topic/typing/{roomId}")
    public String typing(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ) {
        return payload.get("sender");
    }

    @MessageMapping("/delete/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message deleteMessage(
            @DestinationVariable String roomId,
            Map<String, String> payload
    ){
        String messageId = payload.get("messageId");
        Room room = roomRepository.findByRoomId(roomId);
        if(room == null) return null;

        for(Message msg : room.getMessage()){
            if(msg.getId().equals(messageId)){
                msg.setDeleted(true);
                msg.setContent("This message was deleted");
                roomRepository.save(room);
                return msg;
            }
        }
        return null;
    }

}

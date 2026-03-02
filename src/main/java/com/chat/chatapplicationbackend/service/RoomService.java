package com.chat.chatapplicationbackend.service;

import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.repositories.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoomService {

    private RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    // create room
    public Room createRoom(String roomId) {
        // room is already there
        if(roomRepository.findByRoomId(roomId)!=null){
            throw new RuntimeException("Room already exists");
        }

        // craete new room
        Room room = new Room();
        room.setRoomId(roomId);
        return roomRepository.save(room);
    }

    // join room
    public Room getRoom(String roomId){
        Room room = roomRepository.findByRoomId(roomId);
        if(room==null){
            throw new RuntimeException("Room not found!!");
        }
        return room;
    }

    // get message
    public List<Message> getMessages(String roomId, int page, int size) {

        Room room = getRoom(roomId);

        List<Message> messages = room.getMessage();

        int start = Math.max(0, messages.size() - (page + 1) * size);
        int end = Math.min(messages.size(), start + size);

        return messages.subList(start, end);
    }

}

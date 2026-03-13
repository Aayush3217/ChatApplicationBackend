package com.chat.chatapplicationbackend.service;

import com.chat.chatapplicationbackend.Enum.MessageStatus;
import com.chat.chatapplicationbackend.entities.Message;
import com.chat.chatapplicationbackend.entities.Room;
import com.chat.chatapplicationbackend.repositories.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledMessageService {

    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ScheduledMessageService(RoomRepository roomRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Runs every 30 seconds — checks for scheduled messages ready to send
    @Scheduled(fixedDelay = 30_000)
    public void dispatchScheduledMessages() {
        List<Room> rooms = roomRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Room room : rooms) {
            boolean dirty = false;
            for (Message msg : room.getMessage()) {
                if (msg.isScheduled()
                        && msg.getScheduledAt() != null
                        && msg.getScheduledAt().isBefore(now)) {

                    msg.setScheduled(false);
                    msg.setStatus(MessageStatus.SENT);
                    msg.setTimestamp(now); // set actual send time
                    messagingTemplate.convertAndSend(
                            "/topic/room/" + room.getRoomId(), msg);
                    dirty = true;
                }
            }
            if (dirty) roomRepository.save(room);
        }
    }
}

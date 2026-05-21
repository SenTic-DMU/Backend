package com.project.sentic.domain.room.dto;

import com.project.sentic.domain.room.entity.Room;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomResponse {

    private Long id;
    private String title;
    private String description;
    private Room.RoomType roomType;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .description(room.getDescription())
                .roomType(room.getRoomType())
                .lastMessage(room.getLastMessage())
                .lastMessageAt(room.getLastMessageAt())
                .createdAt(room.getCreatedAt())
                .build();
    }
}
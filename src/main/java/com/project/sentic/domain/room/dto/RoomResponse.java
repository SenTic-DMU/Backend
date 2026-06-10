package com.project.sentic.domain.room.dto;

import com.project.sentic.domain.room.entity.Room;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomResponse {

    private Long id;
    private String roomName;
    private String situation;
    private Room.RoomType roomType;
    private Room.Difficulty difficulty;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    public static RoomResponse from(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomName(room.getRoomName())
                .situation(room.getSituation())
                .roomType(room.getRoomType())
                .difficulty(room.getDifficulty())
                .lastActiveAt(room.getLastActiveAt())
                .createdAt(room.getCreatedAt())
                .build();
    }
}

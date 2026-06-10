package com.project.sentic.domain.room.dto;

import com.project.sentic.domain.room.entity.Room;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RoomCreateRequest {

    private String roomName;
    private String situation;
    private Room.RoomType roomType;
    private Room.Difficulty difficulty;
    private List<CharacterRequest> characters;

    @Getter
    @NoArgsConstructor
    public static class CharacterRequest {
        private String name;
        private String iconType;
        private String personality;
    }
}

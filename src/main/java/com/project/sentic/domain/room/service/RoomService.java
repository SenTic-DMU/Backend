package com.project.sentic.domain.room.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sentic.domain.room.dto.RoomCreateRequest;
import com.project.sentic.domain.room.dto.RoomResponse;
import com.project.sentic.domain.room.entity.Room;
import com.project.sentic.domain.room.repository.RoomRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RoomResponse createRoom(Long userId, RoomCreateRequest request) {
        String charactersJson = serializeCharacters(request.getCharacters());

        Room room = Room.builder()
                .userId(userId)
                .roomName(request.getRoomName())
                .situation(request.getSituation())
                .roomType(request.getRoomType())
                .difficulty(request.getDifficulty())
                .characters(charactersJson)
                .build();

        return RoomResponse.from(roomRepository.save(room));
    }

    public List<RoomResponse> getRooms(Long userId, Room.RoomType roomType) {
        return roomRepository
                .findByUserIdAndRoomTypeAndDeletedFalseOrderByLastActiveAtDesc(userId, roomType)
                .stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRoom(Long userId, Long roomId) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        room.delete();
    }

    private String serializeCharacters(List<RoomCreateRequest.CharacterRequest> characters) {
        if (characters == null || characters.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            return objectMapper.writeValueAsString(characters);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}

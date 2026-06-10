package com.project.sentic.domain.room.service;

import com.project.sentic.domain.room.dto.RoomCreateRequest;
import com.project.sentic.domain.room.dto.RoomResponse;
import com.project.sentic.domain.room.entity.Room;
import com.project.sentic.domain.room.repository.RoomRepository;
import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // 방 생성
    @Transactional
    public RoomResponse createRoom(Long userId, RoomCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Room room = Room.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .roomType(request.getRoomType())
                .build();

        return RoomResponse.from(roomRepository.save(room));
    }

    // 방 목록 조회
    public List<RoomResponse> getRooms(Long userId, Room.RoomType roomType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return roomRepository
                .findByUserAndRoomTypeAndDeletedFalseOrderByLastMessageAtDesc(user, roomType)
                .stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    // 방 삭제
    @Transactional
    public void deleteRoom(Long userId, Long roomId) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        room.delete();
    }
}
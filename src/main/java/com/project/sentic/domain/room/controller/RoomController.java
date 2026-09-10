package com.project.sentic.domain.room.controller;

import com.project.sentic.domain.room.dto.RoomCreateRequest;
import com.project.sentic.domain.room.dto.RoomResponse;
import com.project.sentic.domain.room.entity.Room;
import com.project.sentic.domain.room.service.RoomService;
import com.project.sentic.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 방 생성
    @PostMapping
    public ApiResponse<RoomResponse> createRoom(
            @AuthenticationPrincipal Long userId,
            @RequestBody RoomCreateRequest request) {
        return ApiResponse.success(roomService.createRoom(userId, request));
    }

    // 방 목록 조회
    @GetMapping
    public ApiResponse<List<RoomResponse>> getRooms(
            @AuthenticationPrincipal Long userId,
            @RequestParam Room.RoomType roomType) {
        return ApiResponse.success(roomService.getRooms(userId, roomType));
    }

    // 방 삭제
    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> deleteRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        roomService.deleteRoom(userId, roomId);
        return ApiResponse.success(null);
    }

    // 휴지통 목록 조회
    @GetMapping("/trash")
    public ApiResponse<List<RoomResponse>> getTrash(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(roomService.getTrash(userId));
    }

    // 방 복원
    @PostMapping("/{roomId}/restore")
    public ApiResponse<RoomResponse> restoreRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        return ApiResponse.success(roomService.restoreRoom(userId, roomId));
    }

    // 방 영구 삭제
    @DeleteMapping("/{roomId}/permanent")
    public ApiResponse<Void> permanentDeleteRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        roomService.permanentDeleteRoom(userId, roomId);
        return ApiResponse.success(null);
    }
}
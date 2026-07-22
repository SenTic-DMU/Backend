package com.project.sentic.domain.announcement.controller;

import com.project.sentic.domain.admin.dto.AnnouncementResponse;
import com.project.sentic.domain.announcement.entity.Announcement;
import com.project.sentic.domain.announcement.repository.AnnouncementRepository;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Announcement", description = "공지사항 API")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    @Operation(summary = "공지사항 목록 조회")
    @GetMapping
    public ApiResponse<List<AnnouncementResponse>> getAnnouncements() {
        List<AnnouncementResponse> responses = announcementRepository
                .findByDeletedFalseOrderByPinnedDescCreatedAtDesc()
                .stream()
                .map(AnnouncementResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }
}
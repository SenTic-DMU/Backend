package com.project.sentic.domain.admin.controller;

import com.project.sentic.domain.admin.dto.*;
import com.project.sentic.domain.admin.service.AdminService;
import com.project.sentic.domain.faq.dto.FaqResponse;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── 회원 관리 ──────────────────────────────────

    @Operation(summary = "회원 목록 조회 (검색)")
    @GetMapping("/users")
    public ApiResponse<List<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.getUsers(keyword));
    }

    @Operation(summary = "회원 상세 조회")
    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(adminService.getUser(userId));
    }

    @Operation(summary = "회원 상태 변경 (ACTIVE, INACTIVE, BANNED)")
    @PutMapping("/users/{userId}/status")
    public ApiResponse<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateRequest request) {
        return ApiResponse.success(adminService.updateUserStatus(userId, request));
    }

    // ── 공지사항 ──────────────────────────────────

    @Operation(summary = "공지사항 목록 조회")
    @GetMapping("/announcements")
    public ApiResponse<List<AnnouncementResponse>> getAnnouncements() {
        return ApiResponse.success(adminService.getAnnouncements());
    }

    @Operation(summary = "공지사항 작성")
    @PostMapping("/announcements")
    public ApiResponse<AnnouncementResponse> createAnnouncement(
            @AuthenticationPrincipal Long adminId,
            @RequestBody AnnouncementRequest request) {
        return ApiResponse.success(adminService.createAnnouncement(adminId, request));
    }

    @Operation(summary = "공지사항 수정")
    @PutMapping("/announcements/{announcementId}")
    public ApiResponse<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long announcementId,
            @RequestBody AnnouncementRequest request) {
        return ApiResponse.success(adminService.updateAnnouncement(announcementId, request));
    }

    @Operation(summary = "공지사항 삭제")
    @DeleteMapping("/announcements/{announcementId}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long announcementId) {
        adminService.deleteAnnouncement(announcementId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "공지사항 상단 고정 토글")
    @PutMapping("/announcements/{announcementId}/pin")
    public ApiResponse<AnnouncementResponse> togglePin(@PathVariable Long announcementId) {
        return ApiResponse.success(adminService.togglePin(announcementId));
    }

    // ── FAQ 관리 ──────────────────────────────────

    @Operation(summary = "FAQ 목록 조회")
    @GetMapping("/faq")
    public ApiResponse<List<FaqResponse>> getFaqs() {
        return ApiResponse.success(adminService.getFaqs());
    }

    @Operation(summary = "FAQ 작성")
    @PostMapping("/faq")
    public ApiResponse<FaqResponse> createFaq(@RequestBody AdminFaqRequest request) {
        return ApiResponse.success(adminService.createFaq(request));
    }

    @Operation(summary = "FAQ 수정")
    @PutMapping("/faq/{faqId}")
    public ApiResponse<FaqResponse> updateFaq(
            @PathVariable Long faqId,
            @RequestBody AdminFaqRequest request) {
        return ApiResponse.success(adminService.updateFaq(faqId, request));
    }

    @Operation(summary = "FAQ 삭제")
    @DeleteMapping("/faq/{faqId}")
    public ApiResponse<Void> deleteFaq(@PathVariable Long faqId) {
        adminService.deleteFaq(faqId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "FAQ 순서 변경")
    @PutMapping("/faq/{faqId}/order")
    public ApiResponse<FaqResponse> updateFaqOrder(
            @PathVariable Long faqId,
            @RequestBody FaqOrderUpdateRequest request) {
        return ApiResponse.success(adminService.updateFaqOrder(faqId, request));
    }

    // ── 결제 내역 ──────────────────────────────────

    @Operation(summary = "전체 결제 내역 (상태별 필터)")
    @GetMapping("/payments")
    public ApiResponse<List<AdminPaymentResponse>> getPayments(
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.getPayments(status));
    }

    @Operation(summary = "회원별 결제 내역")
    @GetMapping("/payments/user/{userId}")
    public ApiResponse<List<AdminPaymentResponse>> getUserPayments(@PathVariable Long userId) {
        return ApiResponse.success(adminService.getUserPayments(userId));
    }
}
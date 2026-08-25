package com.project.sentic.domain.admin.service;

import com.project.sentic.domain.admin.dto.*;
import com.project.sentic.domain.announcement.entity.Announcement;
import com.project.sentic.domain.announcement.repository.AnnouncementRepository;
import com.project.sentic.domain.faq.dto.FaqResponse;
import com.project.sentic.domain.faq.entity.Faq;
import com.project.sentic.domain.faq.repository.FaqRepository;
import com.project.sentic.domain.payment.entity.Payment;
import com.project.sentic.domain.payment.repository.PaymentRepository;
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
public class AdminService {

    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final FaqRepository faqRepository;
    private final PaymentRepository paymentRepository;

    // ── 회원 관리 ──────────────────────────────────

    // 회원 목록 조회 (검색)
    public List<AdminUserResponse> getUsers(String keyword) {
        List<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.findByNicknameContainingOrEmailContaining(keyword, keyword);
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
                .map(AdminUserResponse::from)
                .collect(Collectors.toList());
    }

    // 회원 상세 조회
    public AdminUserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return AdminUserResponse.from(user);
    }

    // 회원 상태 변경
    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        User.Status status = User.Status.valueOf(request.getStatus().toUpperCase());
        user.updateStatus(status);

        return AdminUserResponse.from(user);
    }

    // ── 공지사항 ──────────────────────────────────

    // 공지사항 목록 조회
    public List<AnnouncementResponse> getAnnouncements() {
        return announcementRepository.findByDeletedFalseOrderByPinnedDescCreatedAtDesc()
                .stream()
                .map(AnnouncementResponse::from)
                .collect(Collectors.toList());
    }

    // 공지사항 작성
    @Transactional
    public AnnouncementResponse createAnnouncement(Long adminId, AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .adminId(adminId)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return AnnouncementResponse.from(announcementRepository.save(announcement));
    }

    // 공지사항 수정
    @Transactional
    public AnnouncementResponse updateAnnouncement(Long announcementId, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcement.update(request.getTitle(), request.getContent());

        return AnnouncementResponse.from(announcement);
    }

    // 공지사항 삭제
    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcement.delete();
    }

    // 공지사항 상단 고정 토글
    @Transactional
    public AnnouncementResponse togglePin(Long announcementId) {
        Announcement announcement = announcementRepository.findByIdAndDeletedFalse(announcementId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));

        announcement.togglePin();

        return AnnouncementResponse.from(announcement);
    }

    // ── FAQ 관리 ──────────────────────────────────

    // FAQ 목록 조회
    public List<FaqResponse> getFaqs() {
        return faqRepository.findAllByOrderByOrderNumAsc()
                .stream()
                .map(FaqResponse::from)
                .collect(Collectors.toList());
    }

    // FAQ 작성
    @Transactional
    public FaqResponse createFaq(AdminFaqRequest request) {
        int nextOrder = faqRepository.findAllByOrderByOrderNumAsc()
                .stream()
                .mapToInt(Faq::getOrderNum)
                .max()
                .orElse(0) + 1;

        Faq faq = Faq.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())

                .orderNum(nextOrder)
                .build();

        return FaqResponse.from(faqRepository.save(faq));
    }

    // FAQ 수정
    @Transactional
    public FaqResponse updateFaq(Long faqId, AdminFaqRequest request) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        faq.update(request.getQuestion(), request.getAnswer());

        return FaqResponse.from(faq);
    }

    // FAQ 삭제
    @Transactional
    public void deleteFaq(Long faqId) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        faqRepository.delete(faq);
    }

    // FAQ 순서 변경
    @Transactional
    public FaqResponse updateFaqOrder(Long faqId, FaqOrderUpdateRequest request) {
        Faq faq = faqRepository.findById(faqId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        faq.updateOrderNum(request.getOrderNum());

        return FaqResponse.from(faq);
    }

    // ── 결제 내역 ──────────────────────────────────

    // 전체 결제 내역 (상태별 필터)
    public List<AdminPaymentResponse> getPayments(String status) {
        List<Payment> payments;
        if (status != null && !status.isBlank()) {
            payments = paymentRepository.findByStatusOrderByCreatedAtDesc(
                    Payment.PaymentStatus.valueOf(status.toUpperCase())
            );
        } else {
            payments = paymentRepository.findAllByOrderByCreatedAtDesc();
        }
        return payments.stream()
                .map(AdminPaymentResponse::from)
                .collect(Collectors.toList());
    }

    // 회원별 결제 내역
    public List<AdminPaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AdminPaymentResponse::from)
                .collect(Collectors.toList());
    }
}

package com.project.sentic.domain.announcement.repository;

import com.project.sentic.domain.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // 공지사항 목록 (고정 우선, 최신순)
    List<Announcement> findByDeletedFalseOrderByPinnedDescCreatedAtDesc();

    // 공지사항 단건 조회 (삭제되지 않은 것)
    Optional<Announcement> findByIdAndDeletedFalse(Long id);
}
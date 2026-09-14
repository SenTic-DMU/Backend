package com.project.sentic.domain.badge.repository;

import com.project.sentic.domain.badge.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    // 유저의 전체 뱃지 조회
    List<UserBadge> findByUserId(Long userId);

    // 유저의 대표 뱃지 조회
    List<UserBadge> findByUserIdAndFeaturedTrue(Long userId);

    // 유저가 특정 뱃지 보유 여부
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);

    // 유저의 뱃지 개수
    long countByUserId(Long userId);
}
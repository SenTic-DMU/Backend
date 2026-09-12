package com.project.sentic.domain.room.service;

import com.project.sentic.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {

    private final RoomRepository roomRepository;

    // 매일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTrash() {
        LocalDateTime expiredDate = LocalDateTime.now().minusDays(30);
        int count = roomRepository.deleteAllByDeletedTrueAndDeletedAtBefore(expiredDate);
        log.info("[Trash] 30일 지난 방 {}개 영구 삭제 완료", count);
    }
}
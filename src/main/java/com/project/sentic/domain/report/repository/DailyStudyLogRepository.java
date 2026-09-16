package com.project.sentic.domain.report.repository;

import com.project.sentic.domain.report.entity.DailyStudyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStudyLogRepository extends JpaRepository<DailyStudyLog, Long> {

    // 오늘 학습 기록 조회
    Optional<DailyStudyLog> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);

    // 월별 학습 기록 조회 (캘린더용)
    List<DailyStudyLog> findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate
    );
}
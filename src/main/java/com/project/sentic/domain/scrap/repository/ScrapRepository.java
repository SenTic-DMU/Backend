package com.project.sentic.domain.scrap.repository;

import com.project.sentic.domain.scrap.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    List<Scrap> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Scrap> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, Scrap.ScrapCategory category);

    @Query(value = """
            SELECT s.* FROM scraps s
            WHERE s.user_id = :userId 
            AND (
                s.room_id = :roomId
                OR (s.feedback_id IS NOT NULL AND s.feedback_id IN (
                    SELECT f.feedback_id FROM feedbacks f WHERE f.room_id = :roomId
                ))
            )
            ORDER BY s.created_at DESC
            """, nativeQuery = true)
    List<Scrap> findByUserIdAndRoomId(@Param("userId") Long userId, @Param("roomId") Long roomId);
}
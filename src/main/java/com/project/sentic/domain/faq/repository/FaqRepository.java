package com.project.sentic.domain.faq.repository;

import com.project.sentic.domain.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    // 순서대로 FAQ 목록 조회
    List<Faq> findAllByOrderByOrderNumAsc();
}
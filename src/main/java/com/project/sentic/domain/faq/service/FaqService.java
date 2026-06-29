package com.project.sentic.domain.faq.service;

import com.project.sentic.domain.faq.dto.FaqResponse;
import com.project.sentic.domain.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    private final FaqRepository faqRepository;

    // 자주 묻는 질문 목록 조회
    public List<FaqResponse> getFaqs() {
        return faqRepository.findAllByOrderByOrderNumAsc()
                .stream()
                .map(FaqResponse::from)
                .collect(Collectors.toList());
    }
}
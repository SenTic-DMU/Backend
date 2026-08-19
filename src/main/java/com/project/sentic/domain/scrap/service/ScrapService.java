package com.project.sentic.domain.scrap.service;

import com.project.sentic.domain.scrap.dto.ScrapResponse;
import com.project.sentic.domain.scrap.dto.ScrapSaveRequest;
import com.project.sentic.domain.scrap.entity.Scrap;
import com.project.sentic.domain.scrap.repository.ScrapRepository;
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
public class ScrapService {

    private final ScrapRepository scrapRepository;

    @Transactional
    public ScrapResponse saveScrap(Long userId, ScrapSaveRequest request) {

        // 중복 체크 추가
        if (scrapRepository.existsByUserIdAndExpression(userId, request.getExpression())) {
            throw new CustomException(ErrorCode.DUPLICATE_SCRAP);
        }

        Scrap scrap = Scrap.builder()
                .userId(userId)
                .feedbackId(request.getFeedbackId())
                .roomId(request.getRoomId())
                .expression(request.getExpression())
                .context(request.getContext())
                .category(request.getCategory())
                .build();
        return ScrapResponse.from(scrapRepository.save(scrap));
    }

    public List<ScrapResponse> getScraps(Long userId, Scrap.ScrapCategory category, Long roomId) {
        List<Scrap> scraps;
        if (roomId != null) {
            scraps = scrapRepository.findByUserIdAndRoomId(userId, roomId);
        } else if (category != null) {
            scraps = scrapRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
        } else {
            scraps = scrapRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return scraps.stream()
                .map(ScrapResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteScrap(Long userId, Long scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCRAP_NOT_FOUND));

        if (!scrap.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.SCRAP_ACCESS_DENIED);
        }

        scrapRepository.delete(scrap);
    }
}

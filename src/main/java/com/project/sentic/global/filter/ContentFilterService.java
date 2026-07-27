package com.project.sentic.global.filter;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 부적절한 표현 키워드 필터링 서비스
 *
 * 자살/자해, 성적 표현, 혐오 표현 카테고리의 키워드가 감지되면
 * CustomException(ErrorCode.INAPPROPRIATE_CONTENT)를 던집니다.
 * 방 생성(situation), 채팅/음성 대화(content)에서 공통으로 사용해요.
 */
@Component
public class ContentFilterService {

    private static final List<String> SUICIDE_SELF_HARM_KEYWORDS = List.of(
            // 한국어
            "자살", "자해", "죽고싶다", "죽고 싶다", "죽어", "목매", "투신",
            // 영어
            "suicide", "self-harm", "self harm", "kill myself", "end my life", "hang myself"
    );

    private static final List<String> SEXUAL_KEYWORDS = List.of(
            // 한국어
            "섹스", "성관계", "야동", "포르노", "자위", "성기", "보지", "자지", "씨발년", "걸레",
            // 영어
            "sex", "porn", "porno", "masturbat", "blowjob", "handjob", "dick", "pussy", "cock", "nude"
    );

    private static final List<String> HATE_SPEECH_KEYWORDS = List.of(
            // 한국어
            "씨발", "개새끼", "병신", "지랄", "좆같", "미친놈", "미친년", "찐따",
            "장애인새끼", "흑형", "짱깨", "쪽바리", "떼놈",
            // 영어
            "fuck", "bitch", "asshole", "bastard", "nigger", "nigga", "retard", "faggot", "chink", "spic"
    );

    public void check(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String normalized = text.toLowerCase(Locale.ROOT);

        if (containsAny(normalized, SUICIDE_SELF_HARM_KEYWORDS)
                || containsAny(normalized, SEXUAL_KEYWORDS)
                || containsAny(normalized, HATE_SPEECH_KEYWORDS)) {
            throw new CustomException(ErrorCode.INAPPROPRIATE_CONTENT);
        }
    }

    private boolean containsAny(String normalized, List<String> keywords) {
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
